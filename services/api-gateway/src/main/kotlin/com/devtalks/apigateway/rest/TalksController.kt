package com.devtalks.apigateway.rest

import com.devtalks.apigateway.rest.dto.GetTalkDto
import com.devtalks.apigateway.rest.dto.ListTalksDto
import com.devtalks.apigateway.rest.dto.toDto
import com.devtalks.v1.GetTalkRequest
import com.devtalks.v1.ListTalksRequest
import com.devtalks.v1.TalkCatalogServiceGrpc
import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

// REST shell over the in-process gRPC TalkCatalog. The stub is configured in
// GrpcConfig and dials the in-process channel — no network hop.
@RestController
@RequestMapping("/api/v1/talks")
class TalksController(
    private val stub: TalkCatalogServiceGrpc.TalkCatalogServiceBlockingStub,
) {

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "20") pageSize: Int,
        @RequestParam(required = false) pageToken: String?,
    ): ListTalksDto {
        val request = ListTalksRequest.newBuilder()
            .setPageSize(pageSize)
            .setPageToken(pageToken.orEmpty())
            .build()
        val response = stub.listTalks(request)
        return ListTalksDto(
            talks = response.talksList.map { it.toDto() },
            nextPageToken = response.nextPageToken,
        )
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): GetTalkDto {
        val request = GetTalkRequest.newBuilder().setId(id).build()
        val response = try {
            stub.getTalk(request)
        } catch (e: StatusRuntimeException) {
            if (e.status.code == Status.Code.NOT_FOUND) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, e.status.description ?: "talk not found")
            }
            throw e
        }
        return GetTalkDto(talk = response.talk.toDto())
    }
}
