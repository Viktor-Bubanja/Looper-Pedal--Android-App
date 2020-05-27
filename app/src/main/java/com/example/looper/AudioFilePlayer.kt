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
        Log.d("AAA", "play audio file")
        Log.d("AAA", filename)

        if (soundPool == null) {
            soundPool = initialiseSoundPool()
        }
        soundPool?.setOnLoadCompleteListener { soundPool, sampleId, status ->
            audioStreamId = playSound(sampleId, soundPool, this.isLoopingFile)
        }
        loadedAudioId = soundPool?.load(filename, 1)
    }

    fun pauseAudioFile() {
        Log.d("AAA", "pause audio file")
        Log.d("AAA", audioStreamId.toString())
        audioStreamId?.let { soundPool?.pause(it) }
    }

    fun clearAudioFile() {
        pauseAudioFile()
        Log.d("AAA", "clear audio file")
        Log.d("AAA", audioStreamId.toString())
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