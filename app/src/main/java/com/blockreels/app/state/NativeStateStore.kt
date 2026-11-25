// SPDX-License-Identifier: MIT
package com.blockreels.app.state

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

enum class InterventionMode {
    AUTO_BACK,
    REDIRECT_HOME,
    MINDFUL_PAUSE,
    STRICT_BLOCK;

    val displayName: String
        get() = when (this) {
            AUTO_BACK -> "Auto Back"
            REDIRECT_HOME -> "Home Exit"
            MINDFUL_PAUSE -> "Mindful Pause"
            STRICT_BLOCK -> "Strict Block"
        }

    val shortLabel: String
        get() = when (this) {
            AUTO_BACK -> "Back"
            REDIRECT_HOME -> "Home"
            MINDFUL_PAUSE -> "Mindful"
            STRICT_BLOCK -> "Strict"
        }
}

enum class BlockingScope {
    FEED_ONLY,
    ENTIRE_APP;

    val displayName: String
        get() = when (this) {
            FEED_ONLY -> "Feed Only"
            ENTIRE_APP -> "Full App"
        }
}

enum class ContentBlockPolicy(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    REELS_ONLY("Block Reels", "Reels", "Block short-form video feeds only"),
    MAIN_FEED_ONLY("Block Feed", "Feed", "Block home scrolling feeds only"),
    FEED_AND_REELS("Block Both", "Both", "Block both home feed and short-form reels"),
    HIDE_SURFACES("Don't Show", "Hide", "Request not to see feed or reels and auto-dismiss")
}

enum class DetectionSensitivity(
    val displayName: String,
    val minConfidenceScore: Double,
    val debounceWindowMs: Long,
    val specLabel: String
) {
    CONSERVATIVE("Conservative", 1.0, 150L, "Definitive IDs · 150ms"),
    BALANCED("Balanced", 0.85, 75L, "High+ confidence · 75ms"),
    AGGRESSIVE("Aggressive", 0.65, 40L, "Medium+ · 40ms")
}

enum class AudioAction(val displayName: String) {
    SILENCE("Silence"),
    DUCK("Duck"),
    NONE("Off")
}

data class InterventionConfig(
    val mode: InterventionMode = InterventionMode.MINDFUL_PAUSE,
    val holdDurationSeconds: Int = 5,
    val customMessage: String = "Take a deep breath. Do you really want to scroll right now?",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val pinProtected: Boolean = false,
    val allowanceMinutes: Int = 5,
    val dailyPassLimit: Int = 0, // 0 = Unlimited
    val entryDelaySeconds: Int = 0,
    val sensitivity: DetectionSensitivity = DetectionSensitivity.BALANCED,
    val audioAction: AudioAction = AudioAction.SILENCE,
    val strictMode: Boolean = false
)

data class ScheduleRule(
    val id: String,
    val title: String,
    val isActive: Boolean,
    val daysOfWeek: Set<Int>, // ISO-8601: 1 = Monday, ..., 7 = Sunday
    val startMinuteOfDay: Int, // 0 to 1439
    val endMinuteOfDay: Int,   // 0 to 1439
    val enabledPackages: Set<String>,
    val modeOverride: InterventionMode? = null,
    val scopeOverride: BlockingScope? = null
) {
    fun isTimeActive(
        currentIsoDay: Int,
        previousIsoDay: Int,
        currentMinuteOfDay: Int,
        targetPackage: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!isActive) return false
        if (enabledPackages.isNotEmpty() && !enabledPackages.contains(targetPackage)) return false
        if (startMinuteOfDay == endMinuteOfDay) return false

        if (id.startsWith(QUICK_BOOST_ID_PREFIX)) {
            val startedAt = id.removePrefix(QUICK_BOOST_ID_PREFIX).toLongOrNull()
                ?: return false
            var durationMinutes = endMinuteOfDay - startMinuteOfDay
            if (durationMinutes < 0) durationMinutes += 24 * 60
            if (durationMinutes == 0) return false
            val endsAt = startedAt + durationMinutes * 60_000L
            return nowMillis >= startedAt && nowMillis < endsAt
        }

        return if (startMinuteOfDay < endMinuteOfDay) {
            daysOfWeek.contains(currentIsoDay) &&
                currentMinuteOfDay >= startMinuteOfDay &&
                currentMinuteOfDay < endMinuteOfDay
        } else {
            val evening = daysOfWeek.contains(currentIsoDay) &&
                currentMinuteOfDay >= startMinuteOfDay
            val morning = daysOfWeek.contains(previousIsoDay) &&
                currentMinuteOfDay < endMinuteOfDay
            evening || morning
        }
    }

    companion object {
        const val QUICK_BOOST_ID_PREFIX = "quick_boost_"
    }
}

