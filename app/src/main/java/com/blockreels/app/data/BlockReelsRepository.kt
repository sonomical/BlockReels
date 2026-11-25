// SPDX-License-Identifier: MIT
package com.blockreels.app.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.blockreels.app.state.AudioAction
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.DetectionSensitivity
import com.blockreels.app.state.InterventionConfig
import com.blockreels.app.state.InterventionMode
import com.blockreels.app.state.NativeStateStore
import com.blockreels.app.state.PendingBlockEventStore
import com.blockreels.app.state.ScheduleRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.uiDataStore by preferencesDataStore(name = "blockreels_ui_prefs")

class BlockReelsRepository(private val context: Context) {

    private val KEY_MASTER = booleanPreferencesKey("ui_master_enabled")
    private val KEY_GLOBAL_MODE = stringPreferencesKey("ui_global_mode")
    private val KEY_HOLD_SECONDS = intPreferencesKey("ui_hold_seconds")
    private val KEY_ALLOWANCE_MINUTES = intPreferencesKey("ui_allowance_minutes")
    private val KEY_DAILY_PASS_LIMIT = intPreferencesKey("ui_daily_pass_limit")
    private val KEY_ENTRY_DELAY_SECONDS = intPreferencesKey("ui_entry_delay_seconds")
    private val KEY_SENSITIVITY = stringPreferencesKey("ui_detection_sensitivity")
    private val KEY_AUDIO_ACTION = stringPreferencesKey("ui_audio_action")
    private val KEY_CUSTOM_HEADLINE = stringPreferencesKey("ui_custom_headline")
    private val KEY_CUSTOM_MSG = stringPreferencesKey("ui_custom_message")
    private val KEY_DEFAULT_CONTENT_POLICY = stringPreferencesKey("ui_default_content_policy")
    private val KEY_DEFAULT_HIDE_SURFACES = booleanPreferencesKey("ui_default_hide_surfaces")
    private val KEY_DEFAULT_REELS_GOAL = intPreferencesKey("ui_default_reels_goal")
    private val KEY_DEFAULT_FEED_GOAL = intPreferencesKey("ui_default_feed_goal")
    private val KEY_SOUND = booleanPreferencesKey("ui_sound_enabled")
    private val KEY_HAPTICS = booleanPreferencesKey("ui_haptics_enabled")
    private val KEY_PIN_PROTECTED = booleanPreferencesKey("ui_pin_protected")
    private val KEY_PIN_HASH = stringPreferencesKey("ui_pin_hash")
    private val KEY_PIN_SALT = stringPreferencesKey("ui_pin_salt")
    private val KEY_STRICT_MODE = booleanPreferencesKey("ui_strict_mode")
    private val KEY_THEME_MODE = stringPreferencesKey("ui_theme_mode")
    private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("ui_dynamic_color")
    private val KEY_COLOR_SEED = stringPreferencesKey("ui_color_seed")
    private val KEY_CONTRAST_LEVEL = stringPreferencesKey("ui_contrast_level")
    private val KEY_CORNER_STYLE = stringPreferencesKey("ui_corner_style")
    private val KEY_FONT_SCALE = stringPreferencesKey("ui_font_scale")
    private val KEY_ICON_STYLE = stringPreferencesKey("ui_icon_style")
    private val KEY_ONBOARDING_DONE = booleanPreferencesKey("ui_onboarding_done")
    private val KEY_TARGETS_JSON = stringPreferencesKey("ui_targets_json")
    private val KEY_SCHEDULES_JSON = stringPreferencesKey("ui_schedules_json")
    private val KEY_HISTORY_JSON = stringPreferencesKey("ui_history_json")

    data class LoadedSnapshot(
        val settings: AppSettingsState,
        val targets: List<TargetAppConfig>,
        val schedules: List<ScheduleItem>,
        val history: List<HistoryRecord>
    )

