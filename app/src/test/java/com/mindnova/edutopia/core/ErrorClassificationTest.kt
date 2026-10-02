package com.mindnova.edutopia.core

import com.mindnova.edutopia.core.utils.Resource
import com.mindnova.edutopia.core.utils.describeFirestoreError
import com.mindnova.edutopia.core.utils.wrapFirestoreError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorClassificationTest {

    @Test
    fun `missing index error is explained, not hidden`() {
        val ex = Exception(
            "FAILED_PRECONDITION: The query requires an index. You can create it here: https://console.firebase.google.com/..."
        )
        val described = describeFirestoreError(ex)
        assertTrue(described.contains("composite index", ignoreCase = true))
        assertTrue(described.contains("firestore.indexes.json"))
    }

    @Test
    fun `permission denied mentions security rules`() {
        val described = describeFirestoreError(Exception("PERMISSION_DENIED: Missing or insufficient permissions."))
        assertTrue(described.contains("security rules", ignoreCase = true))
    }

    @Test
    fun `network unavailable gets actionable retry text`() {
        val described = describeFirestoreError(Exception("UNAVAILABLE: Backend"))
        assertTrue(described.contains("Network", ignoreCase = true))
    }

    @Test
    fun `unknown errors pass through unchanged`() {
        val raw = "some weird backend message"
        assertTrue(describeFirestoreError(Exception(raw)).contains(raw))
    }

    @Test
    fun `wrapFirestoreError keeps cause and adds hint`() {
        val e = Exception("FAILED_PRECONDITION: requires an index")
        val wrapped = wrapFirestoreError(e)
        assertTrue(wrapped.message!!.contains("composite index"))
    }

    @Test
    fun `Resource Empty and Error are distinct states`() {
        val empty: Resource<List<String>> = Resource.Empty()
        val error: Resource<List<String>> = Resource.Error("boom")
        assertTrue(empty is Resource.Empty)
        assertFalse(empty is Resource.Error)
        assertFalse(error is Resource.Empty)
        val success: Resource<List<String>> = Resource.Success(listOf("a"))
        assertTrue(success is Resource.Success)
        assertFalse(success is Resource.Empty)
    }
}
