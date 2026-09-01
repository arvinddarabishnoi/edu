package com.mindnova.edutopia.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Batch(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val bannerUrl: String = "",
    val startDate: Long = 0L,
    val endDate: Long = Long.MAX_VALUE,
    val targetClass: String = "Class 12", // "Class 11", "Class 12", "Dropper", "All"
    val accessType: String = "Free", // "Free", "Invite Only", "Premium"
    val joinCode: String = "",
    val studentCount: Long = 0L,
    val status: String = "active", // "active", "completed", "upcoming"
    val createdAt: Long = System.currentTimeMillis()
)