    suspend fun loadAll(): LoadedSnapshot = withContext(Dispatchers.IO) {
        NativeStateStore.initialize(context).await()
        val prefs = context.uiDataStore.data.first()

        val legacySound = prefs[KEY_SOUND] ?: true
        val audioAction = prefs[KEY_AUDIO_ACTION]?.let(NativeStateStore::parseAudioAction)
            ?: if (legacySound) AudioAction.SILENCE else AudioAction.NONE

        val settings = AppSettingsState(
            masterEnabled = prefs[KEY_MASTER] ?: NativeStateStore.isMasterEnabled(),
            globalMode = NativeStateStore.parseInterventionMode(prefs[KEY_GLOBAL_MODE]),
            holdDurationSeconds = (prefs[KEY_HOLD_SECONDS] ?: 5).coerceIn(3, 30),
            allowanceMinutes = (prefs[KEY_ALLOWANCE_MINUTES] ?: 5).coerceIn(1, 30),
            dailyPassLimit = (prefs[KEY_DAILY_PASS_LIMIT] ?: 0).coerceIn(0, 10),
            entryDelaySeconds = (prefs[KEY_ENTRY_DELAY_SECONDS] ?: 0).coerceIn(0, 10),
            sensitivity = NativeStateStore.parseSensitivity(prefs[KEY_SENSITIVITY]),
            audioAction = audioAction,
            customMessageHeadline = prefs[KEY_CUSTOM_HEADLINE] ?: "Pause before you scroll.",
            customMessage = prefs[KEY_CUSTOM_MSG]
                ?: "Take a deep breath. Do you really want to spend your focus here right now?",
            defaultContentPolicy = NativeStateStore.parseContentBlockPolicy(prefs[KEY_DEFAULT_CONTENT_POLICY]),
            defaultRequestHideSurfaces = prefs[KEY_DEFAULT_HIDE_SURFACES] ?: false,
            defaultReelsGoalMinutes = (prefs[KEY_DEFAULT_REELS_GOAL] ?: 15).coerceIn(0, 180),
            defaultFeedGoalMinutes = (prefs[KEY_DEFAULT_FEED_GOAL] ?: 20).coerceIn(0, 180),
            soundEnabled = audioAction != AudioAction.NONE,
            hapticsEnabled = prefs[KEY_HAPTICS] ?: true,
            pinProtected = prefs[KEY_PIN_PROTECTED] ?: false,
            pinHash = prefs[KEY_PIN_HASH] ?: "",
            pinSalt = prefs[KEY_PIN_SALT] ?: "",
            strictMode = prefs[KEY_STRICT_MODE] ?: false,
            themeMode = parseTheme(prefs[KEY_THEME_MODE]),
            dynamicColorEnabled = prefs[KEY_DYNAMIC_COLOR] ?: false,
            colorSeed = BackupCodec.parseColorSeed(prefs[KEY_COLOR_SEED]),
            contrastLevel = BackupCodec.parseContrastLevel(prefs[KEY_CONTRAST_LEVEL]),
            cornerStyle = BackupCodec.parseCornerStyle(prefs[KEY_CORNER_STYLE]),
            fontScale = BackupCodec.parseFontScale(prefs[KEY_FONT_SCALE]),
            iconStyle = BackupCodec.parseIconStyle(prefs[KEY_ICON_STYLE]),
            onboardingCompleted = prefs[KEY_ONBOARDING_DONE] ?: false
        )

        val storedTargets = prefs[KEY_TARGETS_JSON]?.let(::parseTargetsJson) ?: SupportedTargets.defaultTargets
        val targets = storedTargets.map { target ->
            target.copy(isInstalled = isPackageInstalled(target.packageName))
        }

        val now = System.currentTimeMillis()
        val schedules = (prefs[KEY_SCHEDULES_JSON]?.let(::parseSchedulesJson) ?: emptyList())
            .filter { !it.isQuickBoost || (it.expiresAtMillis ?: 0L) > now }

        val storedHistory = prefs[KEY_HISTORY_JSON]?.let(::parseHistoryJson) ?: emptyList()
        val drained = drainPendingNativeEvents()
        val mergedHistory = mergeHistory(storedHistory, drained)
        if (drained.isNotEmpty()) {
            saveHistoryInternal(mergedHistory)
        }

        syncToNativeStateStore(settings, targets, schedules)

        LoadedSnapshot(
            settings = settings,
            targets = targets,
            schedules = schedules,
            history = mergedHistory
        )
    }

