package com.example.looper


import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.example.looper.AudioFilePlayer.isLoopingFile
import com.example.looper.AudioFilePlayer.loadedAudioId
import com.example.looper.AudioFilePlayer.pauseAudioFile

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val REQUEST_WRITE_FILE_PERMISSION = 400

class MainActivity : AppCompatActivity() {

    private lateinit var recordButton: ImageButton
    private lateinit var stopRecordButton: ImageButton

    private lateinit var playButton: ImageButton
    private lateinit var pauseButton: ImageButton

    private lateinit var deleteButton: ImageButton

    private lateinit var kickButton: ImageButton
    private lateinit var snareButton: ImageButton
    private lateinit var hatButton: ImageButton

    private var audioRecorder: AudioRecorder? = null
    private var samplePlayer: SamplePlayer? = null

    private var filename: String? = null

    private val CHANNEL_ID: String = "100"

    private var permissionToRecordAccepted = false
    private var permissionToWriteFileAccepted = false

    private var permissions: Array<String> = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createNotificationChannel()
        initialiseRecordingButtons()
        RecordingState.isRecording = false

        Log.d("AAA", "on createa again")

        filename = "${externalCacheDir.absolutePath}/audiorecordtest.mp3"
//        fileName = Environment.getExternalStorageDirectory().absolutePath + "/recording.mp3"

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isLoopingFile = sharedPreferences.getBoolean("enableLooping", true)
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)


    }

    override fun onStart() {
        super.onStart()
        audioRecorder = AudioRecorder(filename!!)
        samplePlayer = SamplePlayer(baseContext)
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
            Log.d("AAA", "samplePlayer")
            Log.d("AAA", samplePlayer.toString())
            samplePlayer?.playKick()
            clickAnimation(kickButton)
        }

        snareButton.setOnClickListener {
            samplePlayer?.playSnare()
            clickAnimation(snareButton)
        }

        hatButton.setOnClickListener {
            samplePlayer?.playHat()
            clickAnimation(hatButton)
        }

        stopRecordButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        playButton.visibility = View.GONE
        deleteButton.visibility = View.GONE
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d("AAA", "onrestoreinstancestate called")
        val hasRecorded = savedInstanceState?.getBoolean("hasRecorded", false)

        if (hasRecorded == true) {
            showPlayButton()
            showDeleteButton()
            RecordingState.hasRecorded = true
        } else {
            hidePlayPauseButtons()
            RecordingState.hasRecorded = false
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("AAA", "onresume called")
        if (loadedAudioId != null) {
            Log.d("AAA", "loaded audio id not null")
            Log.d("AAA", loadedAudioId.toString())
        }
    }

    private fun onStartRecording() {
        RecordingState.hasRecorded = true
        showStopRecordButton()
        audioRecorder?.start()
    }

    private fun onStopRecording() {
        showRecordButton()
        showPlayButton()
        showDeleteButton()
        audioRecorder?.stop()
    }

    private fun onPlayRecording() {
        showPauseButton()
        Log.d("AAA", "play recording")
        Log.d("AAA", filename.toString())
        filename?.let { AudioFilePlayer.playAudioFile(it) }
    }

    private fun onPauseRecording() {
        showPlayButton()
        pauseAudioFile()
    }

    fun deleteAudio(view: View) {
        RecordingState.hasRecorded = false
        AudioFilePlayer.clearAudioFile()
        AudioFilePlayer.release()
        hideDeleteButton()
        hidePlayPauseButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("AAA", "save instanec sttae")
        outState.putBoolean("hasRecorded", RecordingState.hasRecorded)
    }

    override fun onStop() {
        super.onStop()
        audioRecorder?.release()
        samplePlayer?.release()
        AudioFilePlayer.release()
        showRecordButton()
        showPlayButton()
        showDeleteButton()
        if (RecordingState.isRecording) {
            Log.d("AAA", RecordingState.isRecording.toString())
            notifyUser()
        }

        Log.d("AAA", "on stop called")
    }

    private fun notifyUser() {
        val notificationId = System.currentTimeMillis().toInt()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)


        val title = "Looper stopped recording"
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.looper_icon)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
        }
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showRecordButton() {
        recordButton.visibility = View.VISIBLE
        stopRecordButton.visibility = View.GONE
    }

    private fun showStopRecordButton() {
        recordButton.visibility = View.GONE
        stopRecordButton.visibility = View.VISIBLE
    }

    private fun showPlayButton() {
        playButton.visibility = View.VISIBLE
        pauseButton.visibility = View.GONE
    }

    private fun showPauseButton() {
        playButton.visibility = View.GONE
        pauseButton.visibility = View.VISIBLE
    }

    private fun showDeleteButton() {
        deleteButton.visibility = View.VISIBLE
    }

    private fun hideDeleteButton() {
        deleteButton.visibility = View.GONE
    }

    private fun hidePlayPauseButtons() {
        playButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
    }

    fun goToPreferences(view: View) {
        pauseAudioFile()
        startActivity(PreferencesActivity.newIntent(this))
    }

}