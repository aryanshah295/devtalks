plugins {
    id("devtalks.spring-service-conventions")
}

dependencies {
    // Pin transitive grpc / gax / google-auth-library versions to a single BOM so the
    // grpc-server starter and google-cloud-firestore don't fight over io.grpc.* versions.
    implementation(platform(libs.google.cloud.libraries.bom))

    implementation(project(":proto"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.logstash.logback.encoder)

    // gRPC server with in-process transport only (no TCP listener).
    implementation(libs.grpc.spring.boot.starter)
    implementation(libs.grpc.netty.shaded)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
}

springBoot {
    mainClass.set("com.devtalks.apigateway.ApplicationKt")
}
