package com.mindnova.edutopia.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Series(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val subject: String = "", // "Physics", "Chemistry", "Mathematics"
    val chapter: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val order: Int = 1,
    val lectureCount: Int = 0,
    val targetClass: String = "All", // "Class 11", "Class 12", "Dropper", "All"
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Lecture(
    @DocumentId
    val id: String = "",
    val seriesId: String = "",
    val title: String = "",
    val subject: String = "",
    val chapter: String = "",
    val seriesName: String = "",
    val description: String = "",
    val aboutVideo: String = "",
    val videoUrl: String = "",
    val videoId: String = "",
    val thumbnailUrl: String = "",
    val lectureNumber: Int = 1,
    val duration: String = "45 mins",
    val durationSeconds: Long = 2700L,
    val publishStatus: String = "published", // "published", "draft"
    val publishDate: Long = System.currentTimeMillis(),
    val order: Int = 1,
    val views: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)
