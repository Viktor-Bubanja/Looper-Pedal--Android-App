package com.example.looper.repository

import androidx.lifecycle.LiveData
import com.example.looper.dao.AudioFileDao
import com.example.looper.model.AudioFile

class AudioFileRepository(private val audioFileDao: AudioFileDao) {

    val allFiles: LiveData<List<AudioFile>> = audioFileDao.getFiles()

    suspend fun insert(audioFile: AudioFile) {
        audioFileDao.insert(audioFile)
    }

    suspend fun delete(audioFile: AudioFile) {
        audioFileDao.delete(audioFile)
    }
}