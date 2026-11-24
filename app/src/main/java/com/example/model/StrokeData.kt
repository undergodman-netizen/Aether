package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class StrokeData(
    val colorValue: ULong,
    val width: Float,
    val isEraser: Boolean = false,
    val points: List<PointData>
)

@Serializable
data class PointData(
    val x: Float,
    val y: Float
)
