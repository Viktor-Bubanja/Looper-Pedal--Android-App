package com.example.looper.audio

import android.media.MediaRecorder
import android.util.Log
import java.io.IOException

class AudioRecorder(var filename: String) {

    private var recorder: MediaRecorder? = null

    fun start() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(filename)
            try {
                prepare()
            } catch (e: IOException) {
                Log.e("AudioRecorder", "prepare() failed")
            }
            start()
        }
    }

    fun stop() {
        recorder?.stop()
    }

    fun release() {
        recorder?.release()
        recorder = null
    }
}