    suspend fun drainPendingNativeEvents(): List<HistoryRecord> = withContext(Dispatchers.IO) {
        val outbox = PendingBlockEventStore.getInstance(context)
        val pending = outbox.readPending(100)
        if (pending.isEmpty()) return@withContext emptyList()
        val mapped = pending.mapNotNull { ev ->
            val eventId = ev["eventId"] as? String ?: return@mapNotNull null
            val pkg = ev["packageName"] as? String ?: return@mapNotNull null
            val ts = (ev["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            HistoryRecord(
                id = eventId,
                timestampMillis = ts,
                packageName = pkg,
                appLabel = SupportedTargets.labelForPackage(pkg),
                platformKey = SupportedTargets.platformKeyForPackage(pkg),
                interventionMode = NativeStateStore.parseInterventionMode(ev["interventionMode"] as? String),
                diagnosticReason = (ev["detectionReason"] as? String) ?: "REEL_CONTAINER_MATCH"
            )
        }
        outbox.acknowledge(mapped.map { it.id })
        mapped
    }

    suspend fun saveSettings(
        settings: AppSettingsState,
        targets: List<TargetAppConfig>,
        schedules: List<ScheduleItem>
    ) = withContext(Dispatchers.IO) {
        context.uiDataStore.edit { prefs ->
            prefs[KEY_MASTER] = settings.masterEnabled
            prefs[KEY_GLOBAL_MODE] = settings.globalMode.name
            prefs[KEY_HOLD_SECONDS] = settings.holdDurationSeconds
            prefs[KEY_ALLOWANCE_MINUTES] = settings.allowanceMinutes
            prefs[KEY_DAILY_PASS_LIMIT] = settings.dailyPassLimit
            prefs[KEY_ENTRY_DELAY_SECONDS] = settings.entryDelaySeconds
            prefs[KEY_SENSITIVITY] = settings.sensitivity.name
            prefs[KEY_AUDIO_ACTION] = settings.audioAction.name
            prefs[KEY_CUSTOM_HEADLINE] = settings.customMessageHeadline
            prefs[KEY_CUSTOM_MSG] = settings.customMessage
            prefs[KEY_DEFAULT_CONTENT_POLICY] = settings.defaultContentPolicy.name
            prefs[KEY_DEFAULT_HIDE_SURFACES] = settings.defaultRequestHideSurfaces
            prefs[KEY_DEFAULT_REELS_GOAL] = settings.defaultReelsGoalMinutes
            prefs[KEY_DEFAULT_FEED_GOAL] = settings.defaultFeedGoalMinutes
            prefs[KEY_SOUND] = settings.audioAction != AudioAction.NONE
            prefs[KEY_HAPTICS] = settings.hapticsEnabled
            prefs[KEY_PIN_PROTECTED] = settings.pinProtected
            prefs[KEY_PIN_HASH] = settings.pinHash
            prefs[KEY_PIN_SALT] = settings.pinSalt
            prefs[KEY_STRICT_MODE] = settings.strictMode
            prefs[KEY_THEME_MODE] = settings.themeMode.name
            prefs[KEY_DYNAMIC_COLOR] = settings.dynamicColorEnabled
            prefs[KEY_COLOR_SEED] = settings.colorSeed.name
            prefs[KEY_CONTRAST_LEVEL] = settings.contrastLevel.name
            prefs[KEY_CORNER_STYLE] = settings.cornerStyle.name
            prefs[KEY_FONT_SCALE] = settings.fontScale.name
            prefs[KEY_ICON_STYLE] = settings.iconStyle.name
            prefs[KEY_ONBOARDING_DONE] = settings.onboardingCompleted
        }
        syncToNativeStateStore(settings, targets, schedules)
    }

    suspend fun saveTargets(
        settings: AppSettingsState,
        targets: List<TargetAppConfig>,
        schedules: List<ScheduleItem>
    ) = withContext(Dispatchers.IO) {
        context.uiDataStore.edit { prefs ->
            prefs[KEY_TARGETS_JSON] = encodeTargetsJson(targets)
        }
        syncToNativeStateStore(settings, targets, schedules)
    }

    suspend fun saveSchedules(
        settings: AppSettingsState,
        targets: List<TargetAppConfig>,
        schedules: List<ScheduleItem>
    ) = withContext(Dispatchers.IO) {
        context.uiDataStore.edit { prefs ->
            prefs[KEY_SCHEDULES_JSON] = encodeSchedulesJson(schedules)
        }
        syncToNativeStateStore(settings, targets, schedules)
    }

    suspend fun saveHistory(records: List<HistoryRecord>) = withContext(Dispatchers.IO) {
        saveHistoryInternal(records)
    }

    suspend fun clearHistory(): Unit = withContext(Dispatchers.IO) {
        context.uiDataStore.edit { prefs ->
            prefs[KEY_HISTORY_JSON] = "[]"
        }
        NativeStateStore.resetMetrics()
        PendingBlockEventStore.getInstance(context).clearPending()
    }

    fun exportBackupJson(
        settings: AppSettingsState,
        targets: List<TargetAppConfig>,
        schedules: List<ScheduleItem>
    ): String = BackupCodec.exportBackupJson(settings, targets, schedules)

    fun parseAndValidateBackupJson(
        rawJson: String,
        currentSettings: AppSettingsState
    ): Result<Triple<AppSettingsState, List<TargetAppConfig>, List<ScheduleItem>>> {
        return BackupCodec.parseAndValidateBackupJson(rawJson, currentSettings).map { (s, t, sc) ->
            Triple(s, t.map { it.copy(isInstalled = isPackageInstalled(it.packageName)) }, sc)
        }
    }

    private suspend fun saveHistoryInternal(records: List<HistoryRecord>) {
        val trimmed = records.sortedByDescending { it.timestampMillis }.take(500)
        context.uiDataStore.edit { prefs ->
            prefs[KEY_HISTORY_JSON] = encodeHistoryJson(trimmed)
        }
    }

    private suspend fun syncToNativeStateStore(
        settings: AppSettingsState,
        targets: List<TargetAppConfig>,
        schedules: List<ScheduleItem>
    ) {
        val rawRules = targets.map { target ->
            buildMap<String, Any> {
                put("packageName", target.packageName)
                put("isEnabled", target.isEnabled)
                put("interventionMode", target.mode.name)
                put("scope", target.scope.name)
                put("contentPolicy", target.contentPolicy.name)
                put("requestHideSurfaces", target.requestHideSurfaces)
                put("reelsDailyGoalMinutes", target.reelsDailyGoalMinutes)
                put("feedDailyGoalMinutes", target.feedDailyGoalMinutes)
                target.holdDurationOverride?.let { put("holdDurationSeconds", it) }
                target.allowanceMinutesOverride?.let { put("allowanceMinutes", it) }
                target.dailyPassLimitOverride?.let { put("dailyPassLimit", it) }
            }
        }
        val rawSchedules = schedules.map { sched ->
            val nativeId = if (sched.isQuickBoost && !sched.id.startsWith(ScheduleRule.QUICK_BOOST_ID_PREFIX)) {
                "${ScheduleRule.QUICK_BOOST_ID_PREFIX}${sched.id}"
            } else {
                sched.id
            }
            buildMap<String, Any> {
                put("id", nativeId)
                put("title", sched.title)
                put("isActive", sched.isActive)
                put("daysOfWeek", sched.daysOfWeek.toList())
                put("startHour", sched.startHour)
                put("startMinute", sched.startMinute)
                put("endHour", sched.endHour)
                put("endMinute", sched.endMinute)
                put("enabledPackages", sched.enabledPackages.toList())
                sched.modeOverride?.let { put("modeOverride", it.name) }
                sched.scopeOverride?.let { put("scopeOverride", it.name) }
            }
        }
        val config = InterventionConfig(
            mode = settings.globalMode,
            holdDurationSeconds = settings.holdDurationSeconds,
            customMessage = settings.customMessage,
            soundEnabled = settings.audioAction != AudioAction.NONE,
            hapticsEnabled = settings.hapticsEnabled,
            pinProtected = settings.pinProtected,
            allowanceMinutes = settings.allowanceMinutes,
            dailyPassLimit = settings.dailyPassLimit,
            entryDelaySeconds = settings.entryDelaySeconds,
            sensitivity = settings.sensitivity,
            audioAction = settings.audioAction,
            strictMode = settings.strictMode
        )
        NativeStateStore.applyRestoredProtection(
            context = context,
            rawRules = rawRules,
            rawSchedules = rawSchedules,
            config = config,
            enabled = settings.masterEnabled
        )
    }

    private fun mergeHistory(
        existing: List<HistoryRecord>,
        incoming: List<HistoryRecord>
    ): List<HistoryRecord> {
        if (incoming.isEmpty()) return existing
        val byId = LinkedHashMap<String, HistoryRecord>()
        (incoming + existing).sortedByDescending { it.timestampMillis }.forEach { rec ->
            byId.putIfAbsent(rec.id, rec)
        }
        return byId.values.take(500).toList()
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun parseTheme(raw: String?): AppThemeMode = BackupCodec.parseTheme(raw)
    private fun encodeTargetsJson(targets: List<TargetAppConfig>): String = BackupCodec.encodeTargetsJson(targets)
    private fun parseTargetsJson(json: String): List<TargetAppConfig> = BackupCodec.parseTargetsJson(json)
    private fun encodeSchedulesJson(schedules: List<ScheduleItem>): String = BackupCodec.encodeSchedulesJson(schedules)
    private fun parseSchedulesJson(json: String): List<ScheduleItem> = BackupCodec.parseSchedulesJson(json)

    private fun encodeHistoryJson(records: List<HistoryRecord>): String {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("timestampMillis", r.timestampMillis)
                put("packageName", r.packageName)
                put("appLabel", r.appLabel)
                put("platformKey", r.platformKey)
                put("interventionMode", r.interventionMode.name)
                put("diagnosticReason", r.diagnosticReason)
                put("estimatedMinutesSaved", r.estimatedMinutesSaved)
            })
        }
        return arr.toString()
    }

    private fun parseHistoryJson(json: String): List<HistoryRecord> {
        val arr = JSONArray(json)
        val list = mutableListOf<HistoryRecord>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val pkg = o.optString("packageName")
            list.add(
                HistoryRecord(
                    id = o.optString("id", "hist_$i"),
                    timestampMillis = o.optLong("timestampMillis", 0L),
                    packageName = pkg,
                    appLabel = o.optString("appLabel", SupportedTargets.labelForPackage(pkg)),
                    platformKey = o.optString("platformKey", SupportedTargets.platformKeyForPackage(pkg)),
                    interventionMode = NativeStateStore.parseInterventionMode(o.optString("interventionMode")),
                    diagnosticReason = o.optString("diagnosticReason", "REEL_CONTAINER_MATCH"),
                    estimatedMinutesSaved = o.optDouble("estimatedMinutesSaved", 2.5)
                )
            )
        }
        return list
    }
}

