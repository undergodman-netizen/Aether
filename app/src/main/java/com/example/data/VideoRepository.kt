package com.example.data

import kotlinx.coroutines.flow.Flow

class VideoRepository(
    private val videoDao: VideoDao,
    private val pdfDao: PdfDao,
    private val videoShotDao: VideoShotDao,
    private val videoNoteDao: VideoNoteDao
) {
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val allPdfs: Flow<List<PdfEntity>> = pdfDao.getAllPdfs()
    val allShots: Flow<List<VideoShotEntity>> = videoShotDao.getAllShots()

    fun getNotesForVideo(videoUri: String): Flow<List<VideoNoteEntity>> = videoNoteDao.getNotesForVideo(videoUri)

    suspend fun insertNote(note: VideoNoteEntity) {
        videoNoteDao.insertNote(note)
    }

    suspend fun deleteNote(id: Int) {
        videoNoteDao.deleteNote(id)
    }

    fun getVideosBySubject(subject: String): Flow<List<VideoEntity>> {
        return videoDao.getVideosBySubject(subject)
    }

    fun getPdfsBySubject(subject: String): Flow<List<PdfEntity>> {
        return pdfDao.getPdfsBySubject(subject)
    }

    suspend fun insert(video: VideoEntity) {
        videoDao.insertVideo(video)
    }

    suspend fun updateVideo(video: VideoEntity) {
        videoDao.updateVideo(video)
    }

    suspend fun insertPdf(pdf: PdfEntity) {
        pdfDao.insertPdf(pdf)
    }

    suspend fun update(video: VideoEntity) {
        videoDao.updateVideo(video)
    }

    suspend fun updatePdf(pdf: PdfEntity) {
        pdfDao.updatePdf(pdf)
    }

    suspend fun deleteById(id: Int) {
        videoDao.deleteVideoById(id)
    }

    suspend fun deletePdfById(id: Int) {
        pdfDao.deletePdfById(id)
    }

    fun getShotsForVideo(videoUri: String): Flow<List<VideoShotEntity>> = videoShotDao.getShotsForVideo(videoUri)

    suspend fun insertVideoShot(shot: VideoShotEntity) {
        videoShotDao.insert(shot)
    }

    suspend fun deleteVideoShot(id: Long) {
        videoShotDao.deleteShot(id)
    }

    suspend fun deleteShotsForVideo(videoUri: String) {
        videoShotDao.deleteShotsForVideo(videoUri)
    }
}
