package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_shots")
data class VideoShotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoUri: String,
    val timestamp: Long,
    val imagePath: String
)
