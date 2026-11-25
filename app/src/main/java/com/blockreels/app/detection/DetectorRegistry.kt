package com.blockreels.app.detection

class DetectorRegistry(
    val instagramDetector: InstagramDetector = InstagramDetector(),
    val youtubeDetector: YouTubeDetector = YouTubeDetector(),
    val facebookDetector: FacebookDetector = FacebookDetector(),
    val tiktokDetector: TikTokDetector = TikTokDetector(),
    val snapchatDetector: SnapchatDetector = SnapchatDetector()
) {
    private val detectorsByPackage = mutableMapOf<String, AppDetector>()

    init {
        register(instagramDetector)
        register(youtubeDetector)
        register(facebookDetector)
        register(tiktokDetector)
        register(snapchatDetector)
    }

    fun register(detector: AppDetector) {
        for (pkg in detector.targetPackages) {
            detectorsByPackage[pkg] = detector
        }
    }

    fun getDetectorForPackage(packageName: String): AppDetector? {
        return detectorsByPackage[packageName]
    }
}
