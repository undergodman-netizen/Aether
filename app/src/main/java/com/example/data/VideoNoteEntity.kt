package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_notes")
data class VideoNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoUri: String,
    val timestamp: Long,
    val noteText: String
)
