package com.devtalks.apigateway.seed

import com.devtalks.apigateway.domain.toFirestoreMap
import com.devtalks.v1.Talk
import com.devtalks.v1.TalkStatus
import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

// Seeds the `talks` collection with 5 hardcoded KubeCon videos. Triggered via:
//   ./gradlew :services:api-gateway:runSeedScript
// which boots the app under the "seed" profile and exits when this runner finishes.
@Component
@Profile("seed")
class SeedRunner(
    private val firestore: Firestore,
    private val ctx: ConfigurableApplicationContext,
) : CommandLineRunner {

    companion object {
        private val log = LoggerFactory.getLogger(SeedRunner::class.java)

        private val KUBECON_TALKS: List<Talk> = listOf(
            Talk.newBuilder()
                .setId("kBF6Bvth0zw")
                .setTitle("Keynote: Welcome to KubeCon + CloudNativeCon North America 2023")
                .setChannel("KubeCon")
                .setDescription("Opening keynote of KubeCon + CloudNativeCon NA 2023 in Chicago.")
                .setDurationSeconds(1735)
                .setPublishedAt("2023-11-07T18:00:00Z")
                .setThumbnailUrl("https://i.ytimg.com/vi/kBF6Bvth0zw/hqdefault.jpg")
                .setStatus(TalkStatus.TALK_STATUS_INGESTED)
                .build(),
            Talk.newBuilder()
                .setId("PnjGmptHRYg")
                .setTitle("Kubernetes Networking: A Deep Dive - Tim Hockin, Google")
                .setChannel("KubeCon")
                .setDescription("A deep dive into the Kubernetes networking model and implementation details.")
                .setDurationSeconds(2418)
                .setPublishedAt("2022-10-26T15:00:00Z")
                .setThumbnailUrl("https://i.ytimg.com/vi/PnjGmptHRYg/hqdefault.jpg")
                .setStatus(TalkStatus.TALK_STATUS_INGESTED)
                .build(),
            Talk.newBuilder()
                .setId("BE77h7dmoQU")
                .setTitle("Service Mesh Sidecar Performance: Measuring Latency Overhead")
                .setChannel("KubeCon")
                .setDescription("Benchmarking sidecar latency overhead across popular service meshes.")
                .setDurationSeconds(1822)
                .setPublishedAt("2023-04-20T17:00:00Z")
                .setThumbnailUrl("https://i.ytimg.com/vi/BE77h7dmoQU/hqdefault.jpg")
                .setStatus(TalkStatus.TALK_STATUS_INGESTED)
                .build(),
            Talk.newBuilder()
                .setId("0Omvgd7Hg1I")
                .setTitle("eBPF: Unlocking the Kernel for Kubernetes Observability")
                .setChannel("KubeCon")
                .setDescription("How eBPF changes the game for Kubernetes networking and observability.")
                .setDurationSeconds(2110)
                .setPublishedAt("2023-05-18T16:30:00Z")
                .setThumbnailUrl("https://i.ytimg.com/vi/0Omvgd7Hg1I/hqdefault.jpg")
                .setStatus(TalkStatus.TALK_STATUS_INGESTED)
                .build(),
            Talk.newBuilder()
                .setId("WoZG3J-tn7E")
                .setTitle("Pod Scheduling at Scale: Lessons from Operating 100K+ Node Fleets")
                .setChannel("KubeCon")
                .setDescription("Scheduler tuning patterns learned from running very large Kubernetes clusters.")
                .setDurationSeconds(1986)
                .setPublishedAt("2024-03-19T20:00:00Z")
                .setThumbnailUrl("https://i.ytimg.com/vi/WoZG3J-tn7E/hqdefault.jpg")
                .setStatus(TalkStatus.TALK_STATUS_INGESTED)
                .build(),
        )
    }

    override fun run(vararg args: String) {
        log.info("seeding {} talks", KUBECON_TALKS.size)
        val now = Timestamp.now()
        val batch = firestore.batch()
        KUBECON_TALKS.forEach { talk ->
            val doc = firestore.collection("talks").document(talk.id)
            val data = talk.toFirestoreMap() + mapOf(
                "createdAt" to now,
                "updatedAt" to now,
            )
            batch.set(doc, data)
        }
        batch.commit().get()
        log.info("seed complete")
        SpringApplication.exit(ctx, ExitCodeGenerator { 0 })
    }
}
