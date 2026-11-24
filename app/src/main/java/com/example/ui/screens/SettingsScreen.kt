package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("StudyMemory", Context.MODE_PRIVATE) }
    
    var apiKey by remember { mutableStateOf(sharedPreferences.getString("gemini_api_key", "") ?: "") }
    
    val models = listOf(
        Pair("models/gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite (Default & Fast)"),
        Pair("models/gemini-3.8-flash", "Gemini 3.8 Flash")
    )
    var selectedModel by remember { 
        mutableStateOf(sharedPreferences.getString("gemini_model", "models/gemini-3.5-flash-lite") ?: "models/gemini-3.5-flash-lite") 
    }
    var expanded by remember { mutableStateOf(false) }
    
    var customPrompt by remember {
        mutableStateOf(sharedPreferences.getString("gemini_custom_prompt", "") ?: "")
    }
    var aiCollapsibleExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SETTINGS",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section heading
            Text(
                text = "APPEARANCE",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val newValue = !ThemeConfig.isDarkTheme
                        ThemeConfig.isDarkTheme = newValue
                        sharedPreferences.edit().putBoolean("dark_theme", newValue).apply()
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Dark Theme",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = "Switch between dynamic dark aesthetics and high contrast light mode.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Switch(
                        checked = ThemeConfig.isDarkTheme,
                        onCheckedChange = { newValue ->
                            ThemeConfig.isDarkTheme = newValue
                            sharedPreferences.edit().putBoolean("dark_theme", newValue).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            var notificationsEnabled by remember {
                mutableStateOf(sharedPreferences.getBoolean("notifications_enabled", false))
            }
            val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    notificationsEnabled = true
                    sharedPreferences.edit().putBoolean("notifications_enabled", true).apply()
                    val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.ContinueWatchingWorker>(8, java.util.concurrent.TimeUnit.HOURS)
                        .build()
                    androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        "ContinueWatchingWork",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        val newValue = !notificationsEnabled
                        if (newValue) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationsEnabled = true
                                sharedPreferences.edit().putBoolean("notifications_enabled", true).apply()
                                val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.ContinueWatchingWorker>(8, java.util.concurrent.TimeUnit.HOURS)
                                    .build()
                                androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                    "ContinueWatchingWork",
                                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                                    workRequest
                                )
                            }
                        } else {
                            notificationsEnabled = false
                            sharedPreferences.edit().putBoolean("notifications_enabled", false).apply()
                            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("ContinueWatchingWork")
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Continue Watching Reminders",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = "Receive notifications to pick up unfinished videos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationsEnabled = true
                                    sharedPreferences.edit().putBoolean("notifications_enabled", true).apply()
                                    val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.worker.ContinueWatchingWorker>(8, java.util.concurrent.TimeUnit.HOURS)
                                        .build()
                                    androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                        "ContinueWatchingWork",
                                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                                        workRequest
                                    )
                                }
                            } else {
                                notificationsEnabled = false
                                sharedPreferences.edit().putBoolean("notifications_enabled", false).apply()
                                androidx.work.WorkManager.getInstance(context).cancelUniqueWork("ContinueWatchingWork")
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // AI Integration Section heading
            Text(
                text = "AI INTEGRATION",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // Gemini Settings Card (Collapsible)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header row acting as expansion toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { aiCollapsibleExpanded = !aiCollapsibleExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Gemini AI Settings",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp
                            )
                            val modelDisplayName = models.find { it.first == selectedModel }?.second ?: "Gemini 3.5 Flash-Lite"
                            val promptStatus = if (customPrompt.isNotBlank()) "Custom Prompt" else "Default Prompt"
                            Text(
                                text = "$modelDisplayName • $promptStatus",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Icon(
                            imageVector = if (aiCollapsibleExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = if (aiCollapsibleExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (aiCollapsibleExpanded) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 1. API Key
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Gemini API Key",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Uses auto-injected AI Studio system key by default for seamless access. Enter an optional custom API key below to override.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                                
                                OutlinedTextField(
                                    value = apiKey,
                                    onValueChange = { 
                                        apiKey = it
                                        sharedPreferences.edit().putString("gemini_api_key", it).apply()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Auto-injected AI Studio Key Active (or paste AIzaSy...)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.VpnKey, contentDescription = "API Key", tint = MaterialTheme.colorScheme.primary)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            // 2. Model Choice
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "AI Model",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Choose the Gemini model that powers the play-along chat tutor.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedCard(
                                        onClick = { expanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = models.find { it.first == selectedModel }?.second ?: "Gemini 3.5 Flash-Lite",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Icon(
                                                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Expand",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        models.forEach { (modelId, modelName) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = modelName,
                                                        fontSize = 14.sp,
                                                        fontWeight = if (selectedModel == modelId) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedModel == modelId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    selectedModel = modelId
                                                    sharedPreferences.edit().putString("gemini_model", modelId).apply()
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            // 3. Prompt Option
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI System Prompt",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (customPrompt.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                customPrompt = ""
                                                sharedPreferences.edit().remove("gemini_custom_prompt").apply()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Reset", fontSize = 12.sp)
                                        }
                                    }
                                }
                                Text(
                                    text = "Customize the background instruction set sent to Gemini. Leave blank to use primary medical tutor defaults.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )

                                OutlinedTextField(
                                    value = customPrompt,
                                    onValueChange = {
                                        customPrompt = it
                                        sharedPreferences.edit().putString("gemini_custom_prompt", it).apply()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp, max = 200.dp),
                                    placeholder = { 
                                        Text(
                                            text = "e.g., You are a helpful AI tutor for medical/MBBS students...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            fontSize = 13.sp
                                        ) 
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    maxLines = 6,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
