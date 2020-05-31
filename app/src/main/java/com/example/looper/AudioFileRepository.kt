package com.example.looper

import androidx.lifecycle.LiveData

class AudioFileRepository(private val audioFileDao: AudioFileDao) {

    val allFiles: LiveData<List<AudioFile>> = audioFileDao.getFiles()

    suspend fun insert(audioFile: AudioFile) {
        audioFileDao.insert(audioFile)
    }

    suspend fun delete(audioFile: AudioFile) {
        audioFileDao.delete(audioFile)
    }
}