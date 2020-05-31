package com.example.looper.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_file")
data class AudioFile(@PrimaryKey val name: String)