package com.mindnova.edutopia.data.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class PyqQuestion(
    @DocumentId
    val id: String = "",
    val examType: String = "JEE Main", // "JEE Main", "JEE Advanced"
    val year: Int = 2024,
    val shift: String = "Shift 1",
    val subject: String = "Physics", // "Physics", "Chemistry", "Mathematics"
    val chapter: String = "Electrostatics",
    val topic: String = "Electric Field and Potential",
    val difficulty: String = "Medium", // "Easy", "Medium", "Hard"
    val questionText: String = "",
    val options: Map<String, String> = mapOf("A" to "", "B" to "", "C" to "", "D" to ""),
    val correctAnswer: String = "A",
    val explanation: String = "",
    val marks: Int = 4,
    val negativeMarks: Int = 1,
    val solvedCount: Long = 0L,
    val accuracy: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
