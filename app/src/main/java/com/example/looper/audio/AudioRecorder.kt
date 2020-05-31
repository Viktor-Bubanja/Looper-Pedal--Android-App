package com.example.looper.audio

import android.media.MediaRecorder
import android.util.Log
import java.io.IOException

class AudioRecorder(var filename: String) {

    private var recorder: MediaRecorder? = null

    fun start() {
        if (!RecordingState.isRecording) {
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(filename)
                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e("AAA", "prepare() failed")
                }
                start()
            }
            RecordingState.isRecording = true
        }
    }

    fun stop() {
        if (RecordingState.isRecording) {
            RecordingState.isRecording = false
            recorder?.stop()
        }
    }

    fun release() {
        recorder?.release()
        recorder = null
    }
}