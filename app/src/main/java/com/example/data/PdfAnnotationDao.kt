package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PdfAnnotationDao {
    @Query("SELECT * FROM pdf_annotations WHERE pdfUri = :pdfUri AND pageNumber = :pageNumber LIMIT 1")
    suspend fun getAnnotations(pdfUri: String, pageNumber: Int): PdfAnnotationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: PdfAnnotationEntity)
    
    @Query("DELETE FROM pdf_annotations WHERE pdfUri = :pdfUri")
    suspend fun deleteAnnotationsForPdf(pdfUri: String)
}
