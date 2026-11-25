// SPDX-License-Identifier: MIT
package com.blockreels.app.data

import androidx.compose.runtime.Immutable
import com.blockreels.app.state.AudioAction
import com.blockreels.app.state.BlockingScope
import com.blockreels.app.state.DetectionSensitivity
import com.blockreels.app.state.InterventionMode

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED")
}

enum class ProtectionStatus {
    PROTECTED,
    PAUSED,
    PERMISSION_NEEDED
}

enum class StatsRange(val label: String, val days: Int?) {
    WEEK("7D", 7),
    MONTH("30D", 30),
    ALL("All", null)
}

@Immutable
data class TargetAppConfig(
    val packageName: String,
    val displayName: String,
    val feedName: String,
    val platformKey: String,
    val isEnabled: Boolean,
    val mode: InterventionMode,
    val scope: BlockingScope = BlockingScope.FEED_ONLY,
    val holdDurationOverride: Int? = null,
    val allowanceMinutesOverride: Int? = null,
    val dailyPassLimitOverride: Int? = null,
    val isInstalled: Boolean = true
) {
    val hasCustomOverrides: Boolean
        get() = scope != BlockingScope.FEED_ONLY ||
            holdDurationOverride != null ||
            allowanceMinutesOverride != null ||
            dailyPassLimitOverride != null
}

@Immutable
data class ScheduleItem(
    val id: String,
    val title: String,
    val isActive: Boolean,
    val daysOfWeek: Set<Int>, // ISO 1=Mon .. 7=Sun
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val enabledPackages: Set<String>,
    val modeOverride: InterventionMode? = null,
    val scopeOverride: BlockingScope? = null,
    val isQuickBoost: Boolean = false,
    val expiresAtMillis: Long? = null
) {
    fun formattedTimeRange(): String {
        val start = String.format("%02d:%02d", startHour, startMinute)
        val end = String.format("%02d:%02d", endHour, endMinute)
        return "$start – $end"
    }
}

@Immutable
data class HistoryRecord(
    val id: String,
    val timestampMillis: Long,
    val packageName: String,
    val appLabel: String,
    val platformKey: String,
    val interventionMode: InterventionMode,
    val diagnosticReason: String,
    val estimatedMinutesSaved: Double = 2.5
)

@Immutable
data class ActiveAllowanceItem(
    val packageName: String,
    val appLabel: String,
    val platformKey: String,
    val remainingMillis: Long
)

@Immutable
data class AppSettingsState(
    val masterEnabled: Boolean = true,
    val globalMode: InterventionMode = InterventionMode.MINDFUL_PAUSE,
    val holdDurationSeconds: Int = 5,
    val allowanceMinutes: Int = 5,
    val dailyPassLimit: Int = 0, // 0 = Unlimited
    val entryDelaySeconds: Int = 0, // 0 = Instant
    val sensitivity: DetectionSensitivity = DetectionSensitivity.BALANCED,
    val audioAction: AudioAction = AudioAction.SILENCE,
    val customMessage: String = "Take a deep breath. Do you really want to scroll right now?",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val pinProtected: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val strictMode: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val onboardingCompleted: Boolean = false
)

@Immutable
data class DayCountPoint(
    val dayLabel: String,
    val dateKey: Int,
    val count: Int,
    val isToday: Boolean
)

@Immutable
data class PlatformBreakdownItem(
    val platformKey: String,
    val label: String,
    val count: Int,
    val fraction: Float
)

@Immutable
data class AnalyticsSummary(
    val totalBlocks: Int = 0,
    val blocksToday: Int = 0,
    val minutesSaved: Int = 0,
    val minutesSavedToday: Int = 0,
    val streakDays: Int = 0,
    val peakHourLabel: String = "—",
    val averageDailyBlocks: Double = 0.0,
    val dailySeries: List<DayCountPoint> = emptyList(),
    val platformBreakdown: List<PlatformBreakdownItem> = emptyList()
)

@Immutable
data class SystemHealthState(
    val accessibilityEnabled: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val canDrawOverlays: Boolean = false,
    val monitoringServiceRunning: Boolean = false
)

