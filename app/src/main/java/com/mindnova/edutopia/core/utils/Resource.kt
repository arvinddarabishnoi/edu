package com.mindnova.edutopia.core.utils

/**
 * Unified result type for data operations.
 * Distinguishes between loading, success, empty, and error states.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Empty<T>(val data: T? = null) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
}
