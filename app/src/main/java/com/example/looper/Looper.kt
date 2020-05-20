package com.example.looper


import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_audio_recorder.*

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val REQUEST_WRITE_FILE_PERMISSION = 400

class Looper : AppCompatActivity() {

    private lateinit var recordButton: ImageButton
    private lateinit var stopRecordButton: ImageButton

    private lateinit var playButton: ImageButton
    private lateinit var pauseButton: ImageButton

    private lateinit var deleteButton: ImageButton

    private lateinit var kickButton: ImageButton
    private lateinit var snareButton: ImageButton
    private lateinit var hatButton: ImageButton

    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null

    private var filename: String? = null

    private var permissionToRecordAccepted = false
    private var permissionToWriteFileAccepted = false

    private var permissions: Array<String> = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_audio_recorder)
        initialiseRecordingButtons()

        filename = "${externalCacheDir.absolutePath}/audiorecordtest.mp3"
//        fileName = Environment.getExternalStorageDirectory().absolutePath + "/recording.mp3"

        audioRecorder = AudioRecorder(filename!!)
        audioPlayer = AudioPlayer(baseContext)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    private fun initialiseRecordingButtons() {
        recordButton = findViewById(R.id.startRecordingButton)
        stopRecordButton = findViewById(R.id.stopRecordingButton)
        playButton = findViewById(R.id.playButton)
        pauseButton = findViewById(R.id.pauseButton)
        deleteButton = findViewById(R.id.deleteButton)

        kickButton = findViewById(R.id.kickButton)
        snareButton = findViewById(R.id.snareButton)
        hatButton = findViewById(R.id.hatButton)

        recordButton.setOnClickListener { onStartRecording() }
        stopRecordButton.setOnClickListener { onStopRecording() }
        playButton.setOnClickListener { onPlayRecording() }
        pauseButton.setOnClickListener { onPauseRecording() }

        kickButton.setOnClickListener {
            audioPlayer?.playKick()
            clickAnimation(kickButton)
        }

        snareButton.setOnClickListener {
            audioPlayer?.playSnare()
            clickAnimation(snareButton)
        }

        hatButton.setOnClickListener {
            audioPlayer?.playHat()
            clickAnimation(hatButton)
        }

        stopRecordButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        playButton.visibility = View.GONE
        deleteButton.visibility = View.GONE
    }

    private fun clickAnimation(element: ImageButton) {
        val duration = 100L
        val valueAnimator = ValueAnimator.ofFloat(1.0F, 1.2F, 1.0F)
        valueAnimator.addUpdateListener {
            val animationValue = it.animatedValue as Float
            element.scaleX = animationValue
            element.scaleY = animationValue
        }
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.duration = duration
        valueAnimator.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
            REQUEST_WRITE_FILE_PERMISSION -> {
                permissionToWriteFileAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun onStartRecording() {
        startRecordingButton.visibility = View.GONE
        stopRecordButton.visibility = View.VISIBLE
        audioRecorder?.start()
    }

    private fun onStopRecording() {
        startRecordingButton.visibility = View.VISIBLE
        stopRecordButton.visibility = View.GONE
        playButton.visibility = View.VISIBLE
        audioRecorder?.stop()
        deleteButton.visibility = View.VISIBLE
    }

    private fun onPlayRecording() {
        playButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE
        filename?.let { audioPlayer?.playAudioFile(it) }
    }

    private fun onPauseRecording() {
        playButton.visibility = View.VISIBLE
        pauseButton.visibility = View.GONE
        audioPlayer?.pauseAudioFile()
    }

    fun deleteAudio(view: View) {
        filename = null
        audioPlayer?.clearAudioFile()
        deleteButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        playButton.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        audioRecorder?.release()
        audioPlayer?.release()
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, Looper::class.java)
    }
}