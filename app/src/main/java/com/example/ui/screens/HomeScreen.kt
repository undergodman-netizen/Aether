package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VideoEntity
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.GridVideoCard
import com.example.ui.components.HeroVideoCard
import com.example.ui.components.ListVideoCard
import com.example.ui.components.SubjectSelectionBottomSheet
import com.example.ui.components.VideoOptionsBottomSheet
import com.example.viewmodel.VideoViewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VideoViewModel,
    onNavigate: (String) -> Unit,
    onVideoClick: (VideoEntity) -> Unit
) {
    val videos by viewModel.allVideos.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:" + context.packageName)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        }
    }

    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
    var showSubjectSheet by remember { mutableStateOf(false) }
    var isSpeedDialExpanded by remember { mutableStateOf(false) }
    var isListViewMode by remember { mutableStateOf(false) }

    var pendingPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showPdfSubjectSheet by remember { mutableStateOf(false) }

    var videoActionTarget by remember { mutableStateOf<VideoEntity?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingVideoUri = uri
            showSubjectSheet = true
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingPdfUri = uri
            showPdfSubjectSheet = true
        }
    }

    if (showSubjectSheet) {
        SubjectSelectionBottomSheet(
            onSubjectSelected = { subject ->
                pendingVideoUri?.let { uri ->
                    viewModel.addVideo(context, uri, subject)
                }
                showSubjectSheet = false
                pendingVideoUri = null
            },
            onDismissRequest = {
                showSubjectSheet = false
                pendingVideoUri = null
            }
        )
    }

    if (showPdfSubjectSheet) {
        SubjectSelectionBottomSheet(
            onSubjectSelected = { subject ->
                pendingPdfUri?.let { uri ->
                    viewModel.addPdf(context, uri, subject)
                }
                showPdfSubjectSheet = false
                pendingPdfUri = null
            },
            onDismissRequest = {
                showPdfSubjectSheet = false
                pendingPdfUri = null
            }
        )
    }

    if (videoActionTarget != null) {
        VideoOptionsBottomSheet(
            video = videoActionTarget!!,
            onDismiss = { videoActionTarget = null },
            onUpdateVideo = { updatedVideo ->
                viewModel.updateVideo(updatedVideo)
            },
            onDeleteVideo = { video ->
                viewModel.deleteVideo(video.id)
            },
            onViewVideoShots = { video ->
                val encodedUri = Base64.encodeToString(video.uri.toByteArray(StandardCharsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
                onNavigate("videoShots/$encodedUri")
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MEMBER ACCESS",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row {
                            Text(
                                "Aether ",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Light
                            )
                            Text(
                                "Ultra",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isSpeedDialExpanded) {
                    // Small floating button for PDF
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 4.dp
                        ) {
                            Text(
                                "Import PDF",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                isSpeedDialExpanded = false
                                pdfLauncher.launch(arrayOf("application/pdf"))
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Book, contentDescription = "Import PDF")
                        }
                    }

                    // Small floating button for Video
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 4.dp
                        ) {
                            Text(
                                "Add Video",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        FloatingActionButton(
                            onClick = {
                                isSpeedDialExpanded = false
                                launcher.launch(arrayOf("video/*"))
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Add Video")
                        }
                    }
                }

                // Main Obvious Add Action FAB
                FloatingActionButton(
                    onClick = { isSpeedDialExpanded = !isSpeedDialExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.shadow(16.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isSpeedDialExpanded) Icons.Filled.Close else Icons.Default.Add,
                        contentDescription = "Add content",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isSpeedDialExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = { isSpeedDialExpanded = false })
            )
        }

        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "No Study Videos Added Yet",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Import local video lectures or study PDFs to build your medical study library and learn with AI.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { launcher.launch(arrayOf("video/*")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Video")
                            }
                            OutlinedButton(
                                onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import PDF")
                            }
                        }
                    }
                }
            }
        } else {
            val inProgressVideos = videos.filter { it.lastPlayedPosition > 5000 && it.duration > 0 && it.lastPlayedPosition < it.duration * 0.95 }
            val continueWatchingVideo = videos.firstOrNull { it.lastPlayedPosition > 0 && (it.duration == 0L || it.lastPlayedPosition < it.duration - 3000) }
                ?: videos.maxByOrNull { it.timestampAdded }
                ?: videos.first()

            val recentVideos = videos.filter { it.id != continueWatchingVideo.id }

            LazyVerticalGrid(
                columns = if (isListViewMode) GridCells.Fixed(1) else GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp, top = 8.dp)
            ) {
                if (inProgressVideos.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                            Text(
                                text = "CONTINUE WATCHING",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(inProgressVideos) { vid ->
                                    ContinueWatchingCard(
                                        video = vid,
                                        onClick = { onVideoClick(vid) },
                                        onLongClick = { videoActionTarget = vid }
                                    )
                                }
                            }
                        }
                    }
                }

                // Continue Watching section header
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = if (inProgressVideos.isNotEmpty()) "RECENT LECTURES" else "CONTINUE WATCHING",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp, top = if (inProgressVideos.isNotEmpty()) 8.dp else 0.dp)
                    )
                }

                // Continue Watching hero card
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HeroVideoCard(
                        video = continueWatchingVideo,
                        onClick = { onVideoClick(continueWatchingVideo) },
                        onLongClick = { videoActionTarget = continueWatchingVideo }
                    )
                }
                
                if (recentVideos.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "RECENT LECTURES",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isListViewMode = !isListViewMode }) {
                                    Icon(
                                        if (isListViewMode) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                                        contentDescription = "Toggle View",
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                items(recentVideos) { video ->
                    if (isListViewMode) {
                        ListVideoCard(
                            video = video,
                            onClick = { onVideoClick(video) },
                            onLongClick = { videoActionTarget = video }
                        )
                    } else {
                        GridVideoCard(
                            video = video,
                            onClick = { onVideoClick(video) },
                            onLongClick = { videoActionTarget = video }
                        )
                    }
                }
            }
        }
    }
}
