package com.mindnova.edutopia.core.utils

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first

/**
 * Firestore snapshot listeners that surface Loading / Success / Empty / Error as
 * distinct states. Errors are NEVER collapsed into empty data.
 */

/**
 * Streams a list query as [Resource]. The [map] function converts a document
 * into the domain model (documents failing conversion are dropped, but a
 * completely empty snapshot yields [Resource.Empty]).
 */
fun <T> Query.listResourceFlow(map: (DocumentSnapshot) -> T?): Flow<Resource<List<T>>> = callbackFlow {
    trySend(Resource.Loading)
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            trySend(Resource.Error(describeFirestoreError(error), error))
            return@addSnapshotListener
        }
        if (snapshot == null) {
            trySend(Resource.Error("Firestore returned no snapshot."))
            return@addSnapshotListener
        }
        if (snapshot.isEmpty) {
            trySend(Resource.Empty())
            return@addSnapshotListener
        }
        trySend(Resource.Success(snapshot.documents.mapNotNull(map)))
    }
    awaitClose { registration.remove() }
}

/**
 * Streams a single document as [Resource]. A non-existent document is
 * [Resource.Empty]; a listener failure is [Resource.Error].
 */
fun <T> DocumentReference.docResourceFlow(map: (DocumentSnapshot) -> T?): Flow<Resource<T>> = callbackFlow {
    trySend(Resource.Loading)
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            trySend(Resource.Error(describeFirestoreError(error), error))
            return@addSnapshotListener
        }
        if (snapshot == null || !snapshot.exists()) {
            trySend(Resource.Empty())
            return@addSnapshotListener
        }
        val value = map(snapshot)
        if (value == null) {
            trySend(Resource.Error("Document ${snapshot.id} exists but could not be parsed."))
        } else {
            trySend(Resource.Success(value))
        }
    }
    awaitClose { registration.remove() }
}

/**
 * Produces actionable error text. Missing composite indexes in particular
 * must never look like "no data" — the developer needs the index hint.
 */
fun describeFirestoreError(error: Throwable): String {
    val raw = error.message ?: "Unknown Firestore error"
    return when {
        raw.contains("FAILED_PRECONDITION", ignoreCase = true) ||
            raw.contains("requires an index", ignoreCase = true) ->
            "Missing Firestore composite index. Deploy firestore.indexes.json via the Firebase CLI " +
                "or create the index from the link in the raw error: $raw"
        raw.contains("PERMISSION_DENIED", ignoreCase = true) ->
            "Permission denied by Firestore security rules. Check that you are signed in with an " +
                "account that has access, and that firestore.rules allows this read. ($raw)"
        raw.contains("UNAUTHENTICATED", ignoreCase = true) ->
            "Your session expired. Please sign in again. ($raw)"
        raw.contains("UNAVAILABLE", ignoreCase = true) ||
            raw.contains("network", ignoreCase = true) ->
            "Network unavailable. Check your connection and retry. ($raw)"
        else -> raw
    }
}

/**
 * Wraps a suspend one-shot Firestore failure into an exception with a
 * developer-actionable message (index/permission hints preserved).
 */
fun wrapFirestoreError(e: Exception): Exception {
    val message = describeFirestoreError(e)
    return if (message == e.message) e else Exception(message, e)
}

/**
 * Awaiting the first non-Loading emission of a snapshot-backed [Resource] flow.
 * Plain .first() would return the initial Loading frame and leave screens stuck
 * on empty data — this helper is the correct primitive for one-shot reads.
 */
suspend fun <T> Flow<Resource<T>>.firstSettled(): Resource<T> = first { it !is Resource.Loading }
