package com.blockreels.app

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.blockreels.app.detection.AppPlatform
import com.blockreels.app.detection.DetectionResult
import com.blockreels.app.detection.DetectorRegistry
import com.blockreels.app.detection.FacebookDetector
import com.blockreels.app.detection.InstagramDetector
import com.blockreels.app.detection.TikTokDetector
import com.blockreels.app.detection.YouTubeDetector
import com.blockreels.app.engine.DetectionTrace
import com.blockreels.app.engine.EventThrottler
import com.blockreels.app.engine.ForegroundWindowSelection
import com.blockreels.app.engine.ScreenScanQueue
import com.blockreels.app.engine.TraversalEngine
import com.blockreels.app.engine.ViewerScreenRecheck
import com.blockreels.app.intervention.InterventionConfigResolver
import com.blockreels.app.intervention.InterventionController
import com.blockreels.app.intervention.TikTokPlaybackPause
import com.blockreels.app.model.RealAccessibilityNode
import com.blockreels.app.state.NativeEventBus
import com.blockreels.app.state.NativeStateStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockReelsAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var activeInstance: BlockReelsAccessibilityService? = null

        fun isServiceActive(): Boolean = activeInstance != null

        fun grantTemporaryAllowance(packageName: String, durationMs: Long): Boolean {
            val service = activeInstance ?: return false
            service.interventionController.grantTemporaryAllowance(packageName, durationMs)
            return true
        }
    }

    private lateinit var detectorRegistry: DetectorRegistry
    private lateinit var traversalEngine: TraversalEngine
    private lateinit var eventThrottler: EventThrottler
    private lateinit var interventionController: InterventionController
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val screenHandler = Handler(Looper.getMainLooper())
    private val audioThread = HandlerThread("BlockReelsAudio")
    private lateinit var audioHandler: Handler
    private val instagramPackage = "com.instagram.android"
    private var foregroundPackage: String? = null
    private var lastWindowSignal: Pair<String?, Int>? = null
    private var overlayCheck: Runnable? = null
    private var allowanceCheck: Runnable? = null
    private var leaveCheck: Job? = null
    private var exitCheck: Job? = null
    private var entryCheck: ViewerScreenRecheck? = null
    private var entryPackage: String? = null

    private data class Signal(
        val pkg: String?,
        val type: Int? = null,
        val time: Long? = null,
        val window: Int? = null,
        val received: Long = SystemClock.uptimeMillis(),
        val followUp: Boolean = false
    )

    private data class Screen(
        val frame: DetectionTrace.Frame,
        val result: DetectionResult? = null
    )

    private lateinit var scans: ScreenScanQueue<Signal, Screen>

    override fun onCreate() {
        super.onCreate()
        DetectionTrace.configure(this)
        NativeEventBus.bindStateStore()
        NativeStateStore.initialize(applicationContext)

        detectorRegistry = DetectorRegistry()
        traversalEngine = TraversalEngine()
        eventThrottler = EventThrottler(debounceWindowMs = 75L)
        audioThread.start()
        audioHandler = Handler(audioThread.looper)
        interventionController = InterventionController(
            context = this,
            onGlobalBackAction = { performGlobalAction(GLOBAL_ACTION_BACK) },
            onSystemHomeAction = { performGlobalAction(GLOBAL_ACTION_HOME) },
            onExitStarted = ::verifyTikTokExit,
            onExitFinished = { exitCheck?.cancel() },
            onFeedLeft = { pkg ->
                scans.invalidate()
                cancelEntryCheck()
                cancelOverlayCheck()
                leaveCheck?.cancel()
                leaveCheck = serviceScope.launch {
                    delay(InterventionController.BACK_TRANSITION_MS)
                    scans.invalidate()
                    scans.submit(Signal(pkg))
                }
            },
            dispatchAudio = { action -> audioHandler.post(action); Unit },
            onTikTokOverlayAttached = ::pauseTikTokPlayback,
            onAllowanceGranted = { pkg ->
                scans.invalidate()
                cancelEntryCheck()
                if (foregroundPackage == pkg) checkAtAllowanceExpiry(pkg)
            }
        )
        audioHandler.post { interventionController.audioDuckingManager.recoverMediaMute() }
        scans = ScreenScanQueue(serviceScope, Dispatchers.IO, ::readScreen, ::applyScreen)
        activeInstance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            try {
                NativeStateStore.initialize(applicationContext).await()
                scans.submit(Signal(null))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                android.util.Log.e("BlockReels", "Could not initialize protection state", failure)
            }
        }
        NativeEventBus.emit(
            mapOf(
                "eventType" to "SERVICE_STATE_CHANGED",
                "isEnabled" to true,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !NativeStateStore.isReady()) return
        foregroundPackage?.let(::scheduleOverlayCheck)
        val pkg = event.packageName?.toString()
        val type = event.eventType
        val windowEvent = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            type == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        if (!windowEvent && AppPlatform.fromPackageName(pkg) == null) return
        val signal = Signal(pkg, type, event.eventTime, event.windowId)
        if (windowEvent) {
            val window = pkg to event.windowId
            val focusChanged = type == AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                event.windowChanges and (AccessibilityEvent.WINDOWS_CHANGE_ACTIVE or
                    AccessibilityEvent.WINDOWS_CHANGE_FOCUSED) != 0
            if (focusChanged || (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && window != lastWindowSignal)) {
                scans.invalidate()
                cancelEntryCheck()
            }
            if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) lastWindowSignal = window
        } else {
            if (pkg == null || interventionController.hasInterventionFor(pkg)) return
            if (type == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                scans.invalidate()
                cancelEntryCheck()
            } else if (!eventThrottler.shouldProcess(pkg, type, false)) {
                requestEntryCheck(signal)
                return
            }
        }
        scans.submit(signal)
    }

    private fun cancelEntryCheck() {
        entryCheck?.cancel()
        entryCheck = null
        entryPackage = null
    }

    private fun requestEntryCheck(signal: Signal) {
        val pkg = signal.pkg ?: return
        if (AppPlatform.fromPackageName(pkg) == null) return
        if (entryPackage == pkg && entryCheck?.isPending == true) return
        cancelEntryCheck()
        entryPackage = pkg
        entryCheck = ViewerScreenRecheck(
            postDelayed = { task, ms -> screenHandler.postDelayed(task, ms); Unit },
            removeCallbacks = screenHandler::removeCallbacks,
            firstDelayMs = if (pkg == instagramPackage) 75L else 150L,
            secondDelayMs = if (pkg == instagramPackage) 150L else 350L
        ) {
            scans.submit(signal.copy(followUp = true))
            true
        }
        entryCheck?.request()
    }

    private fun observeForeground(pkg: String, windowId: Int) {
        if (foregroundPackage != pkg) {
            leaveCheck?.cancel()
            if (pkg != entryPackage) cancelEntryCheck()
            cancelOverlayCheck()
            cancelAllowanceCheck()
            foregroundPackage = pkg
        }
        interventionController.onForegroundChanged(pkg, windowId)
    }

    private fun foregroundRoot(expectedPackage: String? = null): AccessibilityNodeInfo? {
        val snapshot = try { windows } catch (_: Exception) { emptyList() }
        var details = emptyList<ForegroundWindowSelection.Window>()
        var selected: Int? = null
        try {
            details = snapshot.map {
                ForegroundWindowSelection.Window(it.id, it.type, it.isFocused, it.isActive)
            }
            selected = ForegroundWindowSelection.select(details)
            snapshot.firstOrNull { it.id == selected }?.root?.let { return it }
        } catch (_: Exception) {
            // A window can disappear between enumeration and retrieving its root.
        } finally {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                snapshot.forEach { it.recycle() }
            }
        }
        val root = try { rootInActiveWindow } catch (_: Exception) { null } ?: return null
        val type = if (selected == null) {
            details.firstOrNull { it.id == root.windowId }?.type ?: windowType(root)
        } else null
        if (ForegroundWindowSelection.acceptsActiveRoot(
                selected, root.windowId, type, root.packageName?.toString(), expectedPackage
            )
        ) return root
        recycleNode(root)
        return null
    }

    private fun windowType(root: AccessibilityNodeInfo): Int? {
        val window = try { root.window } catch (_: Exception) { null } ?: return null
        return try {
            window.type
        } finally {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                window.recycle()
            }
        }
    }

    private fun verifyTikTokExit(id: Long) {
        scans.invalidate()
        exitCheck?.cancel()
        exitCheck = serviceScope.launch {
            for (wait in listOf(250L, 750L)) {
                delay(wait)
                val current = withContext(Dispatchers.IO) {
                    val root = foregroundRoot() ?: return@withContext null
                    try {
                        root.packageName?.toString()?.let { it to root.windowId }
                    } finally {
                        recycleNode(root)
                    }
                }
                if (!interventionController.isExitPending(id)) return@launch
                current?.let { observeForeground(it.first, it.second) }
                if (!interventionController.isExitPending(id)) return@launch
            }
            interventionController.onExitVerificationTimeout(id)
        }
    }

    private fun pauseTikTokPlayback(pkg: String, id: Long) {
        serviceScope.launch(Dispatchers.IO) {
            val root = foregroundRoot(pkg) ?: return@launch
            try {
                if (root.packageName?.toString() == pkg && interventionController.isActive(id)) {
                    val outcome = TikTokPlaybackPause.attempt(RealAccessibilityNode(root), traversalEngine) {
                        interventionController.isActive(id)
                    }
                    DetectionTrace.audio(id, "TIKTOK_PAUSE_${outcome.name}")
                }
            } finally {
                recycleNode(root)
            }
        }
    }

    private fun cancelOverlayCheck() {
        overlayCheck?.let { screenHandler.removeCallbacks(it) }
        overlayCheck = null
    }

    private fun scheduleOverlayCheck(packageName: String) {
        if (overlayCheck != null || !interventionController.hasInterventionFor(packageName)) return
        val callback = Runnable {
            overlayCheck = null
            if ((getSystemService(POWER_SERVICE) as? PowerManager)?.isInteractive == false) {
                if (AppPlatform.fromPackageName(packageName) == AppPlatform.TIKTOK) {
                    interventionController.dismissOverlay()
                }
                return@Runnable
            }
            scans.submit(Signal(packageName, followUp = true))
            if (foregroundPackage == packageName) scheduleOverlayCheck(packageName)
        }
        overlayCheck = callback
        screenHandler.postDelayed(callback, 400L)
    }

    private fun cancelAllowanceCheck() {
        allowanceCheck?.let { screenHandler.removeCallbacks(it) }
        allowanceCheck = null
    }

    private fun checkAtAllowanceExpiry(packageName: String) {
        cancelAllowanceCheck()
        val remaining = interventionController.temporaryAllowanceRemainingMs(packageName)
        if (remaining <= 0L) return
        val callback = Runnable {
            allowanceCheck = null
            scans.submit(Signal(packageName))
        }
        allowanceCheck = callback
        screenHandler.postDelayed(callback, remaining)
    }

    private fun readScreen(signal: Signal, isCurrent: () -> Boolean): Screen {
        val trace = DetectionTrace.Frame(
            signal.pkg.orEmpty(), signal.type, signal.time, signal.window,
            signal.followUp, receivedUptimeMs = signal.received
        )
        trace.ready = NativeStateStore.isReady()
        if (!isCurrent()) return Screen(trace)
        val root = foregroundRoot(signal.pkg) ?: return Screen(trace)
        try {
            val pkg = root.packageName?.toString() ?: return Screen(trace)
            trace.rootPackage = pkg
            trace.rootWindow = root.windowId
            val node = RealAccessibilityNode(root)
            trace.rootVisible = node.isVisibleToUser
            val detector = detectorRegistry.getDetectorForPackage(pkg) ?: return Screen(trace)
            val windowEvent = !signal.followUp && (signal.type == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                signal.type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
            if (trace.rootVisible != true || (!windowEvent && signal.pkg != null && signal.pkg != pkg)) {
                return Screen(trace)
            }
            if (DetectionTrace.enabled) {
                trace.masterEnabled = NativeStateStore.isMasterEnabled()
                trace.appEnabled = NativeStateStore.getAppToggles()[pkg] == true
            }
            trace.blockingActive = NativeStateStore.isAppBlockingActive(pkg)
            trace.temporarilyAllowed = interventionController.isAppTemporarilyAllowed(pkg)
            if (!trace.blockingActive || trace.temporarilyAllowed) return Screen(trace)
            val sample = if (DetectionTrace.enabled) mutableListOf<DetectionTrace.NodeSummary>() else null
            val observer: ((com.blockreels.app.model.AccessibilityNode, Int) -> Unit)? = sample?.let {
                { item, depth ->
                    if (it.size < 48) {
                        it.add(DetectionTrace.NodeSummary.from(item, depth, detector.platform, packageName = pkg))
                    }
                    Unit
                }
            }
            val engine = if (detector is FacebookDetector) {
                TraversalEngine(maxBudget = 320, isCurrent = isCurrent)
            } else TraversalEngine(isCurrent = isCurrent)
            val result = when (detector) {
                is InstagramDetector -> detector.evaluateWithTrace(node, engine, observer) {
                    trace.evidenceUptimeMs = SystemClock.uptimeMillis()
                }
                is YouTubeDetector -> detector.evaluateWithTrace(node, null, engine, observer)
                is FacebookDetector -> detector.evaluateWithTrace(node, engine, observer, isCurrent)
                is TikTokDetector -> detector.evaluateWithTrace(node, engine, observer)
                else -> detector.evaluate(node, null, engine)
            }
            trace.detectionCompletedUptimeMs = SystemClock.uptimeMillis()
            if (!isCurrent()) return Screen(trace)
            if (result.isMatch) {
                val current = foregroundRoot(pkg)
                try {
                    if (current == null || current.windowId != root.windowId || current.packageName?.toString() != pkg) {
                        trace.rootPackage = current?.packageName?.toString()
                        trace.rootWindow = current?.windowId
                        return Screen(trace)
                    }
                } finally {
                    current?.let(::recycleNode)
                }
            } else sample?.let { DetectionTrace.structure(trace, it) }
            return Screen(trace, result)
        } catch (failure: RuntimeException) {
            android.util.Log.w("BlockReels", "Could not read the active window", failure)
            return Screen(trace)
        } finally {
            recycleNode(root)
        }
    }

    private fun applyScreen(signal: Signal, screen: Screen) {
        val trace = screen.frame
        val pkg = trace.rootPackage
        val window = trace.rootWindow
        if (pkg == null || window == null) {
            DetectionTrace.pipeline(trace, DetectionTrace.Stage.NO_ROOT)
            if (!signal.followUp) requestEntryCheck(signal)
            return
        }
        observeForeground(pkg, window)
        if (AppPlatform.fromPackageName(pkg) == null) return
        if (!NativeStateStore.isAppBlockingActive(pkg)) {
            if (interventionController.hasInterventionFor(pkg)) interventionController.dismissOverlay()
            cancelEntryCheck()
            cancelAllowanceCheck()
            return
        }
        if (interventionController.isAppTemporarilyAllowed(pkg)) {
            cancelEntryCheck()
            checkAtAllowanceExpiry(pkg)
            return
        }
        val result = screen.result
        if (result == null && signal.pkg != pkg && trace.rootVisible != false) {
            scans.submit(Signal(pkg))
            return
        }
        if (result != null) {
            val config = InterventionConfigResolver.resolve(
                NativeStateStore.getInterventionConfig(),
                NativeStateStore.getRule(pkg)
            )
            trace.ruleMode = config.mode.name
            DetectionTrace.pipeline(trace, DetectionTrace.Stage.DETECTED, result, traversalEngine)
            if (result.isMatch) {
                cancelEntryCheck()
                if (interventionController.executeIntervention(pkg, config, windowId = window, evaluationId = trace.id)) {
                    NativeStateStore.recordBlockEvent(pkg, result.layerMatched?.name ?: "REEL_CONTAINER_MATCH", config.mode)
                    DetectionTrace.pipeline(trace, DetectionTrace.Stage.INTERVENTION, result, traversalEngine)
                }
                scheduleOverlayCheck(pkg)
                return
            }
            interventionController.onScreenEvaluated(pkg, result)
        }
        if (!signal.followUp && !interventionController.hasInterventionFor(pkg)) {
            requestEntryCheck(signal.copy(pkg = pkg))
        }
    }

    private fun recycleNode(sourceNode: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                @Suppress("DEPRECATION")
                sourceNode.recycle()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    override fun onInterrupt() {
        scans.invalidate()
        exitCheck?.cancel()
        leaveCheck?.cancel()
        cancelEntryCheck()
        cancelAllowanceCheck()
        cancelOverlayCheck()
        interventionController.dismissOverlay()
    }

    override fun onDestroy() {
        onInterrupt()
        serviceScope.cancel()
        audioThread.quitSafely()
        super.onDestroy()
        if (activeInstance === this) activeInstance = null
        NativeEventBus.emit(
            mapOf(
                "eventType" to "SERVICE_STATE_CHANGED",
                "isEnabled" to false,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}