/** Pure, Android-independent JSON serialization codec for configuration backups and rules. */
object BackupCodec {
    fun exportBackupJson(
        settings: AppSettingsState,
        targets: List<TargetAppConfig>,
        schedules: List<ScheduleItem>
    ): String {
        val root = JSONObject().apply {
            put("schemaVersion", 4)
            put("exportedAt", System.currentTimeMillis())
            put("settings", JSONObject().apply {
                put("masterEnabled", settings.masterEnabled)
                put("globalMode", settings.globalMode.name)
                put("holdDurationSeconds", settings.holdDurationSeconds)
                put("allowanceMinutes", settings.allowanceMinutes)
                put("dailyPassLimit", settings.dailyPassLimit)
                put("entryDelaySeconds", settings.entryDelaySeconds)
                put("sensitivity", settings.sensitivity.name)
                put("audioAction", settings.audioAction.name)
                put("customMessageHeadline", settings.customMessageHeadline)
                put("customMessage", settings.customMessage)
                put("defaultContentPolicy", settings.defaultContentPolicy.name)
                put("defaultRequestHideSurfaces", settings.defaultRequestHideSurfaces)
                put("defaultReelsGoalMinutes", settings.defaultReelsGoalMinutes)
                put("defaultFeedGoalMinutes", settings.defaultFeedGoalMinutes)
                put("soundEnabled", settings.audioAction != AudioAction.NONE)
                put("hapticsEnabled", settings.hapticsEnabled)
                put("strictMode", settings.strictMode)
                put("themeMode", settings.themeMode.name)
                put("dynamicColorEnabled", settings.dynamicColorEnabled)
                put("colorSeed", settings.colorSeed.name)
                put("contrastLevel", settings.contrastLevel.name)
                put("cornerStyle", settings.cornerStyle.name)
                put("fontScale", settings.fontScale.name)
                put("iconStyle", settings.iconStyle.name)
            })
            put("targets", JSONArray(encodeTargetsJson(targets)))
            put("schedules", JSONArray(encodeSchedulesJson(schedules.filterNot { it.isQuickBoost })))
        }
        return root.toString(2)
    }

