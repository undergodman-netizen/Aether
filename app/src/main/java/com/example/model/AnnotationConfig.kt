package com.example.model

import androidx.compose.ui.graphics.Color

enum class AnnotationTool {
    PEN, HIGHLIGHTER, ERASER
}

data class AnnotationConfig(
    val isEnabled: Boolean = false,
    val tool: AnnotationTool = AnnotationTool.PEN,
    val color: Color = Color.Red,
    val strokeWidth: Float = 8f,
    val undoTrigger: Int = 0,
    val redoTrigger: Int = 0,
    val clearTrigger: Int = 0
)