data class RuleConfig(
    val packageName: String,
    val isEnabled: Boolean,
    val mode: InterventionMode,
    val holdDurationSeconds: Int? = null,
    val scope: BlockingScope = BlockingScope.FEED_ONLY,
    val allowanceMinutes: Int? = null,
    val dailyPassLimit: Int? = null,
    val contentPolicy: ContentBlockPolicy = ContentBlockPolicy.REELS_ONLY,
    val requestHideSurfaces: Boolean = false,
    val reelsDailyGoalMinutes: Int = 0,
    val feedDailyGoalMinutes: Int = 0
)

val Context.appDataStore by preferencesDataStore(name = "blockreels_state")

object NativeStateStore {

    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initializationLock = Any()
    private val stateMutex = Mutex()
    @Volatile private var initialization: Deferred<Unit>? = null
    @Volatile private var preferencesStore: DataStore<Preferences>? = null
    @Volatile private var ready = false

    @Volatile
    private var appContext: Context? = null
    private val metricsDate = AtomicReference(currentDateKey())

    private val appToggles = ConcurrentHashMap<String, Boolean>()
    private val interventionConfig = AtomicReference(InterventionConfig())
    private val schedules = AtomicReference<List<ScheduleRule>>(emptyList())
    private val rules = ConcurrentHashMap<String, RuleConfig>()
    private val masterEnabled = AtomicBoolean(true)

    private val totalBlockedToday = AtomicInteger(0)
    private val blockedByApp = ConcurrentHashMap<String, AtomicInteger>()
    private val passesUsedByApp = ConcurrentHashMap<String, AtomicInteger>()
    private val lastBlockedPackage = AtomicReference("")
    private val lastBlockedTimestamp = AtomicLong(0L)
    private val serviceStartTime = AtomicLong(System.currentTimeMillis())

    var eventBroadcaster: ((Map<String, Any>) -> Unit)? = null
    internal var blockEventWriterForTests: (suspend (PendingBlockEvent) -> Unit)? = null

    private val KEY_MASTER_ENABLED = booleanPreferencesKey("master_enabled")
    private val KEY_APP_TOGGLES_JSON = stringPreferencesKey("app_toggles_json")
    private val KEY_INTERVENTION_MODE = stringPreferencesKey("intervention_mode")
    private val KEY_HOLD_DURATION = intPreferencesKey("hold_duration_seconds")
    private val KEY_CUSTOM_MESSAGE = stringPreferencesKey("custom_message")
    private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    private val KEY_PIN_PROTECTED = booleanPreferencesKey("pin_protected")
    private val KEY_ALLOWANCE_MINUTES = intPreferencesKey("allowance_minutes")
    private val KEY_DAILY_PASS_LIMIT = intPreferencesKey("daily_pass_limit")
    private val KEY_ENTRY_DELAY_SECONDS = intPreferencesKey("entry_delay_seconds")
    private val KEY_SENSITIVITY = stringPreferencesKey("detection_sensitivity")
    private val KEY_AUDIO_ACTION = stringPreferencesKey("audio_action")
    private val KEY_STRICT_MODE = booleanPreferencesKey("strict_mode")
    private val KEY_SCHEDULES_JSON = stringPreferencesKey("schedules_json")
    private val KEY_RULES_JSON = stringPreferencesKey("rules_json")
    private val KEY_METRICS_TOTAL = intPreferencesKey("metrics_total_today")
    private val KEY_METRICS_BY_APP = stringPreferencesKey("metrics_by_app_json")
    private val KEY_PASSES_BY_APP = stringPreferencesKey("passes_by_app_json")
    private val KEY_METRICS_DATE = stringPreferencesKey("metrics_date")

