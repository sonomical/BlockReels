// SPDX-License-Identifier: MIT
package com.blockreels.app.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blockreels.app.BlockReelsAccessibilityService
import com.blockreels.app.MonitoringForegroundService
import com.blockreels.app.data.ActiveAllowanceItem
import com.blockreels.app.data.AnalyticsSummary
import com.blockreels.app.data.AppSettingsState
import com.blockreels.app.data.AppThemeMode
import com.blockreels.app.data.BlockReelsRepository
import com.blockreels.app.data.CryptoPinHelper
import com.blockreels.app.data.DayCountPoint
import com.blockreels.app.data.HistoryRecord
import com.blockreels.app.data.PlatformBreakdownItem
import com.blockreels.app.data.ProtectionStatus
import com.blockreels.app.data.ScheduleItem
import com.blockreels.app.data.StatsRange
import com.blockreels.app.data.SupportedTargets
import com.blockreels.app.data.SystemHealthState
import com.blockreels.app.data.TargetAppConfig
import com.blockreels.app.state.AudioAction
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.DetectionSensitivity
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.state.NativeEventBus
import com.blockreels.app.state.NativeStateStore
import com.blockreels.app.state.ScheduleRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

@Immutable
data class BlockReelsUiState(
    val isLoading: Boolean = true,
    val settings: AppSettingsState = AppSettingsState(),
    val targets: List<TargetAppConfig> = SupportedTargets.defaultTargets,
    val schedules: List<ScheduleItem> = emptyList(),
    val history: List<HistoryRecord> = emptyList(),
    val systemHealth: SystemHealthState = SystemHealthState(),
    val activeAllowances: List<ActiveAllowanceItem> = emptyList(),
    val passesUsedToday: Map<String, Int> = emptyMap(),
    val selectedStatsRange: StatsRange = StatsRange.WEEK,
    val analytics: AnalyticsSummary = AnalyticsSummary(),
    val nowMillis: Long = System.currentTimeMillis()
) {
    val protectionStatus: ProtectionStatus
        get() = when {
            !systemHealth.accessibilityEnabled -> ProtectionStatus.PERMISSION_NEEDED
            !settings.masterEnabled -> ProtectionStatus.PAUSED
            else -> ProtectionStatus.PROTECTED
        }

    val activeQuickBoost: ScheduleItem?
        get() = schedules.firstOrNull {
            it.isQuickBoost && it.isActive && (it.expiresAtMillis ?: 0L) > nowMillis
        }

    val regularSchedules: List<ScheduleItem>
        get() = schedules.filterNot { it.isQuickBoost }

    val enabledTargetsCount: Int
        get() = targets.count { it.isEnabled }

    val installedTargetsCount: Int
        get() = targets.count { it.isInstalled }

    val activeRegularSchedulesCount: Int
        get() = regularSchedules.count { it.isActive }

    val totalPassesUsedToday: Int
        get() = passesUsedToday.values.sum()

    val blocksLast7Days: Int
        get() = analytics.dailySeries.sumOf { it.count }

    val topBlockedPlatform: PlatformBreakdownItem?
        get() = analytics.platformBreakdown.firstOrNull()
}

class BlockReelsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BlockReelsRepository(application)

    private val _uiState = MutableStateFlow(BlockReelsUiState())
    val uiState: StateFlow<BlockReelsUiState> = _uiState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private var boostTickerJob: Job? = null

    init {
        NativeEventBus.bindStateStore()
        loadInitialData()
        observeNativeEvents()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val loaded = repository.loadAll()
            val health = withContext(Dispatchers.IO) { readSystemHealth() }
            val analytics = withContext(Dispatchers.Default) {
                computeAnalytics(loaded.history, _uiState.value.selectedStatsRange)
            }
            val now = System.currentTimeMillis()
            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    settings = loaded.settings,
                    targets = loaded.targets,
                    schedules = loaded.schedules,
                    history = loaded.history,
                    systemHealth = health,
                    activeAllowances = readActiveAllowances(),
                    passesUsedToday = NativeStateStore.getAllPassesUsedToday(),
                    analytics = analytics,
                    nowMillis = now
                )
            }
            reconcileForegroundService(loaded.settings.masterEnabled, health.accessibilityEnabled)
            syncBoostTicker()
        }
    }

    private fun observeNativeEvents() {
        viewModelScope.launch {
            NativeEventBus.events.collect { event ->
                when (event["eventType"] as? String) {
                    "SERVICE_STATE_CHANGED" -> refreshSystemHealth()
                    "PASS_GRANTED" -> {
                        _uiState.update {
                            it.copy(
                                activeAllowances = readActiveAllowances(),
                                passesUsedToday = NativeStateStore.getAllPassesUsedToday()
                            )
                        }
                        syncBoostTicker()
                    }
                    "REEL_BLOCKED" -> {
                        val drained = repository.drainPendingNativeEvents()
                        val incoming = if (drained.isNotEmpty()) {
                            drained
                        } else {
                            val pkg = event["packageName"] as? String ?: return@collect
                            listOf(
                                HistoryRecord(
                                    id = event["eventId"] as? String ?: UUID.randomUUID().toString(),
                                    timestampMillis = (event["timestamp"] as? Number)?.toLong()
                                        ?: System.currentTimeMillis(),
                                    packageName = pkg,
                                    appLabel = SupportedTargets.labelForPackage(pkg),
                                    platformKey = SupportedTargets.platformKeyForPackage(pkg),
                                    interventionMode = NativeStateStore.parseInterventionMode(
                                        event["interventionMode"] as? String
                                    ),
                                    diagnosticReason = (event["detectionReason"] as? String)
                                        ?: (event["diagnosticCode"] as? String)
                                        ?: "REEL_CONTAINER_MATCH"
                                )
                            )
                        }
                        appendHistoryRecords(incoming)
                    }
                }
            }
        }
    }

    private fun readActiveAllowances(): List<ActiveAllowanceItem> {
        return BlockReelsAccessibilityService.getActiveAllowancesRemainingMs()
            .entries
            .sortedBy { it.value }
            .map { (pkg, rem) ->
                ActiveAllowanceItem(
                    packageName = pkg,
                    appLabel = SupportedTargets.labelForPackage(pkg),
                    platformKey = SupportedTargets.platformKeyForPackage(pkg),
                    remainingMillis = rem
                )
            }
    }

    private fun syncBoostTicker() {
        val needsTick = _uiState.value.activeQuickBoost != null ||
            _uiState.value.activeAllowances.isNotEmpty()
        if (!needsTick) {
            boostTickerJob?.cancel()
            boostTickerJob = null
            return
        }
        if (boostTickerJob?.isActive == true) return
        boostTickerJob = viewModelScope.launch {
            while (true) {
                delay(15_000L)
                val now = System.currentTimeMillis()
                val currentSchedules = _uiState.value.schedules
                val hasExpired = currentSchedules.any {
                    it.isQuickBoost && (it.expiresAtMillis ?: 0L) <= now
                }
                val allowances = readActiveAllowances()
                if (hasExpired) {
                    val remaining = currentSchedules.filter {
                        !it.isQuickBoost || (it.expiresAtMillis ?: 0L) > now
                    }
                    _uiState.update {
                        it.copy(schedules = remaining, activeAllowances = allowances, nowMillis = now)
                    }
                    repository.saveSchedules(_uiState.value.settings, _uiState.value.targets, remaining)
                    if (remaining.none { it.isQuickBoost } && allowances.isEmpty()) break
                } else {
                    _uiState.update { it.copy(activeAllowances = allowances, nowMillis = now) }
                    if (_uiState.value.activeQuickBoost == null && allowances.isEmpty()) break
                }
            }
            boostTickerJob = null
        }
    }

    fun refreshSystemHealth() {
        viewModelScope.launch {
            val health = withContext(Dispatchers.IO) { readSystemHealth() }
            val drained = repository.drainPendingNativeEvents()
            val now = System.currentTimeMillis()
            val allowances = readActiveAllowances()
            val passes = NativeStateStore.getAllPassesUsedToday()
            if (drained.isNotEmpty()) {
                val merged = mergeHistory(_uiState.value.history, drained)
                val analytics = withContext(Dispatchers.Default) {
                    computeAnalytics(merged, _uiState.value.selectedStatsRange)
                }
                _uiState.update { current ->
                    current.copy(
                        systemHealth = health,
                        activeAllowances = allowances,
                        passesUsedToday = passes,
                        history = merged,
                        analytics = analytics,
                        nowMillis = now
                    )
                }
                repository.saveHistory(merged)
            } else {
                _uiState.update { current ->
                    current.copy(
                        systemHealth = health,
                        activeAllowances = allowances,
                        passesUsedToday = passes,
                        nowMillis = now
                    )
                }
            }
            reconcileForegroundService(_uiState.value.settings.masterEnabled, health.accessibilityEnabled)
            syncBoostTicker()
        }
    }

    private fun readSystemHealth(): SystemHealthState {
        val app = getApplication<Application>()
        return SystemHealthState(
            accessibilityEnabled = NativeEventBus.isAccessibilityServiceEnabled(app),
            batteryOptimizationIgnored = NativeEventBus.isBatteryOptimizationIgnored(app),
            canDrawOverlays = NativeEventBus.canDrawOverlays(app),
            monitoringServiceRunning = MonitoringForegroundService.isRunning()
        )
    }

    private fun reconcileForegroundService(masterEnabled: Boolean, accessibilityEnabled: Boolean) {
        val app = getApplication<Application>()
        if (masterEnabled && accessibilityEnabled) {
            if (!MonitoringForegroundService.isRunning()) {
                MonitoringForegroundService.start(app)
            }
        } else if (MonitoringForegroundService.isRunning()) {
            MonitoringForegroundService.stop(app)
        }
    }

    fun toggleMasterProtection(enabled: Boolean) {
        updateSettings { it.copy(masterEnabled = enabled) }
        reconcileForegroundService(enabled, _uiState.value.systemHealth.accessibilityEnabled)
        emitMessage(if (enabled) "Shield enabled" else "Shield paused")
    }

    fun toggleTargetApp(packageName: String, enabled: Boolean) {
        val updated = _uiState.value.targets.map {
            if (it.packageName == packageName) it.copy(isEnabled = enabled) else it
        }
        persistTargets(updated)
    }

    fun updateTargetRule(updatedRule: TargetAppConfig) {
        val updated = _uiState.value.targets.map {
            if (it.packageName == updatedRule.packageName) updatedRule else it
        }
        persistTargets(updated)
        emitMessage("${updatedRule.displayName} rule updated")
    }

    fun setTargetAppMode(packageName: String, mode: InterventionMode) {
        val updated = _uiState.value.targets.map {
            if (it.packageName == packageName) it.copy(mode = mode) else it
        }
        persistTargets(updated)
    }

    fun setTargetAppScope(packageName: String, scope: BlockingScope) {
        val updated = _uiState.value.targets.map {
            if (it.packageName == packageName) it.copy(scope = scope) else it
        }
        persistTargets(updated)
    }

    fun updateGlobalMode(mode: InterventionMode) {
        updateSettings { it.copy(globalMode = mode) }
    }

    fun updateHoldDuration(seconds: Int) {
        updateSettings { it.copy(holdDurationSeconds = seconds.coerceIn(3, 30)) }
    }

    fun updateAllowanceMinutes(minutes: Int) {
        updateSettings { it.copy(allowanceMinutes = minutes.coerceIn(1, 30)) }
    }

    fun updateDailyPassLimit(limit: Int) {
        updateSettings { it.copy(dailyPassLimit = limit.coerceIn(0, 10)) }
    }

    fun updateEntryDelaySeconds(seconds: Int) {
        updateSettings { it.copy(entryDelaySeconds = seconds.coerceIn(0, 10)) }
    }

    fun updateSensitivity(sensitivity: DetectionSensitivity) {
        updateSettings { it.copy(sensitivity = sensitivity) }
    }

    fun updateAudioAction(action: AudioAction) {
        updateSettings {
            it.copy(audioAction = action, soundEnabled = action != AudioAction.NONE)
        }
    }

    fun updateCustomMessage(message: String) {
        val cleaned = message.trim().ifEmpty { "Take a deep breath. Do you really want to scroll right now?" }
        updateSettings { it.copy(customMessage = cleaned.take(240)) }
        emitMessage("Pause prompt updated")
    }

    fun setHapticsEnabled(enabled: Boolean) {
        updateSettings { it.copy(hapticsEnabled = enabled) }
    }

    fun setStrictMode(enabled: Boolean) {
        updateSettings { it.copy(strictMode = enabled) }
        emitMessage(if (enabled) "Strict Lock enforced across all targets" else "Strict Lock disabled")
    }

    fun setThemeMode(mode: AppThemeMode) {
        updateSettings { it.copy(themeMode = mode) }
    }

    fun completeOnboarding() {
        updateSettings { it.copy(onboardingCompleted = true) }
    }

    fun resetOnboarding() {
        updateSettings { it.copy(onboardingCompleted = false) }
    }

    fun revokeTemporaryAllowance(packageName: String) {
        BlockReelsAccessibilityService.revokeTemporaryAllowance(packageName)
        _uiState.update { it.copy(activeAllowances = readActiveAllowances()) }
        syncBoostTicker()
        emitMessage("Pass revoked for ${SupportedTargets.labelForPackage(packageName)}")
    }

    fun enablePinLock(pin: String) {
        viewModelScope.launch {
            val (salt, hash) = withContext(Dispatchers.Default) {
                val s = CryptoPinHelper.generateSalt()
                s to CryptoPinHelper.hashPin(pin, s)
            }
            updateSettings { it.copy(pinProtected = true, pinHash = hash, pinSalt = salt) }
            emitMessage("PIN protection enabled")
        }
    }

    fun disablePinLock() {
        updateSettings { it.copy(pinProtected = false, pinHash = "", pinSalt = "") }
        emitMessage("PIN protection removed")
    }

    fun verifyPin(candidate: String): Boolean {
        val settings = _uiState.value.settings
        return CryptoPinHelper.verifyPin(candidate, settings.pinHash, settings.pinSalt)
    }

    fun startQuickBoost(
        durationMinutes: Int,
        modeOverride: InterventionMode = InterventionMode.STRICT_BLOCK
    ) {
        val now = System.currentTimeMillis()
        val startCal = Calendar.getInstance().apply { timeInMillis = now }
        val endCal = Calendar.getInstance().apply { timeInMillis = now + durationMinutes * 60_000L }
        val allPackages = _uiState.value.targets.map { it.packageName }.toSet()
        val boost = ScheduleItem(
            id = "${ScheduleRule.QUICK_BOOST_ID_PREFIX}$now",
            title = "Focus Lock (${durationMinutes}m)",
            isActive = true,
            daysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7),
            startHour = startCal.get(Calendar.HOUR_OF_DAY),
            startMinute = startCal.get(Calendar.MINUTE),
            endHour = endCal.get(Calendar.HOUR_OF_DAY),
            endMinute = endCal.get(Calendar.MINUTE),
            enabledPackages = allPackages,
            modeOverride = modeOverride,
            isQuickBoost = true,
            expiresAtMillis = now + durationMinutes * 60_000L
        )
        val filtered = _uiState.value.schedules.filterNot { it.isQuickBoost } + boost
        persistSchedules(filtered)
        if (!_uiState.value.settings.masterEnabled) {
            updateSettings { it.copy(masterEnabled = true) }
        }
        syncBoostTicker()
        emitMessage("Focus Lock active for ${durationMinutes}m (${modeOverride.shortLabel})")
    }

    fun cancelQuickBoost() {
        val filtered = _uiState.value.schedules.filterNot { it.isQuickBoost }
        persistSchedules(filtered)
        syncBoostTicker()
        emitMessage("Focus Lock ended")
    }

    fun upsertSchedule(schedule: ScheduleItem) {
        val current = _uiState.value.schedules
        val exists = current.any { it.id == schedule.id }
        val updated = if (exists) {
            current.map { if (it.id == schedule.id) schedule else it }
        } else {
            current + schedule
        }
        persistSchedules(updated)
        emitMessage(if (exists) "Schedule updated" else "Schedule created")
    }

    fun toggleSchedule(scheduleId: String, active: Boolean) {
        val updated = _uiState.value.schedules.map {
            if (it.id == scheduleId) it.copy(isActive = active) else it
        }
        persistSchedules(updated)
    }

    fun deleteSchedule(scheduleId: String) {
        val updated = _uiState.value.schedules.filterNot { it.id == scheduleId }
        persistSchedules(updated)
        emitMessage("Schedule deleted")
    }

    fun selectStatsRange(range: StatsRange) {
        val historySnapshot = _uiState.value.history
        viewModelScope.launch {
            val analytics = withContext(Dispatchers.Default) {
                computeAnalytics(historySnapshot, range)
            }
            _uiState.update { it.copy(selectedStatsRange = range, analytics = analytics) }
        }
    }

    fun recordPreviewInterception() {
        val record = HistoryRecord(
            id = "preview_${UUID.randomUUID()}",
            timestampMillis = System.currentTimeMillis(),
            packageName = "com.instagram.android",
            appLabel = "Overlay Test",
            platformKey = "INSTAGRAM",
            interventionMode = _uiState.value.settings.globalMode,
            diagnosticReason = "INTERACTIVE_PREVIEW"
        )
        appendHistoryRecords(listOf(record))
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            val emptyAnalytics = withContext(Dispatchers.Default) {
                computeAnalytics(emptyList(), _uiState.value.selectedStatsRange)
            }
            _uiState.update { current ->
                current.copy(
                    history = emptyList(),
                    passesUsedToday = emptyMap(),
                    analytics = emptyAnalytics
                )
            }
            emitMessage("Activity history cleared")
        }
    }

    fun exportBackupJson(): String {
        val state = _uiState.value
        return repository.exportBackupJson(state.settings, state.targets, state.schedules)
    }

    fun importBackupJson(rawJson: String): Boolean {
        val result = repository.parseAndValidateBackupJson(rawJson, _uiState.value.settings)
        return result.fold(
            onSuccess = { (restoredSettings, restoredTargets, restoredSchedules) ->
                viewModelScope.launch {
                    _uiState.update {
                        it.copy(
                            settings = restoredSettings,
                            targets = restoredTargets,
                            schedules = restoredSchedules
                        )
                    }
                    repository.saveSettings(restoredSettings, restoredTargets, restoredSchedules)
                    repository.saveTargets(restoredSettings, restoredTargets, restoredSchedules)
                    repository.saveSchedules(restoredSettings, restoredTargets, restoredSchedules)
                    syncBoostTicker()
                    emitMessage("Backup restored")
                }
                true
            },
            onFailure = { err ->
                emitMessage(err.message ?: "Invalid backup JSON")
                false
            }
        )
    }

    fun openAccessibilitySettings() = NativeEventBus.openAccessibilitySettings(getApplication())
    fun openBatterySettings() = NativeEventBus.openBatteryOptimizationSettings(getApplication())
    fun openOverlaySettings() = NativeEventBus.openOverlaySettings(getApplication())
    fun openNotificationSettings() = NativeEventBus.openNotificationSettings(getApplication())

    private fun updateSettings(transform: (AppSettingsState) -> AppSettingsState) {
        val next = transform(_uiState.value.settings)
        _uiState.update { it.copy(settings = next) }
        viewModelScope.launch {
            repository.saveSettings(next, _uiState.value.targets, _uiState.value.schedules)
        }
    }

    private fun persistTargets(targets: List<TargetAppConfig>) {
        _uiState.update { it.copy(targets = targets) }
        viewModelScope.launch {
            repository.saveTargets(_uiState.value.settings, targets, _uiState.value.schedules)
        }
    }

    private fun persistSchedules(schedules: List<ScheduleItem>) {
        _uiState.update { it.copy(schedules = schedules, nowMillis = System.currentTimeMillis()) }
        viewModelScope.launch {
            repository.saveSchedules(_uiState.value.settings, _uiState.value.targets, schedules)
        }
    }

    private fun appendHistoryRecords(records: List<HistoryRecord>) {
        val merged = mergeHistory(_uiState.value.history, records)
        val range = _uiState.value.selectedStatsRange
        viewModelScope.launch {
            val analytics = withContext(Dispatchers.Default) {
                computeAnalytics(merged, range)
            }
            _uiState.update { current ->
                current.copy(
                    history = merged,
                    analytics = analytics,
                    nowMillis = System.currentTimeMillis()
                )
            }
            repository.saveHistory(merged)
        }
    }

    private fun mergeHistory(
        existing: List<HistoryRecord>,
        incoming: List<HistoryRecord>
    ): List<HistoryRecord> {
        val map = LinkedHashMap<String, HistoryRecord>(existing.size + incoming.size)
        (incoming + existing).sortedByDescending { it.timestampMillis }.forEach {
            map.putIfAbsent(it.id, it)
        }
        return map.values.take(500).toList()
    }

    private fun emitMessage(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    private fun computeAnalytics(history: List<HistoryRecord>, range: StatsRange): AnalyticsSummary {
        val cal = Calendar.getInstance()
        val shortWeekdays = DateFormatSymbols.getInstance(Locale.US).shortWeekdays

        fun Calendar.toDateKey(): Int =
            get(Calendar.YEAR) * 10_000 + (get(Calendar.MONTH) + 1) * 100 + get(Calendar.DAY_OF_MONTH)

        val todayKey = cal.toDateKey()

        val cutoffMillis = range.days?.let { days ->
            (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -(days - 1))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } ?: Long.MIN_VALUE

        val chartKeys = IntArray(7)
        val chartLabels = Array(7) { "" }
        val chartCounts = IntArray(7)
        val chartIndexByKey = HashMap<Int, Int>(16)
        val dayCursor = cal.clone() as Calendar
        for (i in 6 downTo 0) {
            val k = dayCursor.toDateKey()
            chartKeys[i] = k
            chartLabels[i] = shortWeekdays[dayCursor.get(Calendar.DAY_OF_WEEK)].take(3)
            chartIndexByKey[k] = i
            dayCursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        val distinctDateKeys = HashSet<Int>(64)
        val hourCounts = IntArray(24)
        val platformCounts = HashMap<String, Int>(8)

        var todayCount = 0
        var todayMinutes = 0.0
        var scopedCount = 0
        var scopedMinutes = 0.0

        val recCal = Calendar.getInstance()
        for (record in history) {
            recCal.timeInMillis = record.timestampMillis
            val recKey = recCal.toDateKey()
            distinctDateKeys.add(recKey)

            chartIndexByKey[recKey]?.let { idx -> chartCounts[idx]++ }

            if (recKey == todayKey) {
                todayCount++
                todayMinutes += record.estimatedMinutesSaved
            }
            if (record.timestampMillis >= cutoffMillis) {
                scopedCount++
                scopedMinutes += record.estimatedMinutesSaved
                hourCounts[recCal.get(Calendar.HOUR_OF_DAY)]++
                platformCounts[record.platformKey] = (platformCounts[record.platformKey] ?: 0) + 1
            }
        }

        val series = List(7) { i ->
            DayCountPoint(
                dayLabel = chartLabels[i],
                dateKey = chartKeys[i],
                count = chartCounts[i],
                isToday = i == 6
            )
        }

        var streak = 0
        val streakCursor = cal.clone() as Calendar
        while (distinctDateKeys.contains(streakCursor.toDateKey())) {
            streak++
            streakCursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        var peakHourIdx = -1
        var peakHourMax = 0
        for (h in 0 until 24) {
            if (hourCounts[h] > peakHourMax) {
                peakHourMax = hourCounts[h]
                peakHourIdx = h
            }
        }
        val peakHourLabel = if (peakHourIdx >= 0) {
            val nextHour = (peakHourIdx + 1) % 24
            String.format("%02d:00–%02d:00", peakHourIdx, nextHour)
        } else "—"

        val totalForFraction = scopedCount.coerceAtLeast(1).toFloat()
        val breakdown = platformCounts.entries
            .sortedByDescending { it.value }
            .map { (key, count) ->
                PlatformBreakdownItem(
                    platformKey = key,
                    label = SupportedTargets.platformDisplayName(key),
                    count = count,
                    fraction = count / totalForFraction
                )
            }

        val dayDivisor = (range.days ?: 30).coerceAtLeast(1)

        return AnalyticsSummary(
            totalBlocks = scopedCount,
            blocksToday = todayCount,
            minutesSaved = scopedMinutes.roundToInt(),
            minutesSavedToday = todayMinutes.roundToInt(),
            streakDays = streak,
            peakHourLabel = peakHourLabel,
            averageDailyBlocks = scopedCount.toDouble() / dayDivisor,
            dailySeries = series,
            platformBreakdown = breakdown
        )
    }
}
