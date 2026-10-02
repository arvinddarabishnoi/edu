package com.mindnova.edutopia.domain.services

import com.google.gson.Gson
import com.google.gson.JsonElement
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
) {
    val allValid: Boolean get() = globalError == null && totalCount > 0 && invalidCount == 0
}

/**
 * Strict, testable JSON question-array validator.
 *
 * Rules (all violations become explicit item-level errors — nothing is
 * silently skipped):
 *  - input must be a single JSON object or a JSON array of objects
 *  - array items that are primitives/null/arrays are rejected individually
 *  - "question" (or "text") must be present and non-blank
 *  - "options" must contain non-blank string values for A, B, C and D
 *  - "correctAnswer" (or "answer") must be one of A/B/C/D
 *  - "subject" must be Physics, Chemistry or Mathematics (case-insensitive)
 *  - "difficulty" must be Easy, Medium or Hard
 *  - "marks" must be an integer >= 1; "negativeMarks" an integer >= 0
 *  - "questionType" defaults to "Single Correct MCQ"; only that type is supported
 */
object JsonQuestionValidator {

    private val gson = Gson()
    private val OPTION_KEYS = listOf("A", "B", "C", "D")
    private val VALID_SUBJECTS = listOf("Physics", "Chemistry", "Mathematics")
    private val VALID_DIFFICULTIES = listOf("Easy", "Medium", "Hard")
    private const val SUPPORTED_TYPE = "Single Correct MCQ"

    fun validateJson(rawInput: String, defaultTestId: String = ""): JsonValidationResult {
        if (rawInput.isBlank()) {
            return failure(0, "JSON input cannot be empty")
        }

        val parsed: JsonElement = try {
            JsonParser.parseString(rawInput.trim())
        } catch (e: Exception) {
            return failure(0, "Invalid JSON syntax: ${e.localizedMessage}")
        }

        if (parsed.isJsonNull) {
            return failure(0, "JSON input cannot be null")
        }

        val elements: List<Pair<Int, JsonElement>> = when {
            parsed.isJsonArray -> {
                val arr = parsed.asJsonArray
                (0 until arr.size()).map { it + 1 to arr.get(it) }
            }
            parsed.isJsonObject -> listOf(1 to parsed)
            else -> return failure(0, "Expected a JSON object or an array of question objects.")
        }

        if (elements.isEmpty()) {
            return failure(0, "The JSON array is empty.")
        }

        val items = elements.map { (number, element) ->
            validateItem(number, element, defaultTestId)
        }

        return JsonValidationResult(
            totalCount = items.size,
            validCount = items.count { it.isValid },
            invalidCount = items.count { !it.isValid },
            items = items,
            globalError = null
        )
    }

    private fun failure(total: Int, message: String) = JsonValidationResult(
        totalCount = total,
        validCount = 0,
        invalidCount = total,
        items = emptyList(),
        globalError = message
    )

