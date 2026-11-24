package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.VideoEntity
import com.example.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: VideoViewModel,
    onBack: () -> Unit,
    onVideoClick: (VideoEntity) -> Unit
) {
    val context = LocalContext.current
    val videos by viewModel.allVideos.collectAsState()
    val sharedPreferences = remember { context.getSharedPreferences("StudyMemory", Context.MODE_PRIVATE) }
    
    var historyList by remember { mutableStateOf(emptyList<VideoEntity>()) }
    
    // Read history list dynamically
    LaunchedEffect(videos) {
        val historyString = sharedPreferences.getString("history", "") ?: ""
        val uris = if (historyString.startsWith("[")) {
            try {
                val jsonArray = org.json.JSONArray(historyString)
                List(jsonArray.length()) { jsonArray.getString(it) }
            } catch (e: Exception) {
                historyString.split(",").filter { it.isNotEmpty() }
            }
        } else {
            historyString.split(",").filter { it.isNotEmpty() }
        }
        val mappedVideos = uris.mapNotNull { uri ->
            videos.find { it.uri == uri }
        }
        historyList = mappedVideos
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent History", fontSize = 16.sp, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = {
                            sharedPreferences.edit().remove("history").apply()
                            historyList = emptyList()
                        }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No watched videos yet.\nYour study sessions will appear here.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(historyList) { video ->
                    HistoryVideoItem(
                        video = video,
                        context = context,
                        sharedPreferences = sharedPreferences,
                        onClick = { onVideoClick(video) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryVideoItem(
    video: VideoEntity,
    context: Context,
    sharedPreferences: android.content.SharedPreferences,
    onClick: () -> Unit
) {
    val savedPosition = remember(video.uri) { sharedPreferences.getLong("video_${video.uri}", 0L) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 110.dp, height = 66.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(android.net.Uri.parse(video.uri))
                        .crossfade(true)
                        .build(),
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                )
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = video.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Normal
                )
                
                if (savedPosition > 0L) {
                    val progressText = formatPosition(savedPosition)
                    Text(
                        text = "Paused at $progressText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun formatPosition(millis: Long): String {
    val hrs = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(millis)
    val mins = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val secs = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