    init {
        seedDefaultToggles()
    }

    private fun seedDefaultToggles() {
        appToggles["com.instagram.android"] = true
        appToggles["com.google.android.youtube"] = true
        appToggles["com.facebook.katana"] = false
        appToggles["com.facebook.lite"] = false
        appToggles["com.zhiliaoapp.musically"] = true
        appToggles["com.ss.android.ugc.trill"] = false
        appToggles["com.ss.android.ugc.aweme"] = false
        appToggles["com.ss.android.ugc.aweme.lite"] = false
        appToggles["com.snapchat.android"] = false
    }

    fun initialize(context: Context): Deferred<Unit> {
        appContext = context.applicationContext
        return initialize(context.applicationContext.appDataStore)
    }

    internal fun initialize(store: DataStore<Preferences>): Deferred<Unit> =
        synchronized(initializationLock) {
            initialization?.let { return@synchronized it }
            preferencesStore = store
            serviceStartTime.set(System.currentTimeMillis())
            storeScope.async(start = CoroutineStart.LAZY) {
                stateMutex.withLock {
                    val prefs = store.data.first()
                    masterEnabled.set(prefs[KEY_MASTER_ENABLED] ?: true)

                    prefs[KEY_APP_TOGGLES_JSON]?.let { jsonStr ->
                        val obj = JSONObject(jsonStr)
                        appToggles.clear()
                        obj.keys().forEach { key ->
                            appToggles[key] = obj.getBoolean(key)
                        }
                    }

                    val mode = parseInterventionMode(prefs[KEY_INTERVENTION_MODE])
                    val holdSec = (prefs[KEY_HOLD_DURATION] ?: 5).coerceIn(3, 30)
                    val customMsg = prefs[KEY_CUSTOM_MESSAGE] ?: "Take a deep breath."
                    val sound = prefs[KEY_SOUND_ENABLED] ?: true
                    val haptics = prefs[KEY_HAPTICS_ENABLED] ?: true
                    val pin = prefs[KEY_PIN_PROTECTED] ?: false
                    val allowanceMin = (prefs[KEY_ALLOWANCE_MINUTES] ?: 5).coerceIn(1, 30)
                    val passLimit = (prefs[KEY_DAILY_PASS_LIMIT] ?: 0).coerceIn(0, 10)
                    val entryDelay = (prefs[KEY_ENTRY_DELAY_SECONDS] ?: 0).coerceIn(0, 10)
                    val sensitivity = parseSensitivity(prefs[KEY_SENSITIVITY])
                    val audioAction = prefs[KEY_AUDIO_ACTION]?.let(::parseAudioAction)
                        ?: if (sound) AudioAction.SILENCE else AudioAction.NONE
                    val strictMode = prefs[KEY_STRICT_MODE] ?: false

                    interventionConfig.set(
                        InterventionConfig(
                            mode = mode,
                            holdDurationSeconds = holdSec,
                            customMessage = customMsg,
                            soundEnabled = audioAction != AudioAction.NONE,
                            hapticsEnabled = haptics,
                            pinProtected = pin,
                            allowanceMinutes = allowanceMin,
                            dailyPassLimit = passLimit,
                            entryDelaySeconds = entryDelay,
                            sensitivity = sensitivity,
                            audioAction = audioAction,
                            strictMode = strictMode
                        )
                    )

                    prefs[KEY_SCHEDULES_JSON]?.let { jsonStr ->
                        parseAndSetSchedulesFromJson(jsonStr)
                    }

                    prefs[KEY_RULES_JSON]?.let { jsonStr ->
                        parseAndSetRulesFromJson(jsonStr)
                    }

                    val storedDate = prefs[KEY_METRICS_DATE]
                    metricsDate.set(storedDate ?: currentDateKey())
                    if (storedDate == currentDateKey()) {
                        totalBlockedToday.set(prefs[KEY_METRICS_TOTAL] ?: 0)
                        prefs[KEY_METRICS_BY_APP]?.let { jsonStr ->
                            val obj = JSONObject(jsonStr)
                            obj.keys().forEach { pkg ->
                                blockedByApp[pkg] = AtomicInteger(obj.optInt(pkg, 0))
                            }
                        }
                        prefs[KEY_PASSES_BY_APP]?.let { jsonStr ->
                            val obj = JSONObject(jsonStr)
                            obj.keys().forEach { pkg ->
                                passesUsedByApp[pkg] = AtomicInteger(obj.optInt(pkg, 0))
                            }
                        }
                    } else {
                        totalBlockedToday.set(0)
                    }
                    ready = true
                }
            }.also {
                initialization = it
                it.start()
            }
        }

