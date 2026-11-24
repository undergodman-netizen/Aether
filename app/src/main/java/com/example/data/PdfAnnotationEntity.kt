package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_annotations", primaryKeys = ["pdfUri", "pageNumber"])
data class PdfAnnotationEntity(
    val pdfUri: String,
    val pageNumber: Int,
    val annotationsJson: String // Serialized StrokeList
)
