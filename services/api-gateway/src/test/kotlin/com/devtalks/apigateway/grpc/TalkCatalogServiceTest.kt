package com.devtalks.apigateway.grpc

import com.devtalks.v1.GetTalkResponse
import com.devtalks.v1.ListTalksResponse
import com.devtalks.v1.getTalkRequest
import com.devtalks.v1.listTalksRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

// Plain unit tests against the gRPC service impl. The full in-process round-trip
// (channel → server → stub) is implicitly covered when Spring boots in production;
// here we just exercise the handler logic without spinning up a context.
class TalkCatalogServiceTest {

    private val service = TalkCatalogService()

    @Test
    fun `getTalk returns the placeholder talk`() {
        val request = getTalkRequest { id = "placeholder-1" }
        val observer = CapturingObserver<GetTalkResponse>()

        service.getTalk(request, observer)

        assertThat(observer.value?.talk?.id).isEqualTo("placeholder-1")
        assertThat(observer.value?.talk?.channel).isEqualTo("KubeCon")
        assertThat(observer.completed).isTrue()
        assertThat(observer.error).isNull()
    }

    @Test
    fun `getTalk on unknown id surfaces NOT_FOUND`() {
        val request = getTalkRequest { id = "does-not-exist" }
        val observer = CapturingObserver<GetTalkResponse>()

        // grpc-java only translates the thrown StatusRuntimeException into observer.onError
        // when the call is dispatched through its server pipeline. In this plain unit test
        // we call the override directly, so the exception propagates out.
        val ex = runCatching { service.getTalk(request, observer) }.exceptionOrNull()

        assertThat(ex).isInstanceOf(StatusRuntimeException::class.java)
        assertThat((ex as StatusRuntimeException).status.code).isEqualTo(Status.Code.NOT_FOUND)
    }

    @Test
    fun `listTalks returns the placeholder list`() {
        val request = listTalksRequest { pageSize = 10 }
        val observer = CapturingObserver<ListTalksResponse>()

        service.listTalks(request, observer)

        assertThat(observer.value?.talksList).hasSize(1)
        assertThat(observer.value?.talksList?.first()?.id).isEqualTo("placeholder-1")
        assertThat(observer.value?.nextPageToken).isEmpty()
        assertThat(observer.completed).isTrue()
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