object SupportedTargets {
    val defaultTargets: List<TargetAppConfig> = listOf(
        TargetAppConfig(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            feedName = "Reels",
            platformKey = "INSTAGRAM",
            isEnabled = true,
            mode = InterventionMode.MINDFUL_PAUSE
        ),
        TargetAppConfig(
            packageName = "com.google.android.youtube",
            displayName = "YouTube",
            feedName = "Shorts",
            platformKey = "YOUTUBE",
            isEnabled = true,
            mode = InterventionMode.MINDFUL_PAUSE
        ),
        TargetAppConfig(
            packageName = "com.zhiliaoapp.musically",
            displayName = "TikTok",
            feedName = "For You",
            platformKey = "TIKTOK",
            isEnabled = true,
            mode = InterventionMode.MINDFUL_PAUSE
        ),
        TargetAppConfig(
            packageName = "com.facebook.katana",
            displayName = "Facebook",
            feedName = "Reels",
            platformKey = "FACEBOOK",
            isEnabled = false,
            mode = InterventionMode.MINDFUL_PAUSE
        ),
        TargetAppConfig(
            packageName = "com.snapchat.android",
            displayName = "Snapchat",
            feedName = "Spotlight",
            platformKey = "SNAPCHAT",
            isEnabled = false,
            mode = InterventionMode.MINDFUL_PAUSE
        ),
        TargetAppConfig(
            packageName = "com.facebook.lite",
            displayName = "Facebook Lite",
            feedName = "Reels",
            platformKey = "FACEBOOK",
            isEnabled = false,
            mode = InterventionMode.MINDFUL_PAUSE
        ),
        TargetAppConfig(
            packageName = "com.ss.android.ugc.trill",
            displayName = "TikTok (Global)",
            feedName = "For You",
            platformKey = "TIKTOK",
            isEnabled = false,
            mode = InterventionMode.MINDFUL_PAUSE
        ),
        TargetAppConfig(
            packageName = "com.ss.android.ugc.aweme",
            displayName = "Douyin",
            feedName = "Short Video",
            platformKey = "TIKTOK",
            isEnabled = false,
            mode = InterventionMode.MINDFUL_PAUSE
        )
    )

    fun labelForPackage(packageName: String): String {
        return defaultTargets.firstOrNull { it.packageName == packageName }?.let {
            "${it.displayName} ${it.feedName}"
        } ?: when {
            packageName.contains("instagram") -> "Instagram Reels"
            packageName.contains("youtube") -> "YouTube Shorts"
            packageName.contains("facebook") -> "Facebook Reels"
            packageName.contains("snapchat") -> "Snapchat Spotlight"
            packageName.contains("musically") || packageName.contains("ugc") -> "TikTok"
            else -> packageName
        }
    }

    fun platformKeyForPackage(packageName: String): String {
        return when {
            packageName.contains("instagram") -> "INSTAGRAM"
            packageName.contains("youtube") -> "YOUTUBE"
            packageName.contains("facebook") -> "FACEBOOK"
            packageName.contains("snapchat") -> "SNAPCHAT"
            packageName.contains("musically") || packageName.contains("ugc") -> "TIKTOK"
            else -> "OTHER"
        }
    }

    fun platformDisplayName(platformKey: String): String = when (platformKey) {
        "INSTAGRAM" -> "Instagram Reels"
        "YOUTUBE" -> "YouTube Shorts"
        "TIKTOK" -> "TikTok"
        "FACEBOOK" -> "Facebook Reels"
        "SNAPCHAT" -> "Snapchat Spotlight"
        else -> "Short Video"
    }

    fun primaryPackageForPlatform(platformKey: String): String? = when (platformKey.uppercase()) {
        "INSTAGRAM" -> "com.instagram.android"
        "YOUTUBE" -> "com.google.android.youtube"
        "TIKTOK" -> "com.zhiliaoapp.musically"
        "FACEBOOK" -> "com.facebook.katana"
        "SNAPCHAT" -> "com.snapchat.android"
        else -> null
    }
}
