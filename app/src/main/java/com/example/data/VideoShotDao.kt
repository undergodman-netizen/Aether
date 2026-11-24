package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoShotDao {
    @Insert
    suspend fun insert(shot: VideoShotEntity)

    @Query("SELECT * FROM video_shots WHERE videoUri = :videoUri ORDER BY timestamp ASC")
    fun getShotsForVideo(videoUri: String): Flow<List<VideoShotEntity>>

    @Query("SELECT * FROM video_shots ORDER BY id DESC")
    fun getAllShots(): Flow<List<VideoShotEntity>>
    
    @Query("DELETE FROM video_shots WHERE id = :id")
    suspend fun deleteShot(id: Long)

    @Query("DELETE FROM video_shots WHERE videoUri = :videoUri")
    suspend fun deleteShotsForVideo(videoUri: String)
}