    fun isReady(): Boolean = ready

    private suspend fun writeState(
        context: Context? = null,
        write: suspend (DataStore<Preferences>) -> Unit
    ) {
        val completion = if (context != null) initialize(context)
        else checkNotNull(initialization) { "Native state has not been initialized" }
        completion.await()
        stateMutex.withLock {
            withContext(Dispatchers.IO) { write(checkNotNull(preferencesStore)) }
        }
    }

    internal suspend fun resetForTests() {
        initialization?.cancelAndJoin()
        synchronized(initializationLock) {
            initialization = null
            preferencesStore = null
            appContext = null
            ready = false
        }
        appToggles.clear()
        seedDefaultToggles()
        masterEnabled.set(true)
        interventionConfig.set(InterventionConfig())
        schedules.set(emptyList())
        rules.clear()
        eventBroadcaster = null
        blockEventWriterForTests = null
        resetMetrics()
    }

    fun findActiveSchedule(packageName: String, nowMillis: Long = System.currentTimeMillis()): ScheduleRule? {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentIsoDay = calendarIsoDay(calendar)
        val previousIsoDay = if (currentIsoDay == 1) 7 else currentIsoDay - 1
        val currentMinute =
            calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val active = schedules.get().filter {
            it.isTimeActive(currentIsoDay, previousIsoDay, currentMinute, packageName, nowMillis)
        }
        // Quick Focus Boost takes precedence over recurring schedules if both overlap.
        return active.firstOrNull { it.id.startsWith(ScheduleRule.QUICK_BOOST_ID_PREFIX) }
            ?: active.firstOrNull()
    }

    fun isAppBlockingActive(packageName: String): Boolean {
        if (!isReady() || !masterEnabled.get()) return false
        if (findActiveSchedule(packageName) != null) return true
        return appToggles[packageName] ?: false
    }

    fun getInterventionConfig(): InterventionConfig = interventionConfig.get()

    fun isMasterEnabled(): Boolean = masterEnabled.get()

    fun getAppToggles(): Map<String, Boolean> = HashMap(appToggles)

    fun getSchedules(): List<ScheduleRule> = schedules.get()

    fun getRule(packageName: String): RuleConfig? = rules[packageName]

    fun getPassesUsedToday(packageName: String): Int {
        resetMetricsIfNewDay()
        return passesUsedByApp[packageName]?.get() ?: 0
    }

    fun getAllPassesUsedToday(): Map<String, Int> {
        resetMetricsIfNewDay()
        return passesUsedByApp.mapValues { it.value.get() }
    }

    fun recordTemporaryPassUsed(packageName: String): Int {
        resetMetricsIfNewDay()
        val count = passesUsedByApp.computeIfAbsent(packageName) { AtomicInteger(0) }.incrementAndGet()
        persistMetrics()
        eventBroadcaster?.invoke(
            mapOf(
                "eventType" to "PASS_GRANTED",
                "packageName" to packageName,
                "passesUsedToday" to count,
                "timestamp" to System.currentTimeMillis()
            )
        )
        return count
    }

