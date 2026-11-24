package com.blockreels.app.intervention

import android.content.Context
import android.media.AudioManager
import android.provider.Settings

/** Owned by the audio thread. The journal allows recovery if Android kills the service process. */
internal class OverlayMediaMute(
    private val stream: Stream,
    private val journal: Journal,
    private val bootCount: () -> Int?
) {
    interface Stream {
        val isFixed: Boolean
        val isMuted: Boolean
        fun setMuted(muted: Boolean)
    }

    interface Journal {
        fun read(): Int?
        fun write(boot: Int): Boolean
        fun clear()
    }

    private var owner: Long? = null

    fun acquire(id: Long): Boolean = runCatching {
        if (owner == id) return@runCatching stream.isMuted
        if (owner != null || stream.isFixed) return@runCatching false
        if (journal.read() != null) {
            recover()
            if (journal.read() != null) return@runCatching false
        }
        if (stream.isMuted) return@runCatching true
        val boot = bootCount() ?: return@runCatching false
        if (!journal.write(boot)) return@runCatching false
        owner = id
        stream.setMuted(true)
        if (stream.isMuted) return@runCatching true
        release(id)
        false
    }.getOrDefault(false)

    fun release(id: Long) {
        if (owner != id) return
        owner = null
        recover()
    }

    fun recover() {
        runCatching {
            if (owner != null) return
            val recordedBoot = journal.read() ?: return
            val currentBoot = bootCount() ?: return
            if (recordedBoot == currentBoot && stream.isMuted) {
                stream.setMuted(false)
                if (stream.isMuted) return
            }
            journal.clear()
        }
    }

    companion object {
        fun forContext(context: Context): OverlayMediaMute? {
            val audio = context.getSystemService(AudioManager::class.java) ?: return null
            val preferences = context.getSharedPreferences("overlay_media", Context.MODE_PRIVATE)
            val stream = object : Stream {
                override val isFixed: Boolean get() = audio.isVolumeFixed
                override val isMuted: Boolean get() = audio.isStreamMute(AudioManager.STREAM_MUSIC)

                override fun setMuted(muted: Boolean) {
                    val direction = if (muted) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
                }
            }
            val journal = object : Journal {
                private val key = "muted_at_boot"
                override fun read() = preferences.getInt(key, -1).takeIf { it >= 0 }
                override fun write(boot: Int) = preferences.edit().putInt(key, boot).commit()
                override fun clear() {
                    preferences.edit().remove(key).commit()
                }
            }
            return OverlayMediaMute(stream, journal) {
                Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
                    .takeIf { it >= 0 }
            }
        }
    }
}
