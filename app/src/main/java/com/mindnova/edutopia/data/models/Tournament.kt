package com.mindnova.edutopia.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Tournament(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val bannerUrl: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationMinutes: Int = 90,
    val totalQuestions: Int = 30,
    val entryRequirements: String = "Open for all JEE Aspirants",
    val prizePoolPoints: Long = 10000L,
    val xpReward: Long = 150L,
    val status: String = "upcoming", // "upcoming", "live", "ended"
    val participantCount: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class TournamentParticipant(
    @DocumentId
    val id: String = "",
    val tournamentId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userClass: String = "",
    val score: Int = 0,
    val rank: Int = 1,
    val timeTakenSeconds: Long = 0L,
    val submittedAt: Long = 0L
)