    fun recordBlockEvent(
        packageName: String,
        detectionReason: String,
        mode: InterventionMode = interventionConfig.get().mode
    ): Job {
        resetMetricsIfNewDay()
        totalBlockedToday.incrementAndGet()
        blockedByApp.computeIfAbsent(packageName) { AtomicInteger(0) }.incrementAndGet()
        lastBlockedPackage.set(packageName)
        val now = System.currentTimeMillis()
        lastBlockedTimestamp.set(now)

        val event = PendingBlockEvent.create(
            now, packageName, mapPackageToAppType(packageName), mode.name, detectionReason
        )

        persistMetrics()
        val context = appContext
        val outbox = context?.let { PendingBlockEventStore.getInstance(it) }
        val generation = outbox?.generation
        val testWriter = blockEventWriterForTests
        return storeScope.launch {
            try {
                if (testWriter != null) testWriter(event)
                else if (!checkNotNull(outbox).insert(event, checkNotNull(generation))) return@launch
                eventBroadcaster?.invoke(event.toWireMap())
            } catch (_: Exception) {
                System.err.println("[BlockReels] Could not persist pending block event")
            }
        }
    }

    fun getMetricsSnapshot(): Map<String, Any> {
        resetMetricsIfNewDay()
        val byAppMap = mutableMapOf<String, Int>()
        blockedByApp.forEach { (pkg, count) ->
            byAppMap[pkg] = count.get()
        }
        return mapOf(
            "totalBlockedToday" to totalBlockedToday.get(),
            "byApp" to byAppMap,
            "lastBlockedPackage" to lastBlockedPackage.get(),
            "lastBlockedTimestamp" to lastBlockedTimestamp.get(),
            "serviceUptimeMs" to (System.currentTimeMillis() - serviceStartTime.get())
        )
    }

    fun setAppTogglesInMemory(toggles: Map<String, Boolean>) {
        appToggles.clear()
        appToggles.putAll(toggles)
    }

    internal fun setMasterEnabledInMemory(enabled: Boolean) {
        masterEnabled.set(enabled)
    }

    suspend fun setAppToggles(context: Context, toggles: Map<String, Boolean>) =
        writeState(context) { store ->
            val json = JSONObject(toggles as Map<*, *>).toString()
            store.edit { prefs -> prefs[KEY_APP_TOGGLES_JSON] = json }
            setAppTogglesInMemory(toggles)
        }

    fun setInterventionConfigInMemory(config: InterventionConfig) {
        interventionConfig.set(config)
    }

    suspend fun setInterventionConfig(
        context: Context?,
        mode: InterventionMode,
        holdSec: Int,
        customMsg: String,
        sound: Boolean?,
        haptics: Boolean,
        pinProtected: Boolean
    ) = writeState(context) { store ->
        val current = interventionConfig.get()
        val resolvedSound = sound ?: current.soundEnabled
        val resolvedAudio = if (!resolvedSound) AudioAction.NONE
        else if (current.audioAction == AudioAction.NONE) AudioAction.SILENCE else current.audioAction
        val newConfig = current.copy(
            mode = mode,
            holdDurationSeconds = holdSec.coerceIn(3, 30),
            customMessage = customMsg,
            soundEnabled = resolvedSound,
            hapticsEnabled = haptics,
            pinProtected = pinProtected,
            audioAction = resolvedAudio
        )
        store.edit { prefs -> prefs.putInterventionConfig(newConfig) }
        interventionConfig.set(newConfig)
    }

    private fun MutablePreferences.putInterventionConfig(config: InterventionConfig) {
        this[KEY_INTERVENTION_MODE] = config.mode.name
        this[KEY_HOLD_DURATION] = config.holdDurationSeconds
        this[KEY_CUSTOM_MESSAGE] = config.customMessage
        this[KEY_SOUND_ENABLED] = config.audioAction != AudioAction.NONE
        this[KEY_HAPTICS_ENABLED] = config.hapticsEnabled
        this[KEY_PIN_PROTECTED] = config.pinProtected
        this[KEY_ALLOWANCE_MINUTES] = config.allowanceMinutes
        this[KEY_DAILY_PASS_LIMIT] = config.dailyPassLimit
        this[KEY_ENTRY_DELAY_SECONDS] = config.entryDelaySeconds
        this[KEY_SENSITIVITY] = config.sensitivity.name
        this[KEY_AUDIO_ACTION] = config.audioAction.name
        this[KEY_STRICT_MODE] = config.strictMode
    }

