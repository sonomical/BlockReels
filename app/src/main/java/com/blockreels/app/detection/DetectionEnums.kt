package com.blockreels.app.detection

enum class DetectionLayer {
    PRIMARY_RESOURCE_ID,
    FALLBACK_REGEX_ID,
    CLASS_HIERARCHY,
    CONTENT_DESCRIPTION,
    MULTILINGUAL_TEXT,
    ACTIVITY_OR_WINDOW_NAME
}

enum class DetectionConfidence(val score: Double) {
    DEFINITIVE(1.0),   // Primary resource ID or verified container match
    HIGH(0.85),        // Fallback regex ID or strong multilingual content description
    MEDIUM(0.65),      // Class structure or regex text match
    LOW(0.30)          // Loose token without structural corroboration
}

enum class SuppressionReason {
    CHAT_COMPOSER_ACTIVE,
    CHAT_THREAD_CONTAINER,
    PROFILE_HEADER_DETECTED,
    SEARCH_EXPLORE_GRID_ACTIVE,
    STANDARD_FEED_SCRUBBER,
    CREATOR_CAMERA_ACTIVE,
    STORY_VIEWER_ACTIVE,
    SURGICAL_MODE_INBOX
}
