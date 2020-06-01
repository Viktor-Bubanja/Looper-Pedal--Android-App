package com.example.looper.audio

import android.media.MediaRecorder
import android.util.Log
import java.io.IOException
import java.lang.Exception

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
                start()
            } catch (e: Exception) {
                Log.e("AudioRecorder", e.stackTrace.toString())
            }
        }
    }

    fun stop() {
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            // Throws RuntimeException when stop() is called when recording is already stopped.
            Log.e("AudioRecorder", e.stackTrace.toString())
        }

    }

    fun release() {
        recorder?.release()
        recorder = null
    }
}