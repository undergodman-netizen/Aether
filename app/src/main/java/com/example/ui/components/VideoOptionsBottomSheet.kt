package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoOptionsBottomSheet(
    video: VideoEntity,
    onDismiss: () -> Unit,
    onUpdateVideo: (VideoEntity) -> Unit,
    onDeleteVideo: (VideoEntity) -> Unit,
    onViewVideoShots: (VideoEntity) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showChangePlaylistDialog by remember { mutableStateOf(false) }

    if (!showRenameDialog && !showChangePlaylistDialog) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "VIDEO OPTIONS",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showRenameDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename Video")
                    Spacer(Modifier.width(16.dp))
                    Text("Rename Video")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showChangePlaylistDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Book, contentDescription = "Change Playlist")
                    Spacer(Modifier.width(16.dp))
                    Text("Change Playlist / Subject")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onViewVideoShots(video)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Video Shots")
                    Spacer(Modifier.width(16.dp))
                    Text("View Video Shots")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDeleteVideo(video)
                            onDismiss()
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Video", tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Text("Delete Video", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(video.title) }
        AlertDialog(
            onDismissRequest = { 
                showRenameDialog = false
                onDismiss()
            },
            title = { Text("Rename Video") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateVideo(video.copy(title = newTitle))
                    showRenameDialog = false
                    onDismiss()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showRenameDialog = false
                    onDismiss()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePlaylistDialog) {
        var isCustomPlaylist by remember { mutableStateOf(false) }
        SubjectSelectionBottomSheet(
            onSubjectSelected = { subject ->
                if (subject == "Create Custom Playlist") {
                    isCustomPlaylist = true
                } else {
                    onUpdateVideo(video.copy(subject = subject))
                    showChangePlaylistDialog = false
                    onDismiss()
                }
            },
            onDismissRequest = { 
                showChangePlaylistDialog = false
                onDismiss()
            }
        )

        if (isCustomPlaylist) {
            var newSubject by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { 
                    showChangePlaylistDialog = false
                    onDismiss()
                },
                title = { Text("Custom Playlist") },
                text = {
                    OutlinedTextField(
                        value = newSubject,
                        onValueChange = { newSubject = it },
                        label = { Text("Playlist Name") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onUpdateVideo(video.copy(subject = newSubject))
                        showChangePlaylistDialog = false
                        onDismiss()
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showChangePlaylistDialog = false
                        onDismiss()
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
