package com.example.navigation

import kotlinx.serialization.Serializable

@Serializable
object Home

@Serializable
object Library

@Serializable
object Subjects

@Serializable
data class SubjectDetail(val subjectName: String)

@Serializable
data class PdfViewer(val encodedUrl: String)

@Serializable
data class Player(val encodedUri: String, val startPosition: Long = -1L)

@Serializable
data class VideoShots(val encodedUri: String)