    fun parseAndValidateBackupJson(
        rawJson: String,
        currentSettings: AppSettingsState
    ): Result<Triple<AppSettingsState, List<TargetAppConfig>, List<ScheduleItem>>> {
        return runCatching {
            val root = JSONObject(rawJson.trim())
            val version = root.optInt("schemaVersion", -1)
            require(version in 1..4) { "Unsupported backup schema version." }

            val settingsObj = root.getJSONObject("settings")
            val legacySound = settingsObj.optBoolean("soundEnabled", true)
            val audioAction = settingsObj.optString("audioAction", "").takeIf { it.isNotBlank() }
                ?.let(NativeStateStore::parseAudioAction)
                ?: if (legacySound) AudioAction.SILENCE else AudioAction.NONE

            val restoredSettings = currentSettings.copy(
                masterEnabled = settingsObj.optBoolean("masterEnabled", true),
                globalMode = NativeStateStore.parseInterventionMode(settingsObj.optString("globalMode")),
                holdDurationSeconds = settingsObj.optInt("holdDurationSeconds", 5).coerceIn(3, 30),
                allowanceMinutes = settingsObj.optInt("allowanceMinutes", 5).coerceIn(1, 30),
                dailyPassLimit = settingsObj.optInt("dailyPassLimit", 0).coerceIn(0, 10),
                entryDelaySeconds = settingsObj.optInt("entryDelaySeconds", 0).coerceIn(0, 10),
                sensitivity = NativeStateStore.parseSensitivity(settingsObj.optString("sensitivity")),
                audioAction = audioAction,
                customMessageHeadline = settingsObj.optString(
                    "customMessageHeadline",
                    "Pause before you scroll."
                ).take(80),
                customMessage = settingsObj.optString(
                    "customMessage",
                    "Take a deep breath. Do you really want to spend your focus here right now?"
                ).take(240),
                defaultContentPolicy = NativeStateStore.parseContentBlockPolicy(
                    settingsObj.optString("defaultContentPolicy")
                ),
                defaultRequestHideSurfaces = settingsObj.optBoolean("defaultRequestHideSurfaces", false),
                defaultReelsGoalMinutes = settingsObj.optInt("defaultReelsGoalMinutes", 15).coerceIn(0, 180),
                defaultFeedGoalMinutes = settingsObj.optInt("defaultFeedGoalMinutes", 20).coerceIn(0, 180),
                soundEnabled = audioAction != AudioAction.NONE,
                hapticsEnabled = settingsObj.optBoolean("hapticsEnabled", true),
                strictMode = settingsObj.optBoolean("strictMode", false),
                themeMode = parseTheme(settingsObj.optString("themeMode")),
                dynamicColorEnabled = settingsObj.optBoolean("dynamicColorEnabled", false),
                colorSeed = parseColorSeed(settingsObj.optString("colorSeed")),
                contrastLevel = parseContrastLevel(settingsObj.optString("contrastLevel")),
                cornerStyle = parseCornerStyle(settingsObj.optString("cornerStyle")),
                fontScale = parseFontScale(settingsObj.optString("fontScale")),
                iconStyle = parseIconStyle(settingsObj.optString("iconStyle"))
            )

            val restoredTargets = parseTargetsJson(root.getJSONArray("targets").toString())
            require(restoredTargets.isNotEmpty()) { "Backup contains no target rules." }

            val restoredSchedules = parseSchedulesJson(root.getJSONArray("schedules").toString())
            Triple(restoredSettings, restoredTargets, restoredSchedules)
        }
    }

