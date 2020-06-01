package com.example.looper.activity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.example.looper.animation.clickAnimation
import com.example.looper.audio.AudioFilePlayer
import com.example.looper.*
import com.example.looper.audio.AudioFilePlayer.isLoopingFile
import com.example.looper.audio.AudioFilePlayer.pauseAudioFile
import com.example.looper.audio.AudioRecorder
import com.example.looper.audio.SamplePlayer
import com.example.looper.model.AudioFile
import com.example.looper.viewmodel.AudioFileViewModel
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Math.abs


private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val REQUEST_WRITE_FILE_PERMISSION = 400

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var recordButton: ImageButton
    private lateinit var stopRecordButton: ImageButton
    private lateinit var playButton: ImageButton
    private lateinit var pauseButton: ImageButton
    private lateinit var deleteButton: ImageButton

    private var saveActionItem: MenuItem? = null
    private var loadActionItem: MenuItem? = null
    private var settingsActionItem: MenuItem? = null
    private var shareActionItem: MenuItem? = null

    private var audioRecorder: AudioRecorder? = null
    private var samplePlayer: SamplePlayer? = null

    private var savedFileNames = mutableListOf<String>()

    private var currentFileName: String = DEFAULT_FILENAME

    private var autoPlayAudio: Boolean = false
    private var motionDetection: Boolean = false

    private val recordingsFolder =
        Environment.getExternalStorageDirectory().absolutePath + "/LooperRecordings" + "/"

    private lateinit var recordingCacheFolder: String
    private lateinit var currentFilePath: String

    private lateinit var audioFileViewModel: AudioFileViewModel

    private val audioState = AudioState()

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val MIN_ACCELERATION = 2F
    private val MIN_TIME_BETWEEN_EVENTS = 1e9 // 1 second
    private var lastRecordedSensorEvent = 0L

    private var permissionToRecordAccepted = false
    private var permissionToWriteFileAccepted = false

    private var permissions: Array<String> = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialiseRecordingButtons()
        createNotificationChannel()

        recordingCacheFolder = if (externalCacheDir != null) {
            externalCacheDir!!.absoluteFile.toString() + "/'"
        } else {
            Environment.getExternalStorageDirectory().absolutePath + "/"
        }
        currentFilePath = recordingCacheFolder

        audioFileViewModel = ViewModelProvider(this).get(AudioFileViewModel::class.java)
        audioFileViewModel.allFiles.observe(this, Observer { files ->
            savedFileNames.clear()
            files?.forEach { savedFileNames.add(it.name) }
            setLoadActionButtonVisibility(files?.isEmpty() == false)
        })

        audioState.isRecording = false
        audioState.recordingLoaded = false

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
    }

    override fun onStart() {
        super.onStart()

        audioRecorder = AudioRecorder(recordingCacheFolder + DEFAULT_FILENAME)
        samplePlayer = SamplePlayer(baseContext)
    }

    override fun onResume() {
        super.onResume();
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isLoopingFile = sharedPreferences.getBoolean("enableLooping", true)
        autoPlayAudio = sharedPreferences.getBoolean("autoPlayAudio", false)
        motionDetection = sharedPreferences.getBoolean("motionDetection", false)
        if (motionDetection) {
            registerAccelerometerListener()
        }
    }

    private fun registerAccelerometerListener() {
        sensorManager.registerListener(this, this.accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    override fun onPause() {
        super.onPause();
        if (motionDetection) {
            sensorManager.unregisterListener(this);
        }
    }

    override fun onStop() {
        super.onStop()
        audioRecorder?.release()
        samplePlayer?.release()
        AudioFilePlayer.release()
        if (audioState.isRecording) {
            notifyUser()
            audioState.recordingLoaded = true
        }
        audioState.isRecording = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_activity_bar, menu)
        saveActionItem = menu?.findItem(R.id.actionSave)
        saveActionItem?.isVisible = audioState.recordingLoaded == true

        loadActionItem = menu?.findItem(R.id.actionLoad)
        loadActionItem?.isVisible = audioFileViewModel.allFiles.value?.isEmpty() == false

        settingsActionItem = menu?.findItem(R.id.actionSettings)
        settingsActionItem?.isVisible = audioState.isRecording == false

        shareActionItem = menu?.findItem(R.id.actionShare)
        shareActionItem?.isVisible = audioState.recordingLoaded == true

        return true
    }

    private fun initialiseRecordingButtons() {
        recordButton = findViewById(R.id.startRecordingButton)
        stopRecordButton = findViewById(R.id.stopRecordingButton)
        playButton = findViewById(R.id.playButton)
        pauseButton = findViewById(R.id.pauseButton)
        deleteButton = findViewById(R.id.deleteButton)
    }

    fun playKick(v: View?) {
        samplePlayer?.playKick()
        clickAnimation(kickButton)
    }

    fun playSnare(v: View?) {
        samplePlayer?.playSnare()
        clickAnimation(snareButton)
    }

    fun playHat(v: View?) {
        samplePlayer?.playHat()
        clickAnimation(hatButton)
    }

    private fun getRecordedCacheFilePath(): String {
        return this.recordingCacheFolder + DEFAULT_FILENAME
    }

    fun onStartRecording(v: View?) {
        if (audioState.isPlaying) {
            onPauseAudio(null)
        }
        audioState.isRecording = true
        audioState.recordingLoaded = false
        audioRecorder?.start()
        resetFilePath()
    }

    fun onStopRecording(v: View?) {
        audioState.isRecording = false
        audioState.recordingLoaded = true
        audioRecorder?.stop()
        if (autoPlayAudio) {
            onPlayAudio(null)
        }
    }

    fun onPlayAudio(v: View?) {
        audioState.isPlaying = true
        AudioFilePlayer.playAudioFile(currentFilePath + currentFileName)
    }

    fun onPauseAudio(v: View?) {
        audioState.isPlaying = false
        pauseAudioFile()
    }

    fun deleteAudio(v: View?) {
        audioState.recordingLoaded = false
        AudioFilePlayer.clearAudioFile()
        AudioFilePlayer.release()
        val file = File(currentFilePath + currentFileName)
        file.delete()
        audioFileViewModel.delete(AudioFile(currentFileName))
        resetFilePath()
    }

    private fun saveRecording(userFilename: String) {
        val savePath = recordingsFolder + userFilename
        val outputFile = File(savePath)
        File(getRecordedCacheFilePath()).copyTo(outputFile, true)
        audioFileViewModel.insert(AudioFile(userFilename))
        setFilePath(userFilename)
    }

    private fun loadRecording(filename: String?) {
        audioState.recordingLoaded = true
        this.currentFilePath = recordingsFolder
        this.currentFileName = filename!!
        setFilePath(filename)
    }

    private fun resetFilePath() {
        currentFilePath = recordingCacheFolder
        currentFileName = DEFAULT_FILENAME
    }

    private fun setFilePath(filename: String) {
        currentFilePath = recordingsFolder
        currentFileName = filename
    }

    private fun setSaveActionButtonVisibility(visible: Boolean) {
        saveActionItem?.isVisible = visible
    }

    private fun setLoadActionButtonVisibility(visible: Boolean) {
        loadActionItem?.isVisible = visible
    }

    private fun setSettingsActionButtonVisibility(visible: Boolean) {
        settingsActionItem?.isVisible = visible
    }

    private fun setShareActionButtonVisibility(visible: Boolean) {
        shareActionItem?.isVisible = visible
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        val timestamp = sensorEvent?.timestamp ?: 0L
        val timeSinceLastEvent = timestamp - lastRecordedSensorEvent

        val change = sensorEvent?.values?.get(0).let { abs(it!!) }
        if (sensorEvent?.sensor?.type == Sensor.TYPE_ACCELEROMETER
            && change > MIN_ACCELERATION
            && timeSinceLastEvent > MIN_TIME_BETWEEN_EVENTS
        ) {
            lastRecordedSensorEvent = timestamp
            if (audioState.isRecording) {
                onStopRecording(null)
            } else {
                onStartRecording(null)
            }
        }
    }

    fun goToPreferences(m: MenuItem) {
        onPauseAudio(null)
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

    fun showSavePopupWindow(m: MenuItem) {
        val savePopup = createPopupWindow(R.layout.save_window)
        val popupLayout = savePopup.contentView
        val saveButton = popupLayout.findViewById<Button>(R.id.saveButton)
        val closeButton = popupLayout.findViewById<ImageButton>(R.id.closeSaveMenu)
        val textView = popupLayout?.findViewById<EditText>(R.id.inputFilename)
        saveButton.setOnClickListener {
            val inputText: String = textView?.text.toString()
            if (inputText.isEmpty()) {
                val text = getString(R.string.empty_filename_msg)
                val duration = Toast.LENGTH_SHORT
                Toast.makeText(applicationContext, text, duration).show()
            } else {
                saveRecording(inputText)
            }
            savePopup.dismiss()
        }
        closeButton.setOnClickListener { savePopup.dismiss() }
        openPopupWindow(savePopup)
    }

    fun showLoadPopupWindow(m: MenuItem) {
        var selectedFile: String? = null
        val loadPopup = createPopupWindow(R.layout.load_window)
        val popupLayout = loadPopup.contentView

        val loadButton = popupLayout.findViewById<Button>(R.id.loadButton)
        loadButton.visibility = View.GONE

        val closeButton = popupLayout.findViewById<ImageButton>(R.id.closeLoadMenu)
        val savedRecordingsSpinner: Spinner = popupLayout.findViewById(R.id.savedRecordings)

        val arrayAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, this.savedFileNames)
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

    private fun notifyUser() {
        val notificationId = System.currentTimeMillis().toInt()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.looper_icon)
            .setContentTitle(getString(R.string.notification_title))
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
        val channel = NotificationChannel(
            CHANNEL_ID, name, importance
        ).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun shareRecording(m: MenuItem) {
        onPauseAudio(null)
        val recording = File(currentFilePath + currentFileName)
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_STREAM,
                FileProvider.getUriForFile(
                    applicationContext,
                    "$packageName.provider",
                    recording
                )
            )
            type = "audio/mpeg"
        }
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
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

    /**
     * Class responsible for storing the current state of recorded audio, i.e. whether the audio is
     * currently recording, currently playing, or has been loaded.
     * The setter methods handle front-end changes as necessary.
     */
    inner class AudioState {

        var isPlaying = false
            set(value) {
                field = value
                when (value) {
                    true -> {
                        if (recordingLoaded) showPauseButton()
                    }
                    false -> {
                        if (recordingLoaded) showPlayButton()
                    }
                }
            }

        var isRecording = false
            set(value) {
                field = value
                setShareActionButtonVisibility(!value)
                when (value) {
                    true -> {
                        showStopRecordButton()
                    }
                    false -> {
                        showRecordButton()
                    }
                }
            }

        var recordingLoaded = false
            set(value) {
                field = value
                setSaveActionButtonVisibility(value)
                setShareActionButtonVisibility(value)
                when (value) {
                    true -> {
                        showPlayButton()
                        showDeleteButton()
                    }
                    false -> {
                        hideDeleteButton()
                        hidePlayPauseButtons()
                    }
                }
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
}