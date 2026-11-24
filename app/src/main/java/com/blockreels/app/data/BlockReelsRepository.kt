package com.blockreels.app.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    private val KEY_CUSTOM_MSG = stringPreferencesKey("ui_custom_message")
    private val KEY_SOUND = booleanPreferencesKey("ui_sound_enabled")
    private val KEY_HAPTICS = booleanPreferencesKey("ui_haptics_enabled")
    private val KEY_PIN_PROTECTED = booleanPreferencesKey("ui_pin_protected")
    private val KEY_PIN_HASH = stringPreferencesKey("ui_pin_hash")
    private val KEY_PIN_SALT = stringPreferencesKey("ui_pin_salt")
    private val KEY_STRICT_MODE = booleanPreferencesKey("ui_strict_mode")
    private val KEY_THEME_MODE = stringPreferencesKey("ui_theme_mode")
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

        val settings = AppSettingsState(
            masterEnabled = prefs[KEY_MASTER] ?: NativeStateStore.isMasterEnabled(),
            globalMode = parseMode(prefs[KEY_GLOBAL_MODE]),
            holdDurationSeconds = (prefs[KEY_HOLD_SECONDS] ?: 5).coerceIn(3, 15),
            customMessage = prefs[KEY_CUSTOM_MSG]
                ?: "Take a deep breath. Do you really want to scroll right now?",
            soundEnabled = prefs[KEY_SOUND] ?: true,
            hapticsEnabled = prefs[KEY_HAPTICS] ?: true,
            pinProtected = prefs[KEY_PIN_PROTECTED] ?: false,
            pinHash = prefs[KEY_PIN_HASH] ?: "",
            pinSalt = prefs[KEY_PIN_SALT] ?: "",
            strictMode = prefs[KEY_STRICT_MODE] ?: false,
            themeMode = parseTheme(prefs[KEY_THEME_MODE]),
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

        // Sync native engine state so AccessibilityService always reflects current config.
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
                interventionMode = parseMode(ev["interventionMode"] as? String),
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
            prefs[KEY_CUSTOM_MSG] = settings.customMessage
            prefs[KEY_SOUND] = settings.soundEnabled
            prefs[KEY_HAPTICS] = settings.hapticsEnabled
            prefs[KEY_PIN_PROTECTED] = settings.pinProtected
            prefs[KEY_PIN_HASH] = settings.pinHash
            prefs[KEY_PIN_SALT] = settings.pinSalt
            prefs[KEY_STRICT_MODE] = settings.strictMode
            prefs[KEY_THEME_MODE] = settings.themeMode.name
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
    ): String {
        val root = JSONObject().apply {
            put("schemaVersion", 2)
            put("exportedAt", System.currentTimeMillis())
            put("settings", JSONObject().apply {
                put("masterEnabled", settings.masterEnabled)
                put("globalMode", settings.globalMode.name)
                put("holdDurationSeconds", settings.holdDurationSeconds)
                put("customMessage", settings.customMessage)
                put("soundEnabled", settings.soundEnabled)
                put("hapticsEnabled", settings.hapticsEnabled)
                put("strictMode", settings.strictMode)
                put("themeMode", settings.themeMode.name)
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
            require(version in 1..2) { "Unsupported backup schema version." }

            val settingsObj = root.getJSONObject("settings")
            val restoredSettings = currentSettings.copy(
                masterEnabled = settingsObj.optBoolean("masterEnabled", true),
                globalMode = parseMode(settingsObj.optString("globalMode")),
                holdDurationSeconds = settingsObj.optInt("holdDurationSeconds", 5).coerceIn(3, 15),
                customMessage = settingsObj.optString(
                    "customMessage",
                    "Take a deep breath. Do you really want to scroll right now?"
                ).take(240),
                soundEnabled = settingsObj.optBoolean("soundEnabled", true),
                hapticsEnabled = settingsObj.optBoolean("hapticsEnabled", true),
                strictMode = settingsObj.optBoolean("strictMode", false),
                themeMode = parseTheme(settingsObj.optString("themeMode"))
            )

            val restoredTargets = parseTargetsJson(root.getJSONArray("targets").toString()).map {
                it.copy(isInstalled = isPackageInstalled(it.packageName))
            }
            require(restoredTargets.isNotEmpty()) { "Backup contains no target rules." }

            val restoredSchedules = parseSchedulesJson(root.getJSONArray("schedules").toString())
            Triple(restoredSettings, restoredTargets, restoredSchedules)
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
        val effectiveMode = if (settings.strictMode) InterventionMode.STRICT_BLOCK else settings.globalMode
        val rawRules = targets.map { target ->
            val mode = if (settings.strictMode) InterventionMode.STRICT_BLOCK else target.mode
            mapOf<String, Any>(
                "packageName" to target.packageName,
                "isEnabled" to target.isEnabled,
                "interventionMode" to mode.name,
                "holdDurationSeconds" to settings.holdDurationSeconds
            )
        }
        val rawSchedules = schedules.map { sched ->
            val nativeId = if (sched.isQuickBoost && !sched.id.startsWith(ScheduleRule.QUICK_BOOST_ID_PREFIX)) {
                "${ScheduleRule.QUICK_BOOST_ID_PREFIX}${sched.id}"
            } else {
                sched.id
            }
            mapOf<String, Any>(
                "id" to nativeId,
                "title" to sched.title,
                "isActive" to sched.isActive,
                "daysOfWeek" to sched.daysOfWeek.toList(),
                "startHour" to sched.startHour,
                "startMinute" to sched.startMinute,
                "endHour" to sched.endHour,
                "endMinute" to sched.endMinute,
                "enabledPackages" to sched.enabledPackages.toList()
            )
        }
        val config = InterventionConfig(
            mode = effectiveMode,
            holdDurationSeconds = settings.holdDurationSeconds,
            customMessage = settings.customMessage,
            soundEnabled = settings.soundEnabled,
            hapticsEnabled = settings.hapticsEnabled,
            pinProtected = settings.pinProtected
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

    private fun parseMode(raw: String?): InterventionMode = when (raw?.uppercase()) {
        "AUTO_BACK" -> InterventionMode.AUTO_BACK
        "STRICT_BLOCK" -> InterventionMode.STRICT_BLOCK
        else -> InterventionMode.MINDFUL_PAUSE
    }

    private fun parseTheme(raw: String?): AppThemeMode = when (raw?.uppercase()) {
        "LIGHT" -> AppThemeMode.LIGHT
        "DARK" -> AppThemeMode.DARK
        "AMOLED" -> AppThemeMode.AMOLED
        else -> AppThemeMode.SYSTEM
    }

    private fun encodeTargetsJson(targets: List<TargetAppConfig>): String {
        val arr = JSONArray()
        targets.forEach { t ->
            arr.put(JSONObject().apply {
                put("packageName", t.packageName)
                put("displayName", t.displayName)
                put("feedName", t.feedName)
                put("platformKey", t.platformKey)
                put("isEnabled", t.isEnabled)
                put("mode", t.mode.name)
            })
        }
        return arr.toString()
    }

    private fun parseTargetsJson(json: String): List<TargetAppConfig> {
        val arr = JSONArray(json)
        val parsed = mutableMapOf<String, TargetAppConfig>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val pkg = o.optString("packageName")
            if (pkg.isBlank()) continue
            parsed[pkg] = TargetAppConfig(
                packageName = pkg,
                displayName = o.optString("displayName", SupportedTargets.labelForPackage(pkg)),
                feedName = o.optString("feedName", "Short Video"),
                platformKey = o.optString("platformKey", SupportedTargets.platformKeyForPackage(pkg)),
                isEnabled = o.optBoolean("isEnabled", false),
                mode = parseMode(o.optString("mode"))
            )
        }
        return SupportedTargets.defaultTargets.map { def ->
            parsed[def.packageName] ?: def
        }
    }

    private fun encodeSchedulesJson(schedules: List<ScheduleItem>): String {
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
                put("isQuickBoost", s.isQuickBoost)
                if (s.expiresAtMillis != null) put("expiresAtMillis", s.expiresAtMillis)
            })
        }
        return arr.toString()
    }

    private fun parseSchedulesJson(json: String): List<ScheduleItem> {
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
                    isQuickBoost = o.optBoolean("isQuickBoost", false),
                    expiresAtMillis = if (o.has("expiresAtMillis")) o.optLong("expiresAtMillis") else null
                )
            )
        }
        return list
    }

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
                    interventionMode = parseMode(o.optString("interventionMode")),
                    diagnosticReason = o.optString("diagnosticReason", "REEL_CONTAINER_MATCH"),
                    estimatedMinutesSaved = o.optDouble("estimatedMinutesSaved", 2.5)
                )
            )
        }
        return list
    }
}
