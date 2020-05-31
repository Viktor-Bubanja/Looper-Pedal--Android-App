package com.example.looper

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudioFileViewModel(application: Application) : AndroidViewModel(application) {

    private val repositoryAudio: AudioFileRepository

    val allFiles: LiveData<List<AudioFile>>

    init {
        val audioFileDao = AudioFileDatabase.getDatabase(application).audioFileDao()
        repositoryAudio = AudioFileRepository(audioFileDao)
        allFiles = repositoryAudio.allFiles
    }

    fun insert(audioFile: AudioFile) = viewModelScope.launch(Dispatchers.IO) {
        repositoryAudio.insert(audioFile)
    }

    fun delete(audioFile: AudioFile) = viewModelScope.launch(Dispatchers.IO) {
        repositoryAudio.delete(audioFile)
    }

}
