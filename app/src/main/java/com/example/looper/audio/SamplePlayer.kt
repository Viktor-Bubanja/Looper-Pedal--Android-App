package com.example.looper.audio

import android.content.Context
import android.media.SoundPool
import android.util.Log
import com.example.looper.audio.AudioPlayer.initialiseSoundPool
import com.example.looper.audio.AudioPlayer.playSound
import com.example.looper.R

class SamplePlayer(context: Context): SoundPoolHolder {
    private var kickId: Int? = null
    private var snareId: Int? = null
    private var hatId: Int? = null
    private var soundPool: SoundPool? = null

    init {
        soundPool = initialiseSoundPool()
        kickId = soundPool?.load(context, R.raw.kick, 1)
        snareId = soundPool?.load(context, R.raw.snare, 1)
        hatId = soundPool?.load(context, R.raw.hat, 1)
    }

    fun playKick() {
        kickId?.let { playSound(it, soundPool) }
    }

    fun playSnare() {
        snareId?.let { playSound(it, soundPool) }
    }

    fun playHat() {
        hatId?.let { playSound(it, soundPool) }
    }

    override fun release() {
        soundPool?.release()
        soundPool = null
        kickId = null
        snareId = null
        hatId = null
    }
}