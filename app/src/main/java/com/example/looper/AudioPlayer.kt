package com.example.looper

import android.media.AudioAttributes
import android.media.SoundPool

object AudioPlayer {

    fun initialiseSoundPool(): SoundPool {
        val audioAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        return SoundPool.Builder().setMaxStreams(4).setAudioAttributes(audioAttributes).build()
    }

    fun playSound(soundId: Int, soundPool: SoundPool?, loop: Boolean = false): Int? {
        val loopValue = if (loop) -1 else 0
        return soundPool?.play(soundId, 1F, 1F, 1, loopValue, 1F)
    }
}

interface SoundPoolHolder {
    fun release()
}