package com.example.looper

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AudioFileDao {

    @Query("SELECT * FROM audio_file")
    fun getFiles(): LiveData<List<AudioFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audioFile: AudioFile)

    @Delete
    suspend fun delete(audioFile: AudioFile)

}