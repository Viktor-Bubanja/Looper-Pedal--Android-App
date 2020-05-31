package com.example.looper

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(AudioFile::class), version = 1, exportSchema = false)
public abstract class AudioFileDatabase : RoomDatabase() {

    abstract fun audioFileDao(): AudioFileDao

    companion object {

        @Volatile
        private var INSTANCE: AudioFileDatabase? = null

        fun getDatabase(context: Context): AudioFileDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AudioFileDatabase::class.java,
                    "audio_file_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}