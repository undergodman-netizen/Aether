package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixNormal
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnnotationConfig
import com.example.model.AnnotationTool

@Composable
fun AnnotationToolbar(
    config: AnnotationConfig,
    onConfigChange: (AnnotationConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!config.isEnabled) return

    val colors = remember {
        listOf(
            Color(0xFFFF5252), // Red
            Color(0xFF448AFF), // Blue
            Color(0xFF69F0AE), // Green
            Color(0xFFFFD740), // Yellow
            Color(0xFFE0E0E0), // White/Light
            Color(0xFF212121)  // Dark
        )
    }

    var showStrokeSlider by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E24).copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shadowElevation = 8.dp,
            modifier = Modifier.wrapContentWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tool Toggle Buttons: Pen, Highlighter, Eraser
                ToolIconButton(
                    selected = config.tool == AnnotationTool.PEN,
                    icon = Icons.Default.Create,
                    contentDescription = "Pen",
                    onClick = {
                        val defaultWidth = if (config.strokeWidth > 16f) 6f else config.strokeWidth
                        onConfigChange(config.copy(tool = AnnotationTool.PEN, strokeWidth = defaultWidth))
                    }
                )

                ToolIconButton(
                    selected = config.tool == AnnotationTool.HIGHLIGHTER,
                    icon = Icons.Default.Edit,
                    contentDescription = "Highlighter",
                    onClick = {
                        val defaultWidth = if (config.strokeWidth < 12f) 20f else config.strokeWidth
                        onConfigChange(config.copy(tool = AnnotationTool.HIGHLIGHTER, strokeWidth = defaultWidth))
                    }
                )

                ToolIconButton(
                    selected = config.tool == AnnotationTool.ERASER,
                    icon = Icons.Default.AutoFixNormal,
                    contentDescription = "Eraser",
                    onClick = {
                        val defaultWidth = if (config.strokeWidth < 16f) 28f else config.strokeWidth
                        onConfigChange(config.copy(tool = AnnotationTool.ERASER, strokeWidth = defaultWidth))
                    }
                )

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.15f)
                )

                // Color Swatches (if not Eraser)
                if (config.tool != AnnotationTool.ERASER) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { color ->
                            val isSelected = config.color.value == color.value
                            val displayColor = if (config.tool == AnnotationTool.HIGHLIGHTER) color.copy(alpha = 0.6f) else color
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(displayColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onConfigChange(config.copy(color = color)) }
                            )
                        }
                    }

                    VerticalDivider(
                        modifier = Modifier
                            .height(20.dp)
                            .width(1.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    )
                }

                // Stroke Weight Button (Click toggles subtle slider below)
                IconButton(
                    onClick = { showStrokeSlider = !showStrokeSlider },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (showStrokeSlider) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                ) {
                    val dotSize = (config.strokeWidth.coerceIn(4f, 28f) / 2.5f).dp
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(
                                if (config.tool == AnnotationTool.ERASER) Color.Gray
                                else if (config.tool == AnnotationTool.HIGHLIGHTER) config.color.copy(alpha = 0.6f)
                                else config.color
                            )
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .width(1.dp),
                    color = Color.White.copy(alpha = 0.15f)
                )

                // Undo / Redo / Clear
                IconButton(
                    onClick = { onConfigChange(config.copy(undoTrigger = config.undoTrigger + 1)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onConfigChange(config.copy(redoTrigger = config.redoTrigger + 1)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onConfigChange(config.copy(clearTrigger = config.clearTrigger + 1)) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expandable Stroke Width Slider popup
        AnimatedVisibility(
            visible = showStrokeSlider,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E24).copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .width(220.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Size",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = config.strokeWidth,
                        onValueChange = { onConfigChange(config.copy(strokeWidth = it)) },
                        valueRange = 2f..44f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
        label = "bgColor"
    )
    val tintColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
        label = "tintColor"
    )

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
