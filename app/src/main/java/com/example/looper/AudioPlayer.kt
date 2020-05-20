package com.example.looper

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log


class AudioPlayer(context: Context) {

    private var soundPool: SoundPool? = null
    private var kickId: Int? = null
    private var snareId: Int? = null
    private var hatId: Int? = null
    private var loadedAudioId: Int? = null
    private var audioStreamId: Int? = null

    init {
        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(audioAttributes).build()
        kickId = soundPool?.load(context, R.raw.kick, 1)
        snareId = soundPool?.load(context, R.raw.snare, 1)
        hatId = soundPool?.load(context, R.raw.hat, 1)
    }

    fun playKick() {
        kickId?.let { playSound(it) }
    }

    fun playSnare() {
        snareId?.let { playSound(it) }
    }

    fun playHat() {
        hatId?.let { playSound(it) }
    }

    private fun playSound(soundId: Int, loop: Boolean = false) {
        val loopValue = if (loop) -1 else 0
        soundPool?.play(soundId, 1F, 1F, 1, loopValue, 1F)
    }

    fun playAudioFile(filename: String) {
        if (audioStreamId != null) {
            audioStreamId?.let {soundPool?.resume(it) }
        } else {
            soundPool?.setOnLoadCompleteListener { soundPool, sampleId, status ->
                audioStreamId = soundPool?.play(sampleId, 1F, 1F, 1, -1, 1F)
            }
            loadedAudioId = soundPool!!.load(filename, 1)
        }
    }

    fun pauseAudioFile() {
        audioStreamId?.let { soundPool?.pause(it) }
    }

    fun clearAudioFile() {
        pauseAudioFile()
        audioStreamId?.let { soundPool?.stop(it) }
        loadedAudioId?.let { soundPool?.unload(it) }
        audioStreamId = null
        loadedAudioId = null
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}