    private fun validateItem(index: Int, element: JsonElement, defaultTestId: String): QuestionValidationItem {
        val raw = runCatching { gson.toJson(element) }.getOrDefault("null")

        if (element.isJsonNull) {
            return QuestionValidationItem(index, false, null, listOf("Item #$index is null; expected a question object."), raw)
        }
        if (!element.isJsonObject) {
            return QuestionValidationItem(
                index, false, null,
                listOf("Item #$index is not a JSON object (found ${element.javaClass.simpleName.replace("Json", "")}); every entry must be an object."),
                raw
            )
        }
        val obj = element.asJsonObject
        val errors = mutableListOf<String>()

        // 1. Question text
        val questionText = obj.optString("question") ?: obj.optString("text") ?: ""
        if (questionText.isBlank()) {
            errors.add("Missing or empty 'question' text")
        }

        // 2. Options — must exist with all four non-blank string values
        val optionsMap = mutableMapOf<String, String>()
        if (obj.has("options") && obj.get("options").isJsonObject) {
            val optsObj = obj.getAsJsonObject("options")
            if (optsObj.keySet().size != OPTION_KEYS.size) {
                val extra = optsObj.keySet().map { it.uppercase() }.filter { it !in OPTION_KEYS }
                if (extra.isNotEmpty()) {
                    errors.add("Unexpected option keys: ${extra.joinToString()}; only A/B/C/D are supported")
                }
            }
            for (key in OPTION_KEYS) {
                val direct = optsObj.get(key) ?: optsObj.get(key.lowercase())
                when {
                    direct == null -> errors.add("Option $key is missing")
                    direct.isJsonNull -> errors.add("Option $key is null")
                    !direct.isJsonPrimitive || !direct.asJsonPrimitive.isString ->
                        errors.add("Option $key must be a string")
                    direct.asString.isBlank() -> errors.add("Option $key is blank")
                    else -> optionsMap[key] = direct.asString.trim()
                }
            }
        } else if (obj.has("options") && obj.get("options").isJsonArray) {
            val arr = obj.getAsJsonArray("options")
            if (arr.size() < OPTION_KEYS.size) {
                errors.add("Options array must contain at least 4 entries (A-D)")
            } else {
                for (i in 0 until 4) {
                    val elem = arr.get(i)
                    if (elem.isJsonPrimitive && elem.asJsonPrimitive.isString && elem.asString.isNotBlank()) {
                        optionsMap[OPTION_KEYS[i]] = elem.asString.trim()
                    } else {
                        errors.add("Option ${OPTION_KEYS[i]} must be a non-blank string")
                    }
                }
            }
        } else {
            errors.add("Missing 'options' object with keys A, B, C, D")
        }

        // 3. Correct answer
        val correctAnswer = (obj.optString("correctAnswer") ?: obj.optString("answer") ?: "")
            .trim().uppercase()
        if (correctAnswer.length == 1 && correctAnswer[0] in 'A'..'D') {
            // ok
        } else {
            errors.add("Invalid 'correctAnswer': must be 'A', 'B', 'C', or 'D'")
        }

        // 4. Subject — may be omitted (defaults to Physics) but an invalid value is rejected
        val rawSubject = obj.optString("subject")?.trim().orEmpty()
        val subject = when {
            rawSubject.isEmpty() -> "Physics"
            else -> VALID_SUBJECTS.firstOrNull { it.equals(rawSubject, ignoreCase = true) }
        }
        if (subject == null) {
            errors.add("Invalid 'subject': must be Physics, Chemistry or Mathematics")
        }

        // 5. Difficulty
        val rawDifficulty = obj.optString("difficulty")?.trim().orEmpty()
        val difficulty = VALID_DIFFICULTIES.firstOrNull { it.equals(rawDifficulty, ignoreCase = true) }
            ?: run {
                if (rawDifficulty.isEmpty()) {
                    "Medium" // default when absent
                } else {
                    errors.add("Invalid 'difficulty': must be Easy, Medium or Hard")
                    null
                }
            }

        // 6. Marks
        val marks = readInt(obj, "marks", default = 4, min = 1, max = 20, field = "'marks'", errors = errors)
        val negativeMarks = readInt(obj, "negativeMarks", default = 1, min = 0, max = 10, field = "'negativeMarks'", errors = errors)
        if (marks != null && negativeMarks != null && negativeMarks >= marks) {
            errors.add("'negativeMarks' must be smaller than 'marks'")
        }

        // 7. Question type
        val questionType = obj.optString("questionType")?.trim().orEmpty()
        if (questionType.isNotEmpty() && !questionType.equals(SUPPORTED_TYPE, ignoreCase = true)) {
            errors.add("Unsupported 'questionType' \"$questionType\": only \"$SUPPORTED_TYPE\" is supported")
        }

        // 8. Optional textual fields must be strings when present
        val explanation = obj.optString("explanation").orEmpty()
        val chapter = obj.optString("chapter").orEmpty()
        val topic = obj.optString("topic").orEmpty()

        val isValid = errors.isEmpty()
        val question = if (isValid && subject != null && difficulty != null && marks != null && negativeMarks != null) {
            Question(
                id = obj.optString("id")?.trim().orEmpty(),
                testId = defaultTestId,
                questionNumber = index,
                questionText = questionText.trim(),
                options = optionsMap,
                correctAnswer = correctAnswer,
                explanation = explanation,
                subject = subject,
                chapter = chapter,
                topic = topic,
                difficulty = difficulty,
                questionType = SUPPORTED_TYPE,
                marks = marks,
                negativeMarks = negativeMarks,
                order = index
            )
        } else {
            null
        }

        return QuestionValidationItem(
            index = index,
            isValid = isValid && question != null,
            question = question,
            errors = errors.toList(),
            rawJson = raw
        )
    }

    /** Returns trimmed string or null when absent/non-string (non-string is an error). */
    private fun JsonObject.optString(key: String): String? {
        if (!has(key)) return null
        val el = get(key)
        if (el.isJsonNull) return null
        return if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
            el.asString
        } else {
            null
        }
    }

    private fun readInt(
        obj: JsonObject,
        key: String,
        default: Int,
        min: Int,
        max: Int,
        field: String,
        errors: MutableList<String>
    ): Int? {
        if (!obj.has(key) || obj.get(key).isJsonNull) return default
        val el = obj.get(key)
        return try {
            if (!el.isJsonPrimitive || !el.asJsonPrimitive.isNumber) {
                errors.add("$field must be a whole number between $min and $max")
                null
            } else {
                val d = el.asDouble
                if (d != d.toLong().toDouble()) {
                    errors.add("$field must be a whole number")
                    null
                } else {
                    val v = d.toInt()
                    if (v < min || v > max) {
                        errors.add("$field must be between $min and $max")
                        null
                    } else {
                        v
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("$field must be a whole number between $min and $max")
            null
        }
    }
}
