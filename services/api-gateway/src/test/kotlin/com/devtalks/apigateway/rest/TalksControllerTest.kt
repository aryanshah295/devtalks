package com.devtalks.apigateway.rest

import com.devtalks.v1.GetTalkResponse
import com.devtalks.v1.ListTalksResponse
import com.devtalks.v1.Talk
import com.devtalks.v1.TalkCatalogServiceGrpc
import com.devtalks.v1.TalkStatus
import io.grpc.Status
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(TalksController::class)
@Import(TalksControllerTest.StubConfig::class)
class TalksControllerTest(@Autowired val mockMvc: MockMvc) {

    @TestConfiguration
    class StubConfig {
        @Bean
        fun stub(): TalkCatalogServiceGrpc.TalkCatalogServiceBlockingStub {
            val s = mockk<TalkCatalogServiceGrpc.TalkCatalogServiceBlockingStub>()

            every { s.listTalks(any()) } returns ListTalksResponse.newBuilder()
                .addTalks(sampleTalk("a"))
                .addTalks(sampleTalk("b"))
                .setNextPageToken("")
                .build()

            every { s.getTalk(match { it.id == "found" }) } returns GetTalkResponse.newBuilder()
                .setTalk(sampleTalk("found"))
                .build()
            every { s.getTalk(match { it.id == "missing" }) } throws
                Status.NOT_FOUND.withDescription("nope").asRuntimeException()

            return s
        }

        private fun sampleTalk(id: String) = Talk.newBuilder()
            .setId(id)
            .setTitle("Title $id")
            .setChannel("KubeCon")
            .setDescription("desc $id")
            .setDurationSeconds(60)
            .setPublishedAt("2024-01-01T00:00:00Z")
            .setThumbnailUrl("https://example.com/$id.jpg")
            .setStatus(TalkStatus.TALK_STATUS_INGESTED)
            .build()
    }

    @Test
    fun `GET talks returns DTO list`() {
        mockMvc.perform(get("/api/v1/talks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.talks", org.hamcrest.Matchers.hasSize<Int>(2)))
            .andExpect(jsonPath("$.talks[0].id").value("a"))
            .andExpect(jsonPath("$.talks[0].status").value("INGESTED"))
            .andExpect(jsonPath("$.nextPageToken").value(""))
    }

    @Test
    fun `GET talk by id returns wrapped DTO`() {
        mockMvc.perform(get("/api/v1/talks/found"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.talk.id").value("found"))
            .andExpect(jsonPath("$.talk.title").value("Title found"))
    }

    @Test
    fun `GET talk by unknown id returns 404`() {
        mockMvc.perform(get("/api/v1/talks/missing"))
            .andExpect(status().isNotFound)
    }
}
