package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoEntity
import com.example.data.PdfEntity
import com.example.viewmodel.VideoViewModel
import com.example.ui.components.PdfRowCard
import com.example.ui.components.GridVideoCard

import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailScreen(
    subject: String,
    viewModel: VideoViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    onVideoClick: (VideoEntity) -> Unit,
    onPdfClick: (PdfEntity) -> Unit
) {
    BackHandler { onBack() }
    val videos by viewModel.getVideosBySubject(subject).collectAsState()
    val pdfs by viewModel.getPdfsBySubject(subject).collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Videos, 1 = PDFs

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addVideo(context, it, subject) }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addPdf(context, it, subject) }
    }

    var videoActionTarget by remember { mutableStateOf<VideoEntity?>(null) }

    if (videoActionTarget != null) {
        com.example.ui.components.VideoOptionsBottomSheet(
            video = videoActionTarget!!,
            onDismiss = { videoActionTarget = null },
            onUpdateVideo = { updatedVideo ->
                viewModel.updateVideo(updatedVideo)
            },
            onDeleteVideo = { video ->
                viewModel.deleteVideo(video.id)
            },
            onViewVideoShots = { video ->
                val encodedUri = android.util.Base64.encodeToString(video.uri.toByteArray(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                onNavigate("videoShots/$encodedUri")
            }
        )
    }

    var pdfActionTarget by remember { mutableStateOf<PdfEntity?>(null) }
    var showPdfRenameDialog by remember { mutableStateOf(false) }
    var showPdfChangePlaylistDialog by remember { mutableStateOf(false) }

    if (pdfActionTarget != null && !showPdfRenameDialog && !showPdfChangePlaylistDialog) {
        ModalBottomSheet(onDismissRequest = { pdfActionTarget = null }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = "PDF OPTIONS",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showPdfRenameDialog = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename PDF")
                    Spacer(Modifier.width(16.dp))
                    Text("Rename PDF")
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showPdfChangePlaylistDialog = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Book, contentDescription = "Change Playlist")
                    Spacer(Modifier.width(16.dp))
                    Text("Change Playlist / Subject")
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.deletePdf(pdfActionTarget!!.id)
                        pdfActionTarget = null
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete PDF", tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Text("Delete PDF", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showPdfRenameDialog && pdfActionTarget != null) {
        var newTitle by remember { mutableStateOf(pdfActionTarget!!.title) }
        AlertDialog(
            onDismissRequest = { showPdfRenameDialog = false; pdfActionTarget = null },
            title = { Text("Rename PDF") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updatePdf(pdfActionTarget!!.copy(title = newTitle))
                    showPdfRenameDialog = false
                    pdfActionTarget = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfRenameDialog = false; pdfActionTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPdfChangePlaylistDialog && pdfActionTarget != null) {
        var isCustomPlaylist by remember { mutableStateOf(false) }
        com.example.ui.components.SubjectSelectionBottomSheet(
            onSubjectSelected = { selectedSubject ->
                if (selectedSubject == "Create Custom Playlist") {
                    isCustomPlaylist = true
                } else {
                    viewModel.updatePdf(pdfActionTarget!!.copy(subject = selectedSubject))
                    showPdfChangePlaylistDialog = false
                    pdfActionTarget = null
                }
            },
            onDismissRequest = { showPdfChangePlaylistDialog = false; pdfActionTarget = null }
        )

        if (isCustomPlaylist) {
            var newSubject by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showPdfChangePlaylistDialog = false; pdfActionTarget = null },
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
                        viewModel.updatePdf(pdfActionTarget!!.copy(subject = newSubject))
                        showPdfChangePlaylistDialog = false
                        pdfActionTarget = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPdfChangePlaylistDialog = false; pdfActionTarget = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(subject, fontSize = 16.sp, fontWeight = FontWeight.Medium) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("VIDEOS (${videos.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("PDF NOTES (${pdfs.size})", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        launcher.launch(arrayOf("video/*"))
                    } else {
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(16.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "Add Video to $subject" else "Add PDF to $subject",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedTab == 0) {
                if (videos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No videos for $subject.\nTap + to add local tutorials.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp, top = 16.dp)
                    ) {
                        items(videos) { video ->
                            GridVideoCard(
                                video = video,
                                onClick = { onVideoClick(video) },
                                onLongClick = { videoActionTarget = video }
                            )
                        }
                    }
                }
            } else {
                if (pdfs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No notes for $subject.\nTap + to import professional PDF files.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pdfs) { pdf ->
                            PdfRowCard(
                                pdf = pdf,
                                onClick = { onPdfClick(pdf) },
                                onLongClick = { pdfActionTarget = pdf }
                            )
                        }
                    }
                }
            }
        }
    }

}
