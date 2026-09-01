package com.mindnova.edutopia.domain.services

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mindnova.edutopia.data.models.Question

data class QuestionValidationItem(
    val index: Int,
    val isValid: Boolean,
    val question: Question? = null,
    val errors: List<String> = emptyList(),
    val rawJson: String = ""
)

data class JsonValidationResult(
    val totalCount: Int,
    val validCount: Int,
    val invalidCount: Int,
    val items: List<QuestionValidationItem>,
    val globalError: String? = null
)

object JsonQuestionValidator {

    private val gson = Gson()

    /**
     * Validates JSON input that is either a JSON array of questions or a single question object.
     */
    fun validateJson(rawInput: String, defaultTestId: String = ""): JsonValidationResult {
        if (rawInput.isBlank()) {
            return JsonValidationResult(
                totalCount = 0,
                validCount = 0,
                invalidCount = 0,
                items = emptyList(),
                globalError = "JSON input cannot be empty"
            )
        }

        try {
            val parsedElement = JsonParser.parseString(rawInput.trim())
            val questionObjects = mutableListOf<JsonObject>()

            if (parsedElement.isJsonArray) {
                val array = parsedElement.asJsonArray
                for (i in 0 until array.size()) {
                    val elem = array.get(i)
                    if (elem.isJsonObject) {
                        questionObjects.add(elem.asJsonObject)
                    }
                }
            } else if (parsedElement.isJsonObject) {
                questionObjects.add(parsedElement.asJsonObject)
            } else {
                return JsonValidationResult(
                    totalCount = 0,
                    validCount = 0,
                    invalidCount = 0,
                    items = emptyList(),
                    globalError = "Expected a JSON object or array of question objects."
                )
            }

            val items = mutableListOf<QuestionValidationItem>()

            for ((index, obj) in questionObjects.withIndex()) {
                val errors = mutableListOf<String>()

                // 1. Question text
                val questionText = if (obj.has("question")) obj.get("question").asString.trim() else ""
                if (questionText.isBlank()) {
                    errors.add("Missing or empty 'question' text")
                }

                // 2. Options
                val optionsMap = mutableMapOf<String, String>()
                if (obj.has("options") && obj.get("options").isJsonObject) {
                    val optsObj = obj.getAsJsonObject("options")
                    for (key in listOf("A", "B", "C", "D")) {
                        if (optsObj.has(key)) {
                            optionsMap[key] = optsObj.get(key).asString.trim()
                        } else if (optsObj.has(key.lowercase())) {
                            optionsMap[key] = optsObj.get(key.lowercase()).asString.trim()
                        } else {
                            errors.add("Option $key is missing")
                        }
                    }
                } else {
                    errors.add("Missing 'options' object with keys A, B, C, D")
                }

                // 3. Correct Answer
                val correctAnswer = if (obj.has("correctAnswer")) {
                    obj.get("correctAnswer").asString.trim().uppercase()
                } else if (obj.has("answer")) {
                    obj.get("answer").asString.trim().uppercase()
                } else {
                    ""
                }
                if (correctAnswer !in listOf("A", "B", "C", "D")) {
                    errors.add("Invalid 'correctAnswer': must be 'A', 'B', 'C', or 'D'")
                }

                // 4. Subject & details
                val subject = if (obj.has("subject")) obj.get("subject").asString.trim() else "Physics"
                val chapter = if (obj.has("chapter")) obj.get("chapter").asString.trim() else ""
                val topic = if (obj.has("topic")) obj.get("topic").asString.trim() else ""
                val difficulty = if (obj.has("difficulty")) obj.get("difficulty").asString.trim() else "Medium"
                val explanation = if (obj.has("explanation")) obj.get("explanation").asString.trim() else ""
                val marks = if (obj.has("marks")) obj.get("marks").asInt else 4
                val negativeMarks = if (obj.has("negativeMarks")) obj.get("negativeMarks").asInt else 1

                val isValid = errors.isEmpty()
                val question = if (isValid) {
                    Question(
                        id = "",
                        testId = defaultTestId,
                        questionNumber = index + 1,
                        questionText = questionText,
                        options = optionsMap,
                        correctAnswer = correctAnswer,
                        explanation = explanation,
                        subject = subject,
                        chapter = chapter,
                        topic = topic,
                        difficulty = difficulty,
                        marks = marks,
                        negativeMarks = negativeMarks,
                        order = index + 1
                    )
                } else null

                items.add(
                    QuestionValidationItem(
                        index = index + 1,
                        isValid = isValid,
                        question = question,
                        errors = errors,
                        rawJson = gson.toJson(obj)
                    )
                )
            }

            return JsonValidationResult(
                totalCount = items.size,
                validCount = items.count { it.isValid },
                invalidCount = items.count { !it.isValid },
                items = items,
                globalError = null
            )
        } catch (e: Exception) {
            return JsonValidationResult(
                totalCount = 0,
                validCount = 0,
                invalidCount = 0,
                items = emptyList(),
                globalError = "Invalid JSON Syntax: ${e.localizedMessage}"
            )
        }
    }
}
