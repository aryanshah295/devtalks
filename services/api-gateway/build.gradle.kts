import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    id("devtalks.spring-service-conventions")
}

dependencies {
    // Pin transitive grpc / gax / google-auth-library versions to a single BOM so the
    // grpc-server starter and google-cloud-firestore don't fight over io.grpc.* versions.
    // enforcedPlatform overrides Spring's dependency-management plugin, which would
    // otherwise pin grpc to an older version that mismatches grpc-netty-shaded.
    implementation(enforcedPlatform(libs.google.cloud.libraries.bom))

    implementation(project(":proto"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.logstash.logback.encoder)

    // gRPC server with in-process transport only (no TCP listener).
    implementation(libs.grpc.spring.boot.starter)
    implementation(libs.grpc.netty.shaded)

    // Firestore client (uses ADC locally, attached SA in Cloud Run).
    implementation(libs.google.cloud.firestore)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
}

springBoot {
    mainClass.set("com.devtalks.apigateway.ApplicationKt")
}

// `./gradlew :services:api-gateway:runSeedScript` boots the app under the "seed"
// profile so SeedRunner inserts 5 KubeCon talks and exits.
tasks.register<BootRun>("runSeedScript") {
    group = "application"
    description = "Seed 5 talks into Firestore via the seed profile."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.devtalks.apigateway.ApplicationKt")
    args("--spring.profiles.active=seed")
}