    fun setSchedulesInMemory(ruleList: List<ScheduleRule>) {
        schedules.set(ruleList)
    }

    suspend fun setSchedules(context: Context, rawSchedules: List<Map<String, Any>>) =
        writeState(context) { store ->
            val parsed = rawSchedules.mapNotNull(::parseSchedule)
            store.edit { prefs -> prefs[KEY_SCHEDULES_JSON] = recordsJson(rawSchedules) }
            schedules.set(parsed)
        }

    suspend fun setRules(context: Context, rawRules: List<Map<String, Any>>) =
        writeState(context) { store ->
            val parsed = rawRules.mapNotNull(::parseRule)
            store.edit { prefs -> prefs[KEY_RULES_JSON] = recordsJson(rawRules) }
            rules.clear()
            parsed.forEach { rules[it.packageName] = it }
        }

    internal suspend fun applyRestoredProtection(
        context: Context?,
        rawRules: List<Map<String, Any>>,
        rawSchedules: List<Map<String, Any>>,
        config: InterventionConfig,
        enabled: Boolean
    ) = writeState(context) { store ->
        val parsedRules = rawRules.map { requireNotNull(parseRule(it)) }
        val parsedSchedules = rawSchedules.map { requireNotNull(parseSchedule(it)) }
        val toggles = parsedRules.associate { it.packageName to (enabled && it.isEnabled) }
        store.edit { prefs ->
            prefs[KEY_RULES_JSON] = recordsJson(rawRules)
            prefs[KEY_SCHEDULES_JSON] = recordsJson(rawSchedules)
            prefs[KEY_APP_TOGGLES_JSON] = JSONObject(toggles).toString()
            prefs[KEY_MASTER_ENABLED] = enabled
            prefs.putInterventionConfig(config)
        }
        setAppTogglesInMemory(toggles)
        rules.clear()
        parsedRules.forEach { rules[it.packageName] = it }
        schedules.set(parsedSchedules)
        interventionConfig.set(config)
        masterEnabled.set(enabled)
    }

    private fun recordsJson(records: List<Map<String, Any>>): String =
        JSONArray().apply {
            records.forEach { put(JSONObject(it)) }
        }.toString()

    private fun parseSchedule(map: Map<String, Any>): ScheduleRule? {
        val id = map["id"] as? String ?: return null
        val packages = map["enabledPackages"] ?: map["enabledPackageNames"]
            ?: map["enabledApps"] ?: map["targetPackageNames"]
        val startHour = (map["startHour"] as? Number)?.toInt() ?: 0
        val startMinute = (map["startMinute"] as? Number)?.toInt() ?: 0
        val endHour = (map["endHour"] as? Number)?.toInt() ?: 23
        val endMinute = (map["endMinute"] as? Number)?.toInt() ?: 59
        val modeOverride = (map["modeOverride"] as? String)?.takeIf { it.isNotBlank() }
            ?.let(::parseInterventionMode)
        val scopeOverride = (map["scopeOverride"] as? String)?.takeIf { it.isNotBlank() }
            ?.let(::parseBlockingScope)
        return ScheduleRule(
            id = id,
            title = map["title"] as? String ?: "Schedule",
            isActive = map["isActive"] as? Boolean ?: true,
            daysOfWeek = (map["daysOfWeek"] as? List<*>)
                ?.mapNotNull { (it as? Number)?.toInt() }?.toSet() ?: emptySet(),
            startMinuteOfDay = startHour * 60 + startMinute,
            endMinuteOfDay = endHour * 60 + endMinute,
            enabledPackages = (packages as? List<*>)?.filterIsInstance<String>()?.toSet() ?: emptySet(),
            modeOverride = modeOverride,
            scopeOverride = scopeOverride
        )
    }

