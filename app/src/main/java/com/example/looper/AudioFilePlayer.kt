package com.example.looper

import android.media.SoundPool
import android.util.Log
import com.example.looper.AudioPlayer.initialiseSoundPool
import com.example.looper.AudioPlayer.playSound


object AudioFilePlayer: SoundPoolHolder {
    var loadedAudioId: Int? = null
    private var audioStreamId: Int? = null
    var isLoopingFile: Boolean = true
    private var soundPool: SoundPool? = null

    init {
        soundPool = initialiseSoundPool()
    }

    fun playAudioFile(filename: String) {
        if (soundPool == null) {
            soundPool = initialiseSoundPool()
        }
        soundPool?.setOnLoadCompleteListener { soundPool, sampleId, status ->
            audioStreamId = playSound(sampleId, soundPool, this.isLoopingFile)
        }
        loadedAudioId = soundPool?.load(filename, 1)
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

    override fun release() {
        soundPool?.release()
        soundPool = null
    }
}