package com.mindnova.edutopia.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class BannerItem(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val clickUrl: String = "",
    val active: Boolean = true,
    val priority: Int = 1,
    val startDate: Long = 0L,
    val endDate: Long = Long.MAX_VALUE,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val actionUrl: String = "",
    val targetAudience: String = "all", // "all", "Class 11", "Class 12", "Dropper", or batchId
    val priority: String = "normal", // "high", "normal", "low"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