    fun parseTheme(raw: String?): AppThemeMode = when (raw?.uppercase()) {
        "LIGHT" -> AppThemeMode.LIGHT
        "DARK" -> AppThemeMode.DARK
        "AMOLED" -> AppThemeMode.AMOLED
        else -> AppThemeMode.SYSTEM
    }

    fun parseColorSeed(raw: String?): AppColorSeed =
        AppColorSeed.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AppColorSeed.EMERALD

    fun parseContrastLevel(raw: String?): AppContrastLevel =
        AppContrastLevel.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AppContrastLevel.STANDARD

    fun parseCornerStyle(raw: String?): AppCornerStyle =
        AppCornerStyle.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AppCornerStyle.EXPRESSIVE

    fun parseFontScale(raw: String?): AppFontScale =
        AppFontScale.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AppFontScale.DEFAULT

    fun parseIconStyle(raw: String?): AppIconStyle =
        AppIconStyle.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: AppIconStyle.TONAL_COOKIE

    fun encodeTargetsJson(targets: List<TargetAppConfig>): String {
        val arr = JSONArray()
        targets.forEach { t ->
            arr.put(JSONObject().apply {
                put("packageName", t.packageName)
                put("displayName", t.displayName)
                put("feedName", t.feedName)
                put("platformKey", t.platformKey)
                put("isEnabled", t.isEnabled)
                put("mode", t.mode.name)
                put("scope", t.scope.name)
                put("contentPolicy", t.contentPolicy.name)
                put("requestHideSurfaces", t.requestHideSurfaces)
                put("reelsDailyGoalMinutes", t.reelsDailyGoalMinutes)
                put("feedDailyGoalMinutes", t.feedDailyGoalMinutes)
                if (t.holdDurationOverride != null) put("holdDurationOverride", t.holdDurationOverride)
                if (t.allowanceMinutesOverride != null) put("allowanceMinutesOverride", t.allowanceMinutesOverride)
                if (t.dailyPassLimitOverride != null) put("dailyPassLimitOverride", t.dailyPassLimitOverride)
            })
        }
        return arr.toString()
    }

