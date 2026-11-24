package com.blockreels.app.intervention

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.blockreels.app.MonitoringForegroundService
import com.blockreels.app.engine.DetectionTrace

internal enum class FocusChange { GAIN, LOSS_TRANSIENT, LOSS_CAN_DUCK, LOSS_PERMANENT }

internal interface AudioFocusClient {
    fun requestFocus(interventionId: Long, onChange: (FocusChange) -> Unit): Boolean
    fun abandonFocus()
}

/** Cooperative audio focus reduces playback where supported. A grant cannot verify silence. */
class AudioDuckingManager internal constructor(
    private val focusClient: AudioFocusClient,
    private val mediaMute: OverlayMediaMute? = null
) {
    constructor(context: Context, dispatchChange: (() -> Unit) -> Unit = { it() }) :
        this(AndroidAudioFocusClient(context, dispatchChange), OverlayMediaMute.forContext(context))

    enum class FocusState {
        IDLE, REQUESTING, HELD, DENIED, REQUEST_ERROR,
        LOST_TRANSIENT, LOST_CAN_DUCK, LOST_PERMANENT
    }

    private var owner: Long? = null
    private var generation = 0L
    @Volatile
    var focusState = FocusState.IDLE
        private set
    val hasAudioFocus: Boolean get() = focusState == FocusState.HELD

    @Synchronized
    fun requestTransientFocus(interventionId: Long): Boolean {
        if (owner == interventionId) return hasAudioFocus
        releaseCurrentFocus()
        owner = interventionId
        val requestGeneration = ++generation
        focusState = FocusState.REQUESTING
        DetectionTrace.audio(interventionId, focusState.name)
        var requestFailed = false
        val granted = try {
            focusClient.requestFocus(interventionId) { change ->
                onFocusChanged(interventionId, requestGeneration, change)
            }
        } catch (_: Exception) {
            requestFailed = true
            false
        }
        if (owner != interventionId || generation != requestGeneration) return false
        if (!granted) {
            releaseCurrentFocus()
            focusState = if (requestFailed) FocusState.REQUEST_ERROR else FocusState.DENIED
            DetectionTrace.audio(interventionId, focusState.name)
            return false
        }
        if (focusState == FocusState.REQUESTING) focusState = FocusState.HELD
        DetectionTrace.audio(interventionId, focusState.name)
        return hasAudioFocus
    }

    @Synchronized
    private fun onFocusChanged(interventionId: Long, requestGeneration: Long, change: FocusChange) {
        if (owner != interventionId || generation != requestGeneration) return
        if (focusState == FocusState.LOST_PERMANENT) return
        focusState = when (change) {
            FocusChange.GAIN -> FocusState.HELD
            FocusChange.LOSS_TRANSIENT -> FocusState.LOST_TRANSIENT
            FocusChange.LOSS_CAN_DUCK -> FocusState.LOST_CAN_DUCK
            FocusChange.LOSS_PERMANENT -> FocusState.LOST_PERMANENT
        }
        DetectionTrace.audio(interventionId, focusState.name)
    }

    internal fun recoverMediaMute() {
        mediaMute?.recover()
    }

    internal fun silenceOverlayMedia(interventionId: Long): Boolean {
        val muted = mediaMute?.acquire(interventionId) == true
        DetectionTrace.audio(interventionId, if (muted) "MEDIA_STREAM_MUTED" else "MEDIA_MUTE_UNAVAILABLE")
        return muted
    }

    @Synchronized
    fun restoreAudio(interventionId: Long) {
        mediaMute?.release(interventionId)
        if (owner != interventionId) return
        releaseCurrentFocus()
    }

    private fun releaseCurrentFocus() {
        val previous = owner
        owner = null
        generation++
        focusState = FocusState.IDLE
        if (previous != null) {
            try {
                focusClient.abandonFocus()
            } catch (_: Exception) {
                // Cleanup must never prevent blocking or release a newer owner.
            }
            DetectionTrace.audio(previous, "REQUEST_RELEASED")
        }
    }
}

private class AndroidAudioFocusClient(
    context: Context,
    private val dispatchChange: (() -> Unit) -> Unit
) : AudioFocusClient {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val targetSdk = context.applicationInfo.targetSdkVersion
    private var focusRequest: AudioFocusRequest? = null
    private var legacyListener: AudioManager.OnAudioFocusChangeListener? = null

    override fun requestFocus(interventionId: Long, onChange: (FocusChange) -> Unit): Boolean {
        val manager = audioManager ?: return false
        val listener = AudioManager.OnAudioFocusChangeListener { value ->
            val change = when (value) {
                AudioManager.AUDIOFOCUS_GAIN -> FocusChange.GAIN
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> FocusChange.LOSS_TRANSIENT
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> FocusChange.LOSS_CAN_DUCK
                AudioManager.AUDIOFOCUS_LOSS -> FocusChange.LOSS_PERMANENT
                else -> null
            }
            if (change != null) dispatchChange { onChange(change) }
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(listener)
                .build()
            focusRequest = request
            manager.requestAudioFocus(request)
        } else {
            legacyListener = listener
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
        val foregroundService = MonitoringForegroundService.isRunning()
        val restrictionPossible = Build.VERSION.SDK_INT >= 35 && targetSdk >= 35 &&
            result == AudioManager.AUDIOFOCUS_REQUEST_FAILED
        DetectionTrace.audio(
            interventionId,
            if (restrictionPossible) "BACKGROUND_FOCUS_RESTRICTION_POSSIBLE" else "REQUEST_RESULT",
            Build.VERSION.SDK_INT, targetSdk, foregroundService, result
        )
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun abandonFocus() {
        val request = focusRequest
        val legacy = legacyListener
        focusRequest = null
        legacyListener = null
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            request?.let { manager.abandonAudioFocusRequest(it) }
        } else if (legacy != null) {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(legacy)
        }
    }
}
