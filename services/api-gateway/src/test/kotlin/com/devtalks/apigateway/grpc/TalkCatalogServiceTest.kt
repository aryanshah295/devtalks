package com.devtalks.apigateway.grpc

import com.devtalks.v1.GetTalkResponse
import com.devtalks.v1.ListTalksResponse
import com.devtalks.v1.getTalkRequest
import com.devtalks.v1.listTalksRequest
import com.google.api.core.ApiFutures
import com.google.cloud.Timestamp
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import com.google.cloud.firestore.QuerySnapshot
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TalkCatalogServiceTest {

    private val firestore: Firestore = mockk()
    private val service = TalkCatalogService(firestore)

    @Test
    fun `getTalk returns the talk when the document exists`() {
        stubGet("kBF6Bvth0zw", existing = true)

        val observer = CapturingObserver<GetTalkResponse>()
        service.getTalk(getTalkRequest { id = "kBF6Bvth0zw" }, observer)

        assertThat(observer.value?.talk?.id).isEqualTo("kBF6Bvth0zw")
        assertThat(observer.value?.talk?.title).isEqualTo("Test Talk")
        assertThat(observer.completed).isTrue()
    }

    @Test
    fun `getTalk on missing document surfaces NOT_FOUND`() {
        stubGet("missing", existing = false)

        val observer = CapturingObserver<GetTalkResponse>()
        val ex = runCatching {
            service.getTalk(getTalkRequest { id = "missing" }, observer)
        }.exceptionOrNull()

        assertThat(ex).isInstanceOf(StatusRuntimeException::class.java)
        assertThat((ex as StatusRuntimeException).status.code).isEqualTo(Status.Code.NOT_FOUND)
    }

    @Test
    fun `listTalks returns documents ordered by publishedAt desc`() {
        stubList(listOf("a", "b", "c"))

        val observer = CapturingObserver<ListTalksResponse>()
        service.listTalks(listTalksRequest { pageSize = 10 }, observer)

        assertThat(observer.value?.talksList?.map { it.id }).containsExactly("a", "b", "c")
        assertThat(observer.value?.nextPageToken).isEmpty()
    }

    private fun stubGet(id: String, existing: Boolean) {
        val collection: CollectionReference = mockk()
        val docRef: DocumentReference = mockk()
        val snapshot: DocumentSnapshot = mockk()

        every { firestore.collection("talks") } returns collection
        every { collection.document(id) } returns docRef
        every { docRef.get() } returns ApiFutures.immediateFuture(snapshot)
        every { snapshot.exists() } returns existing
        if (existing) {
            every { snapshot.id } returns id
            every { snapshot.getString("id") } returns id
            every { snapshot.getString("title") } returns "Test Talk"
            every { snapshot.getString("channel") } returns "KubeCon"
            every { snapshot.getString("description") } returns ""
            every { snapshot.getString("thumbnailUrl") } returns ""
            every { snapshot.getString("status") } returns "INGESTED"
            every { snapshot.getLong("durationSeconds") } returns 0L
            every { snapshot.get("publishedAt") } returns Timestamp.ofTimeSecondsAndNanos(0, 0)
        }
    }

    private fun stubList(ids: List<String>) {
        val collection: CollectionReference = mockk()
        val ordered: Query = mockk()
        val limited: Query = mockk()
        val querySnapshot: QuerySnapshot = mockk()

        every { firestore.collection("talks") } returns collection
        every { collection.orderBy("publishedAt", Query.Direction.DESCENDING) } returns ordered
        every { ordered.limit(any()) } returns limited
        every { limited.get() } returns ApiFutures.immediateFuture(querySnapshot)
        every { querySnapshot.documents } returns ids.map { id ->
            mockk<com.google.cloud.firestore.QueryDocumentSnapshot>().also { snap ->
                every { snap.id } returns id
                every { snap.getString("id") } returns id
                every { snap.getString("title") } returns "Talk $id"
                every { snap.getString("channel") } returns "KubeCon"
                every { snap.getString("description") } returns ""
                every { snap.getString("thumbnailUrl") } returns ""
                every { snap.getString("status") } returns "INGESTED"
                every { snap.getLong("durationSeconds") } returns 0L
                every { snap.get("publishedAt") } returns Timestamp.ofTimeSecondsAndNanos(0, 0)
            }
        }
    }
}

private class CapturingObserver<T> : StreamObserver<T> {
    var value: T? = null
    var error: Throwable? = null
    var completed: Boolean = false

    override fun onNext(value: T) { this.value = value }
    override fun onError(t: Throwable) { error = t }
    override fun onCompleted() { completed = true }
}
