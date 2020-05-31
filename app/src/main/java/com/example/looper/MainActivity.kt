package com.example.looper

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.example.looper.AudioFilePlayer.isLoopingFile
import com.example.looper.AudioFilePlayer.pauseAudioFile
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


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

    private var saveActionItem: MenuItem? = null

    private var audioRecorder: AudioRecorder? = null
    private var samplePlayer: SamplePlayer? = null

    private val FILE_NAME = "/RECORDING.mp3"
    private val recordingsFolder =
        Environment.getExternalStorageDirectory().absolutePath + "/LooperRecordings" + "/"
    private var filePath: String? = null

//    private var filename: String? = null

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
        initialiseRecordingOutputFile()
        audioRecorder = AudioRecorder(filePath!!)
        samplePlayer = SamplePlayer(baseContext)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isLoopingFile = sharedPreferences.getBoolean("enableLooping", true)
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    fun initialiseRecordingOutputFile() {
        filePath = if (externalCacheDir != null) {
            externalCacheDir!!.absoluteFile.toString() + FILE_NAME
        } else {
            Environment.getExternalStorageDirectory().absolutePath + FILE_NAME
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_activity_bar, menu)
        saveActionItem = menu?.findItem(R.id.actionSave)
        saveActionItem?.isVisible = RecordingState.hasRecorded == true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.actionSave -> {
                showSavePopupWindow()
                true
            }
            R.id.actionLoad -> {
                showLoadPopupWindow()
                true
            }
            R.id.actionSettings -> {
                goToPreferences()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSavePopupWindow() {
        val savePopup = createPopupWindow(R.layout.save_window)
        val popupLayout = savePopup.contentView
        val saveButton = popupLayout.findViewById<Button>(R.id.saveButton)
        val closeButton = popupLayout.findViewById<ImageButton>(R.id.closeSaveMenu)
        val textView = popupLayout?.findViewById<EditText>(R.id.inputFileName)
        saveButton.setOnClickListener {
            savePopup.dismiss()
            saveRecording(textView?.text.toString())
        }
        closeButton.setOnClickListener { savePopup.dismiss() }
        openPopupWindow(savePopup)
    }


    private fun getSavedRecordings(): MutableList<String> {
        val savedRecordings = mutableListOf<String>()
        File(recordingsFolder).walk().forEach {
            savedRecordings.add(it.absolutePath)
        }
        return savedRecordings
    }

    private fun showLoadPopupWindow() {
        var selectedFile: String? = null
        val loadPopup = createPopupWindow(R.layout.load_window)
        val popupLayout = loadPopup.contentView

        val loadButton = popupLayout.findViewById<Button>(R.id.loadButton)
        loadButton.visibility = View.GONE
        val closeButton = popupLayout.findViewById<ImageButton>(R.id.closeLoadMenu)

        val savedRecordings = getSavedRecordings()

        val savedRecordingsSpinner: Spinner = popupLayout.findViewById(R.id.savedRecordings)

        val arrayAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, savedRecordings)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        savedRecordingsSpinner.adapter = arrayAdapter

        savedRecordingsSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedFile = parent.getItemAtPosition(position).toString()
                    loadButton.visibility = View.VISIBLE
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    loadButton.visibility = View.GONE
                }
            }

        loadButton.setOnClickListener {
            loadPopup.dismiss()
            loadRecording(selectedFile)
        }
        closeButton.setOnClickListener { loadPopup.dismiss() }
        openPopupWindow(loadPopup)
    }

    private fun openPopupWindow(window: PopupWindow?) {
        window?.showAtLocation(
            root_layout,
            Gravity.CENTER,
            0, -100
        )
    }

    private fun goToPreferences() {
        pauseAudioFile()
        startActivity(PreferencesActivity.newIntent(this))
    }

    private fun createPopupWindow(layoutId: Int): PopupWindow {
        val windowView: View = layoutInflater.inflate(layoutId, null)
        val window = PopupWindow(
            windowView,
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        window.elevation = 5.0f
        window.isFocusable = true // Allows keyboard to be shown
        return window
    }

    private fun saveRecording(userFilename: String?) {
        val savePath = recordingsFolder + userFilename + ".mp3"
        val outputFile = File(savePath)
        val recording = File(filePath).copyTo(outputFile, true)
    }

    private fun loadRecording(filePath: String?) {
        this.filePath = filePath
        showPlayButton()
        showDeleteButton()
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

    private fun onStartRecording() {
        RecordingState.hasRecorded = true
        showStopRecordButton()
        hidePlayPauseButtons()
        hideDeleteButton()
        hideSaveActionButton()
        audioRecorder?.start()
        initialiseRecordingOutputFile()
    }

    private fun onStopRecording() {
        showRecordButton()
        showPlayButton()
        showDeleteButton()
        showSaveActionButton()
        audioRecorder?.stop()
    }

    private fun onPlayRecording() {
        showPauseButton()
        filePath?.let { AudioFilePlayer.playAudioFile(it) }
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
        hideSaveActionButton()
        val file = File(filePath)
        val deleted = file.delete()
        initialiseRecordingOutputFile()
    }

    private fun showSaveActionButton() {
        saveActionItem?.isVisible = true
    }

    private fun hideSaveActionButton() {
        saveActionItem?.isVisible = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasRecorded", RecordingState.hasRecorded)
    }

    override fun onStop() {
        super.onStop()
        audioRecorder?.release()
        samplePlayer?.release()
        AudioFilePlayer.release()
        showRecordButton()
        if (RecordingState.isRecording) {
            notifyUser()
            showPlayButton()
            showDeleteButton()
        }
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


}