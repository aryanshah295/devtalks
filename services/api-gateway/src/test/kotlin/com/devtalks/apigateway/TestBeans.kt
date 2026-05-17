package com.devtalks.apigateway

import com.google.cloud.firestore.Firestore
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

// Shared mock beans for @SpringBootTest classes so the context never reaches
// real GCP. `@Import(TestBeans::class)` in a slice/integration test pulls these in.
@TestConfiguration
class TestBeans {

    @Bean
    @Primary
    fun firestoreMock(): Firestore = mockk(relaxed = true)
}
