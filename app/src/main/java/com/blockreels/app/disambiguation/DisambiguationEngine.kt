package com.blockreels.app.disambiguation

import com.blockreels.app.detection.AppPlatform
import com.blockreels.app.detection.SuppressionReason
import com.blockreels.app.model.AccessibilityNode
import java.util.regex.Pattern

object DisambiguationEngine {

    private val TIME_SCRUBBER_REGEX = Pattern.compile("\\b\\d{1,2}:\\d{2}\\s*/\\s*\\d{1,2}:\\d{2}\\b")

    private val CHAT_COMPOSER_IDS = setOf(
        "com.instagram.android:id/row_thread_composer",
        "com.instagram.android:id/row_thread_composer_edittext",
        "com.instagram.android:id/layout_comment_thread_edittext",
        "com.instagram.android:id/direct_text_message_composer",
        "com.instagram.android:id/comment_composer_edittext",
        "com.google.android.youtube:id/comment_composer",
        "com.google.android.youtube:id/comment_edit_text",
        "com.google.android.youtube:id/create_comment_view",
        "com.facebook.katana:id/message_composer",
        "com.facebook.katana:id/chat_input_container",
        "com.facebook.katana:id/composer_text_view",
        "com.zhiliaoapp.musically:id/im_chat_edit_text",
        "com.zhiliaoapp.musically:id/chat_input_layout",
        "com.zhiliaoapp.musically:id/text_chat_input_box",
        "com.zhiliaoapp.musically:id/comment_edit_text",
        "com.zhiliaoapp.musically:id/et_message"
    )

    private val CHAT_CONTAINER_IDS = setOf(
        "com.instagram.android:id/direct_message_list",
        "com.instagram.android:id/thread_recycler_view",
        "com.instagram.android:id/message_list_recycler_view",
        "com.instagram.android:id/direct_thread_layout",
        "com.instagram.android:id/direct_inbox_recycler_view",
        "com.instagram.android:id/inbox_recycler_view",
        "com.zhiliaoapp.musically:id/chat_list",
        "com.zhiliaoapp.musically:id/im_chat_recycler",
        "com.zhiliaoapp.musically:id/inbox_page",
        "com.zhiliaoapp.musically:id/conversation_list",
        "com.facebook.katana:id/messages_list_view",
        "com.facebook.katana:id/thread_view_container",
        "com.facebook.katana:id/thread_list_container"
    )

    private val TIKTOK_CHAT_SUFFIXES = setOf(
        ":id/chat_list",
        ":id/im_chat_recycler",
        ":id/inbox_page",
        ":id/conversation_list",
        ":id/im_chat_edit_text",
        ":id/chat_input_layout",
        ":id/text_chat_input_box",
        ":id/comment_edit_text",
        ":id/et_message"
    )

    private val PROFILE_HEADER_IDS = setOf(
        "com.instagram.android:id/unified_profile_layout",
        "com.instagram.android:id/profile_header_container",
        "com.instagram.android:id/user_bio_text",
        "com.instagram.android:id/profile_header_bio_text",
        "com.instagram.android:id/profile_score_view",
        "com.instagram.android:id/edit_profile_btn",
        "com.google.android.youtube:id/channel_header",
        "com.google.android.youtube:id/channel_banner",
        "com.google.android.youtube:id/channel_title",
        "com.google.android.youtube:id/subscriber_count",
        "com.google.android.youtube:id/tabs_container",
        "com.facebook.katana:id/profile_header_container",
        "com.facebook.katana:id/cover_photo",
        "com.facebook.katana:id/timeline_profile_name",
        "com.zhiliaoapp.musically:id/profile_header",
        "com.zhiliaoapp.musically:id/user_avatar",
        "com.zhiliaoapp.musically:id/following_count",
        "com.zhiliaoapp.musically:id/follower_count",
        "com.zhiliaoapp.musically:id/bio_text"
    )

    private val YOUTUBE_STANDARD_PLAYER_IDS = setOf(
        "com.google.android.youtube:id/watch_while_layout",
        "com.google.android.youtube:id/player_view",
        "com.google.android.youtube:id/time_bar_current_time",
        "com.google.android.youtube:id/time_bar_total_time",
        "com.google.android.youtube:id/inline_time_bar",
        "com.google.android.youtube:id/fullscreen_button",
        "com.google.android.youtube:id/movie_player"
    )

