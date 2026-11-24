package com.example.ui.screens

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AnnotationConfig
import com.example.model.AnnotationTool
import com.example.ui.components.PdfViewer
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    encodedUrl: String,
    onBack: () -> Unit
) {
    val decodedBytes = Base64.decode(encodedUrl, Base64.URL_SAFE or Base64.NO_WRAP)
    val url = String(decodedBytes, StandardCharsets.UTF_8)

    var config by remember { mutableStateOf(AnnotationConfig()) }
    var scale by remember { mutableFloatStateOf(1f) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("PDF Viewer") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { config = config.copy(isEnabled = !config.isEnabled) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (config.isEnabled) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Annotate")
                        }
                    }
                )
                if (config.isEnabled) {
                    com.example.ui.components.AnnotationToolbar(
                        config = config,
                        onConfigChange = { config = it }
                    )
                }
            }
        }
    ) { padding ->
        PdfViewer(
            url = url,
            config = config,
            scale = scale,
            onScaleChange = { scale = it },
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
