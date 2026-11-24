package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdfs ORDER BY timestampAdded DESC")
    fun getAllPdfs(): Flow<List<PdfEntity>>

    @Query("SELECT * FROM pdfs WHERE subject = :subject ORDER BY timestampAdded DESC")
    fun getPdfsBySubject(subject: String): Flow<List<PdfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: PdfEntity): Long

    @androidx.room.Update
    suspend fun updatePdf(pdf: PdfEntity)

    @Query("DELETE FROM pdfs WHERE id = :id")
    suspend fun deletePdfById(id: Int)
}
