package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Constants

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectListScreen(viewModel: VideoViewModel, onBack: () -> Unit, onNavigateToSubject: (String) -> Unit) {
    val videos by viewModel.allVideos.collectAsState()
    val dynamicSubjects = videos.map { it.subject }.distinct()
    val allCombined = (Constants.neetPgSubjects + dynamicSubjects).distinct()

    val preClinical = listOf("Anatomy", "Physiology", "Biochemistry")
    val paraClinical = listOf("Pathology", "Pharmacology", "Microbiology", "Forensic Medicine")

    val preClinicalList = allCombined.filter { it in preClinical }
    val paraClinicalList = allCombined.filter { it in paraClinical }
    val clinicalList = allCombined.filter { it !in preClinical && it !in paraClinical }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Subjects & Playlists", 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
        ) {
            if (preClinicalList.isNotEmpty()) {
                item {
                    CategoryHeader("Pre-Clinical")
                }
                items(preClinicalList) { subject ->
                    SubjectListItem(
                        subject = subject,
                        onClick = { onNavigateToSubject(subject) }
                    )
                }
            }

            if (paraClinicalList.isNotEmpty()) {
                item {
                    CategoryHeader("Para-Clinical")
                }
                items(paraClinicalList) { subject ->
                    SubjectListItem(
                        subject = subject,
                        onClick = { onNavigateToSubject(subject) }
                    )
                }
            }

            if (clinicalList.isNotEmpty()) {
                item {
                    CategoryHeader("Clinical")
                }
                items(clinicalList) { subject ->
                    SubjectListItem(
                        subject = subject,
                        onClick = { onNavigateToSubject(subject) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
            Text(
                text = title.uppercase(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    }
}

@Composable
fun SubjectListItem(subject: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Text(
                    text = subject,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
