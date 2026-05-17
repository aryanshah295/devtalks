package com.devtalks.apigateway.grpc

import com.devtalks.apigateway.domain.toTalk
import com.devtalks.v1.GetTalkRequest
import com.devtalks.v1.GetTalkResponse
import com.devtalks.v1.ListTalksRequest
import com.devtalks.v1.ListTalksResponse
import com.devtalks.v1.Talk
import com.devtalks.v1.TalkCatalogServiceGrpc
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Query
import io.grpc.Status
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService

// Reads the `talks` Firestore collection. Each document is keyed by YouTube
// video ID and shaped per PLAN.md:227-241. ListTalks orders by publishedAt
// descending; pagination is not implemented yet (will matter once the corpus
// exceeds the page-size cap).
@GrpcService
class TalkCatalogService(
    private val firestore: Firestore,
) : TalkCatalogServiceGrpc.TalkCatalogServiceImplBase() {

    companion object {
        private const val COLLECTION = "talks"
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 50
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
        val snapshot = firestore.collection(COLLECTION).document(request.id).get().get()
        if (!snapshot.exists()) {
            throw Status.NOT_FOUND
                .withDescription("talk not found: ${request.id}")
                .asRuntimeException()
        }
        return GetTalkResponse.newBuilder().setTalk(snapshot.toTalk()).build()
    }

    private fun doListTalks(request: ListTalksRequest): ListTalksResponse {
        val limit = when {
            request.pageSize <= 0 -> DEFAULT_PAGE_SIZE
            request.pageSize > MAX_PAGE_SIZE -> MAX_PAGE_SIZE
            else -> request.pageSize
        }
        val talks: List<Talk> = firestore.collection(COLLECTION)
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .get()
            .documents
            .map { it.toTalk() }
        return ListTalksResponse.newBuilder()
            .addAllTalks(talks)
            .setNextPageToken("")
            .build()
    }
}