    private fun parseRule(map: Map<String, Any>): RuleConfig? {
        val packageName = map["packageName"] as? String ?: return null
        val modeStr = (map["interventionMode"] as? String)
            ?: (map["mode"] as? String)
        val mode = parseInterventionMode(modeStr)
        val scope = parseBlockingScope(map["scope"] as? String)
        val holdSec = (map["holdDurationSeconds"] as? Number)?.toInt()?.takeIf { it in 3..30 }
        val allowanceMin = (map["allowanceMinutes"] as? Number)?.toInt()?.takeIf { it in 1..30 }
        val dailyLimit = (map["dailyPassLimit"] as? Number)?.toInt()?.takeIf { it in 0..10 }
        val contentPolicy = parseContentBlockPolicy(map["contentPolicy"] as? String)
        val requestHide = map["requestHideSurfaces"] as? Boolean ?: (contentPolicy == ContentBlockPolicy.HIDE_SURFACES)
        val reelsGoal = (map["reelsDailyGoalMinutes"] as? Number)?.toInt()?.coerceIn(0, 180) ?: 0
        val feedGoal = (map["feedDailyGoalMinutes"] as? Number)?.toInt()?.coerceIn(0, 180) ?: 0
        return RuleConfig(
            packageName = packageName,
            isEnabled = map["isEnabled"] as? Boolean ?: true,
            mode = mode,
            holdDurationSeconds = holdSec,
            scope = scope,
            allowanceMinutes = allowanceMin,
            dailyPassLimit = dailyLimit,
            contentPolicy = contentPolicy,
            requestHideSurfaces = requestHide,
            reelsDailyGoalMinutes = reelsGoal,
            feedDailyGoalMinutes = feedGoal
        )
    }

    fun parseInterventionMode(value: String?): InterventionMode {
        return when (value?.uppercase()?.replace("_", "")) {
            "MINDFULPAUSE", "MINDFUL" -> InterventionMode.MINDFUL_PAUSE
            "STRICTBLOCK", "STRICT" -> InterventionMode.STRICT_BLOCK
            "REDIRECTHOME", "HOMEEXIT", "HOME" -> InterventionMode.REDIRECT_HOME
            "AUTOBACK", "BACK" -> InterventionMode.AUTO_BACK
            else -> InterventionMode.MINDFUL_PAUSE
        }
    }

    fun parseBlockingScope(value: String?): BlockingScope {
        return when (value?.uppercase()?.replace("_", "")) {
            "ENTIREAPP", "FULLAPP", "WHOLEAPP" -> BlockingScope.ENTIRE_APP
            else -> BlockingScope.FEED_ONLY
        }
    }

    fun parseContentBlockPolicy(value: String?): ContentBlockPolicy {
        return when (value?.uppercase()?.replace("_", "")) {
            "MAINFEEDONLY", "FEEDONLY", "FEED" -> ContentBlockPolicy.MAIN_FEED_ONLY
            "FEEDANDREELS", "BOTH" -> ContentBlockPolicy.FEED_AND_REELS
            "HIDESURFACES", "DONTSHOW", "HIDE" -> ContentBlockPolicy.HIDE_SURFACES
            else -> ContentBlockPolicy.REELS_ONLY
        }
    }

    fun parseSensitivity(value: String?): DetectionSensitivity {
        return when (value?.uppercase()) {
            "CONSERVATIVE" -> DetectionSensitivity.CONSERVATIVE
            "AGGRESSIVE" -> DetectionSensitivity.AGGRESSIVE
            else -> DetectionSensitivity.BALANCED
        }
    }

    fun parseAudioAction(value: String?): AudioAction {
        return when (value?.uppercase()) {
            "DUCK" -> AudioAction.DUCK
            "NONE", "OFF" -> AudioAction.NONE
            else -> AudioAction.SILENCE
        }
    }

    suspend fun updateSettings(context: Context, settings: Map<String, Any>) =
        updateSettingsDurably(settings, context)

    internal suspend fun updateSettings(settings: Map<String, Any>) =
        updateSettingsDurably(settings)

    private suspend fun updateSettingsDurably(
        settings: Map<String, Any>,
        context: Context? = null
    ) = writeState(context) { store ->
        val master = settings["isMasterEnabled"] as? Boolean
        store.edit { prefs ->
            if (master != null) prefs[KEY_MASTER_ENABLED] = master
        }
        if (master != null) masterEnabled.set(master)
    }

    fun resetMetrics() {
        totalBlockedToday.set(0)
        blockedByApp.clear()
        passesUsedByApp.clear()
        lastBlockedPackage.set("")
        lastBlockedTimestamp.set(0L)
        metricsDate.set(currentDateKey())
        persistMetrics()
    }

