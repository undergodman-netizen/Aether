package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.VideoEntity
import com.example.data.PdfEntity
import com.example.data.VideoRepository
import com.example.data.VideoShotEntity
import com.example.data.VideoNoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class VideoViewModel(private val repository: VideoRepository) : ViewModel() {
    val allVideos: StateFlow<List<VideoEntity>> = repository.allVideos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPdfs: StateFlow<List<PdfEntity>> = repository.allPdfs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allShots: StateFlow<List<VideoShotEntity>> = repository.allShots.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getVideosBySubject(subject: String): StateFlow<List<VideoEntity>> {
        return repository.getVideosBySubject(subject).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun getPdfsBySubject(subject: String): StateFlow<List<PdfEntity>> {
        return repository.getPdfsBySubject(subject).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addVideo(context: Context, uri: Uri, subject: String) {
        viewModelScope.launch {
            try {
                // Persist permission so we can read it again later
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Extract title
                var title = "Unknown Video"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            title = cursor.getString(nameIndex)
                        }
                    }
                }

                repository.insert(
                    VideoEntity(
                        uri = uri.toString(),
                        title = title,
                        subject = subject
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addPdf(context: Context, uri: Uri, subject: String) {
        viewModelScope.launch {
            try {
                // Persist permission so we can read it again later
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Extract title
                var title = "Unknown PDF"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            title = cursor.getString(nameIndex)
                        }
                    }
                }

                repository.insertPdf(
                    PdfEntity(
                        uri = uri.toString(),
                        title = title,
                        subject = subject
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteVideo(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun updateVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.update(video)
        }
    }

    fun updatePdf(pdf: PdfEntity) {
        viewModelScope.launch {
            repository.updatePdf(pdf)
        }
    }

    fun deletePdf(id: Int) {
        viewModelScope.launch {
            repository.deletePdfById(id)
        }
    }

    fun deleteVideoShot(id: Long) {
        viewModelScope.launch {
            repository.deleteVideoShot(id)
        }
    }

    fun deleteShotsForVideo(videoUri: String) {
        viewModelScope.launch {
            repository.deleteShotsForVideo(videoUri)
        }
    }

    fun getShotsForVideo(videoUri: String): Flow<List<VideoShotEntity>> = repository.getShotsForVideo(videoUri)

    fun getNotesForVideo(videoUri: String): Flow<List<VideoNoteEntity>> = repository.getNotesForVideo(videoUri)

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun captureVideoShot(context: Context, videoUri: String, positionMs: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var retriever: MediaMetadataRetriever? = null
            try {
                retriever = MediaMetadataRetriever()
                if (videoUri.startsWith("content://") || videoUri.startsWith("file://")) {
                    retriever.setDataSource(context, Uri.parse(videoUri))
                } else {
                    retriever.setDataSource(videoUri, HashMap<String, String>())
                }
                val bitmap = retriever.getFrameAtTime(positionMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val file = File(context.cacheDir, "shot_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    repository.insertVideoShot(VideoShotEntity(videoUri = videoUri, timestamp = positionMs, imagePath = file.absolutePath))
                    withContext(Dispatchers.Main) { onResult(true) }
                    return@launch
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { retriever?.release() } catch (e: Exception) {}
            }
            withContext(Dispatchers.Main) { onResult(false) }
        }
    }
}

class VideoViewModelFactory(private val repository: VideoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
