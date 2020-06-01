package com.example.looper.audio

import android.media.SoundPool
import com.example.looper.audio.AudioPlayer.initialiseSoundPool
import com.example.looper.audio.AudioPlayer.playSound
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


object AudioFilePlayer : SoundPoolHolder {
    var loadedAudioId: Int? = null
    private var audioStreamId: Int? = null
    var isLoopingFile: Boolean = true
    private var soundPool: SoundPool? = null

    init {
        soundPool = initialiseSoundPool()
    }

    fun playAudioFile(filename: String) {
        GlobalScope.launch {
            if (soundPool == null) {
                soundPool = initialiseSoundPool()
            }
            soundPool?.setOnLoadCompleteListener { soundPool, sampleId, _ ->
                audioStreamId = playSound(sampleId, soundPool, isLoopingFile)
            }
            loadedAudioId = soundPool?.load(filename, 1)
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

    override fun release() {
        soundPool?.release()
        soundPool = null
    }
}