    private fun resetMetricsIfNewDay() {
        val today = currentDateKey()
        if (metricsDate.get() != today) {
            totalBlockedToday.set(0)
            blockedByApp.clear()
            passesUsedByApp.clear()
            lastBlockedPackage.set("")
            lastBlockedTimestamp.set(0L)
            metricsDate.set(today)
            persistMetrics()
        }
    }

    private fun persistMetrics() {
        val context = appContext ?: return
        storeScope.launch {
            try {
                writeState(context) { store ->
                    val total = totalBlockedToday.get()
                    val date = metricsDate.get()
                    val byApp = JSONObject()
                    blockedByApp.forEach { (pkg, count) -> byApp.put(pkg, count.get()) }
                    val passesByApp = JSONObject()
                    passesUsedByApp.forEach { (pkg, count) -> passesByApp.put(pkg, count.get()) }
                    store.edit { prefs ->
                        prefs[KEY_METRICS_TOTAL] = total
                        prefs[KEY_METRICS_DATE] = date
                        prefs[KEY_METRICS_BY_APP] = byApp.toString()
                        prefs[KEY_PASSES_BY_APP] = passesByApp.toString()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun currentDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun calendarIsoDay(calendar: Calendar): Int {
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        return if (day == Calendar.SUNDAY) 7 else day - 1
    }

    private fun parseAndSetSchedulesFromJson(jsonStr: String) {
        try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ScheduleRule>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val daysArray = obj.optJSONArray("daysOfWeek") ?: JSONArray()
                val days = mutableSetOf<Int>()
                for (d in 0 until daysArray.length()) days.add(daysArray.getInt(d))

                val pkgsArray = obj.optJSONArray("enabledPackages")
                    ?: obj.optJSONArray("enabledPackageNames")
                    ?: obj.optJSONArray("enabledApps")
                    ?: obj.optJSONArray("targetPackageNames")
                    ?: JSONArray()
                val pkgs = mutableSetOf<String>()
                for (p in 0 until pkgsArray.length()) pkgs.add(pkgsArray.getString(p))

                val startH = obj.optInt("startHour", 0)
                val startM = obj.optInt("startMinute", 0)
                val endH = obj.optInt("endHour", 23)
                val endM = obj.optInt("endMinute", 59)
                val modeOverride = obj.optString("modeOverride", "").takeIf { it.isNotBlank() }
                    ?.let(::parseInterventionMode)
                val scopeOverride = obj.optString("scopeOverride", "").takeIf { it.isNotBlank() }
                    ?.let(::parseBlockingScope)

                list.add(
                    ScheduleRule(
                        id = obj.getString("id"),
                        title = obj.optString("title", obj.optString("name", "Schedule")),
                        isActive = obj.optBoolean("isActive", true),
                        daysOfWeek = days,
                        startMinuteOfDay = startH * 60 + startM,
                        endMinuteOfDay = endH * 60 + endM,
                        enabledPackages = pkgs,
                        modeOverride = modeOverride,
                        scopeOverride = scopeOverride
                    )
                )
            }
            schedules.set(list)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseAndSetRulesFromJson(jsonStr: String) {
        try {
            val array = JSONArray(jsonStr)
            val parsed = mutableListOf<RuleConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val map = mutableMapOf<String, Any>()
                obj.keys().forEach { key ->
                    if (!obj.isNull(key)) map[key] = obj.get(key)
                }
                parseRule(map)?.let { parsed.add(it) }
            }
            rules.clear()
            parsed.forEach { rules[it.packageName] = it }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mapPackageToAppType(pkg: String): String = when (pkg) {
        "com.instagram.android" -> "INSTAGRAM"
        "com.google.android.youtube" -> "YOUTUBE"
        "com.facebook.katana", "com.facebook.lite" -> "FACEBOOK"
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme", "com.ss.android.ugc.aweme.lite" -> "TIKTOK"
        "com.snapchat.android" -> "SNAPCHAT"
        else -> "UNKNOWN"
    }
}
