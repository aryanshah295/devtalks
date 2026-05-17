package com.devtalks.apigateway.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// Singleton Firestore client wired with Application Default Credentials.
// Locally this picks up `gcloud auth application-default login`; on Cloud Run it
// uses the attached `sa-api-gateway` runtime service account.
@Configuration
class FirestoreConfig {

    @Bean(destroyMethod = "close")
    fun firestore(@Value("\${gcp.project-id}") projectId: String): Firestore =
        FirestoreOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()
            .service
}
