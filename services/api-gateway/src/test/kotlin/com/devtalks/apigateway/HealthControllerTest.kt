package com.devtalks.apigateway

import com.devtalks.apigateway.rest.HealthController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(HealthController::class)
class HealthControllerTest(@Autowired val mockMvc: MockMvc) {

    @Test
    fun `api healthz returns ok`() {
        mockMvc.perform(get("/api/healthz"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ok"))
    }
}
