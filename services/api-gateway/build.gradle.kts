plugins {
    id("devtalks.spring-service-conventions")
}

dependencies {
    implementation(project(":proto-gen"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.logstash.logback.encoder)

    testImplementation(libs.spring.boot.starter.test)
}

springBoot {
    mainClass.set("com.devtalks.apigateway.ApplicationKt")
}