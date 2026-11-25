package com.blockreels.app.detection

enum class AppPlatform(val packageName: String) {
    INSTAGRAM("com.instagram.android"),
    YOUTUBE("com.google.android.youtube"),
    FACEBOOK("com.facebook.katana"),
    TIKTOK("com.zhiliaoapp.musically"),
    SNAPCHAT("com.snapchat.android");

    companion object {
        internal const val FACEBOOK_LITE_PACKAGE = "com.facebook.lite"
        private val PACKAGE_MAP = entries.associateBy { it.packageName }

        fun fromPackageName(pkg: String?): AppPlatform? {
            if (pkg == null) return null
            return PACKAGE_MAP[pkg] ?: when (pkg) {
                FACEBOOK_LITE_PACKAGE -> FACEBOOK
                "com.ss.android.ugc.trill",
                "com.ss.android.ugc.aweme",
                "com.ss.android.ugc.aweme.lite" -> TIKTOK
                else -> null
            }
        }
    }
}