    private val SEARCH_INPUT_IDS = setOf(
        "com.instagram.android:id/action_bar_search_edit_text",
        "com.google.android.youtube:id/search_edit_text",
        "com.zhiliaoapp.musically:id/search_input",
        "com.facebook.katana:id/search_edit_text"
    )

    private val TIKTOK_PROFILE_SUFFIXES = setOf(
        ":id/profile_header",
        ":id/following_count",
        ":id/follower_count",
        ":id/bio_text"
    )

    fun checkSuppression(
        node: AccessibilityNode,
        platform: AppPlatform,
        screenWidth: Int = 1080,
        screenHeight: Int = 2400
    ): SuppressionReason? {
        val viewId = node.viewIdResourceName?.let {
            if (platform == AppPlatform.FACEBOOK && it.startsWith("com.facebook.lite:id/"))
                it.replaceFirst("com.facebook.lite:id/", "com.facebook.katana:id/")
            else it
        }

        if (node.isInputField) {
            return SuppressionReason.CHAT_COMPOSER_ACTIVE
        }
        if (viewId != null && CHAT_COMPOSER_IDS.contains(viewId)) {
            return SuppressionReason.CHAT_COMPOSER_ACTIVE
        }
        if (viewId != null && CHAT_CONTAINER_IDS.contains(viewId)) {
            return SuppressionReason.CHAT_THREAD_CONTAINER
        }
        if (viewId != null && TIKTOK_CHAT_SUFFIXES.any { viewId.endsWith(it) }) {
            return SuppressionReason.CHAT_THREAD_CONTAINER
        }

        val text = node.displayText?.toString()
        val contentDesc = node.displayDescription?.toString()

        if (viewId != null && PROFILE_HEADER_IDS.contains(viewId)) {
            return SuppressionReason.PROFILE_HEADER_DETECTED
        }
        if (viewId != null && TIKTOK_PROFILE_SUFFIXES.any { viewId.endsWith(it) }) {
            return SuppressionReason.PROFILE_HEADER_DETECTED
        }

        if (viewId != null && SEARCH_INPUT_IDS.contains(viewId)) {
            return SuppressionReason.SEARCH_EXPLORE_GRID_ACTIVE
        }
        if (viewId != null && viewId.endsWith(":id/search_input")) {
            return SuppressionReason.SEARCH_EXPLORE_GRID_ACTIVE
        }

        when (platform) {
            AppPlatform.YOUTUBE -> {
                if (viewId != null && YOUTUBE_STANDARD_PLAYER_IDS.contains(viewId)) {
                    return SuppressionReason.STANDARD_FEED_SCRUBBER
                }
                if (text != null && TIME_SCRUBBER_REGEX.matcher(text).find()) {
                    return SuppressionReason.STANDARD_FEED_SCRUBBER
                }
                if (contentDesc != null && TIME_SCRUBBER_REGEX.matcher(contentDesc).find()) {
                    return SuppressionReason.STANDARD_FEED_SCRUBBER
                }
            }
            AppPlatform.INSTAGRAM -> {
                if (viewId == "com.instagram.android:id/explore_grid_recycler") {
                    return SuppressionReason.SEARCH_EXPLORE_GRID_ACTIVE
                }
                if (viewId == "com.instagram.android:id/feed_recycler_view") {
                    val bounds = node.getBoundsInScreenArray()
                    if (bounds[3] - bounds[1] < screenHeight * 0.75) {
                        return SuppressionReason.STANDARD_FEED_SCRUBBER
                    }
                }
            }
            AppPlatform.TIKTOK -> {
                if (viewId == "com.zhiliaoapp.musically:id/record_layout" ||
                    viewId == "com.zhiliaoapp.musically:id/camera_surface" ||
                    viewId?.endsWith(":id/camera_surface") == true ||
                    viewId?.endsWith(":id/record_layout") == true
                ) {
                    return SuppressionReason.CREATOR_CAMERA_ACTIVE
                }
            }
            AppPlatform.FACEBOOK -> {
                if (viewId == "com.facebook.katana:id/newsfeed_recycler_view" ||
                    viewId == "com.facebook.katana:id/news_feed_stream"
                ) {
                    return SuppressionReason.STANDARD_FEED_SCRUBBER
                }
            }
        }

        return null
    }
}
