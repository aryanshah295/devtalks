package com.devtalks.apigateway.grpc

import com.devtalks.v1.GetTalkRequest
import com.devtalks.v1.GetTalkResponse
import com.devtalks.v1.ListTalksRequest
import com.devtalks.v1.ListTalksResponse
import com.devtalks.v1.Talk
import com.devtalks.v1.TalkCatalogServiceGrpc
import com.devtalks.v1.TalkStatus
import io.grpc.Status
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService

// PR 1: hardcoded stub so the gRPC server + in-process channel + REST wiring can be
// exercised end-to-end before Firestore lands. PR 2 replaces the bodies of doGetTalk /
// doListTalks with real Firestore reads.
//
// Pattern: each override is a thin StreamObserver adapter; the doXxx methods hold
// the actual logic and return the response (or throw StatusRuntimeException, which
// grpc-java's dispatcher forwards to observer.onError automatically).
@GrpcService
class TalkCatalogService : TalkCatalogServiceGrpc.TalkCatalogServiceImplBase() {

    companion object {
        private val HARDCODED_TALKS = listOf(
            Talk.newBuilder()
                .setId("placeholder-1")
                .setTitle("Placeholder Talk — PR 2 will replace with real Firestore data")
                .setChannel("KubeCon")
                .setDescription("Stub used by PR 1 to validate the in-process gRPC wiring.")
                .setDurationSeconds(0)
                .setPublishedAt("2026-01-01T00:00:00Z")
                .setThumbnailUrl("")
                .setStatus(TalkStatus.TALK_STATUS_INGESTED)
                .build(),
        )
    }

    override fun getTalk(request: GetTalkRequest, observer: StreamObserver<GetTalkResponse>) {
        observer.onNext(doGetTalk(request))
        observer.onCompleted()
    }

    override fun listTalks(request: ListTalksRequest, observer: StreamObserver<ListTalksResponse>) {
        observer.onNext(doListTalks(request))
        observer.onCompleted()
    }

    private fun doGetTalk(request: GetTalkRequest): GetTalkResponse {
        val talk = HARDCODED_TALKS.firstOrNull { it.id == request.id }
            ?: throw Status.NOT_FOUND
                .withDescription("talk not found: ${request.id}")
                .asRuntimeException()
        return GetTalkResponse.newBuilder().setTalk(talk).build()
    }

    private fun doListTalks(@Suppress("UNUSED_PARAMETER") request: ListTalksRequest): ListTalksResponse =
        ListTalksResponse.newBuilder()
            .addAllTalks(HARDCODED_TALKS)
            .setNextPageToken("")
            .build()
}