    fun parseTargetsJson(json: String): List<TargetAppConfig> {
        val arr = JSONArray(json)
        val parsed = mutableMapOf<String, TargetAppConfig>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val pkg = o.optString("packageName")
            if (pkg.isBlank()) continue
            val contentPolicy = NativeStateStore.parseContentBlockPolicy(o.optString("contentPolicy"))
            parsed[pkg] = TargetAppConfig(
                packageName = pkg,
                displayName = o.optString("displayName", SupportedTargets.labelForPackage(pkg)),
                feedName = o.optString("feedName", "Short Video"),
                platformKey = o.optString("platformKey", SupportedTargets.platformKeyForPackage(pkg)),
                isEnabled = o.optBoolean("isEnabled", false),
                mode = NativeStateStore.parseInterventionMode(o.optString("mode")),
                scope = NativeStateStore.parseBlockingScope(o.optString("scope")),
                contentPolicy = contentPolicy,
                requestHideSurfaces = o.optBoolean("requestHideSurfaces", contentPolicy == com.blockreels.app.state.ContentBlockPolicy.HIDE_SURFACES),
                reelsDailyGoalMinutes = o.optInt("reelsDailyGoalMinutes", 15).coerceIn(0, 180),
                feedDailyGoalMinutes = o.optInt("feedDailyGoalMinutes", 20).coerceIn(0, 180),
                holdDurationOverride = if (o.has("holdDurationOverride") && !o.isNull("holdDurationOverride")) {
                    o.optInt("holdDurationOverride").takeIf { it in 3..30 }
                } else null,
                allowanceMinutesOverride = if (o.has("allowanceMinutesOverride") && !o.isNull("allowanceMinutesOverride")) {
                    o.optInt("allowanceMinutesOverride").takeIf { it in 1..30 }
                } else null,
                dailyPassLimitOverride = if (o.has("dailyPassLimitOverride") && !o.isNull("dailyPassLimitOverride")) {
                    o.optInt("dailyPassLimitOverride").takeIf { it in 0..10 }
                } else null
            )
        }
        return SupportedTargets.defaultTargets.map { def ->
            parsed[def.packageName] ?: def
        }
    }

    fun encodeSchedulesJson(schedules: List<ScheduleItem>): String {
        val arr = JSONArray()
        schedules.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("isActive", s.isActive)
                put("daysOfWeek", JSONArray(s.daysOfWeek.sorted()))
                put("startHour", s.startHour)
                put("startMinute", s.startMinute)
                put("endHour", s.endHour)
                put("endMinute", s.endMinute)
                put("enabledPackages", JSONArray(s.enabledPackages.toList()))
                if (s.modeOverride != null) put("modeOverride", s.modeOverride.name)
                if (s.scopeOverride != null) put("scopeOverride", s.scopeOverride.name)
                put("isQuickBoost", s.isQuickBoost)
                if (s.expiresAtMillis != null) put("expiresAtMillis", s.expiresAtMillis)
            })
        }
        return arr.toString()
    }

    fun parseSchedulesJson(json: String): List<ScheduleItem> {
        val arr = JSONArray(json)
        val list = mutableListOf<ScheduleItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val daysArr = o.optJSONArray("daysOfWeek") ?: JSONArray()
            val days = buildSet {
                for (d in 0 until daysArr.length()) add(daysArr.getInt(d))
            }
            val pkgsArr = o.optJSONArray("enabledPackages") ?: JSONArray()
            val pkgs = buildSet {
                for (p in 0 until pkgsArr.length()) add(pkgsArr.getString(p))
            }
            val modeOverride = o.optString("modeOverride", "").takeIf { it.isNotBlank() }
                ?.let(NativeStateStore::parseInterventionMode)
            val scopeOverride = o.optString("scopeOverride", "").takeIf { it.isNotBlank() }
                ?.let(NativeStateStore::parseBlockingScope)
            list.add(
                ScheduleItem(
                    id = o.optString("id", "sched_$i"),
                    title = o.optString("title", "Focus Schedule"),
                    isActive = o.optBoolean("isActive", true),
                    daysOfWeek = days,
                    startHour = o.optInt("startHour", 9),
                    startMinute = o.optInt("startMinute", 0),
                    endHour = o.optInt("endHour", 17),
                    endMinute = o.optInt("endMinute", 0),
                    enabledPackages = pkgs,
                    modeOverride = modeOverride,
                    scopeOverride = scopeOverride,
                    isQuickBoost = o.optBoolean("isQuickBoost", false),
                    expiresAtMillis = if (o.has("expiresAtMillis") && !o.isNull("expiresAtMillis")) {
                        o.optLong("expiresAtMillis")
                    } else null
                )
            )
        }
        return list
    }
}
