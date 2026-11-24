package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoNoteDao {
    @Query("SELECT * FROM video_notes WHERE videoUri = :videoUri ORDER BY timestamp ASC")
    fun getNotesForVideo(videoUri: String): Flow<List<VideoNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: VideoNoteEntity)

    @Query("DELETE FROM video_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Int)
}
