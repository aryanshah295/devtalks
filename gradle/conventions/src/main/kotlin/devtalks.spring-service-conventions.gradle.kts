import com.google.cloud.tools.jib.gradle.JibExtension

plugins {
    id("devtalks.kotlin-conventions")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("com.google.cloud.tools.jib")
}

configure<JibExtension> {
    from {
        image = "eclipse-temurin:21-jre-jammy"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
        }
    }
    to {
        // Image set lazily on :jib/:jibDockerBuild/:jibBuildTar below, so that
        // tasks other than Jib (compile, test, wrapper, …) don't require env vars.
        image = "placeholder/${project.name}"
    }
    container {
        ports = listOf("8080")
        jvmFlags = listOf(
            "-XX:MaxRAMPercentage=75.0",
            "-XX:+UseG1GC",
            "-Djava.security.egd=file:/dev/./urandom",
        )
        environment = mapOf("SPRING_PROFILES_ACTIVE" to "prod")
        creationTime.set("USE_CURRENT_TIMESTAMP")
    }
}

tasks.matching { it.name in setOf("jib", "jibDockerBuild", "jibBuildTar") }.configureEach {
    doFirst {
        val gcpProject: String = (project.findProperty("gcpProject") as String?)
            ?: System.getenv("GCP_PROJECT_ID")
            ?: error("Set -PgcpProject=<id> or env GCP_PROJECT_ID before running :${name}")
        val gcpRegion: String = (project.findProperty("gcpRegion") as String?)
            ?: System.getenv("GCP_REGION")
            ?: "us-central1"
        val jib = project.extensions.getByType(JibExtension::class.java)
        jib.to.image = "$gcpRegion-docker.pkg.dev/$gcpProject/devtalks/${project.name}"
        jib.to.tags = setOf("latest", System.getenv("GIT_SHA") ?: "dev")
    }
}
