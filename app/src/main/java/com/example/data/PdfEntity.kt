package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdfs")
data class PdfEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String,
    val title: String,
    val subject: String = "Other",
    val timestampAdded: Long = System.currentTimeMillis()
)
