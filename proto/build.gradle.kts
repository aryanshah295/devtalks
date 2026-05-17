plugins {
    id("devtalks.kotlin-conventions")
}

sourceSets {
    main {
        java.srcDirs("gen/java")
        kotlin.srcDirs("gen/kotlin")
    }
}

dependencies {
    // BOM keeps the grpc-* and protobuf-* versions aligned across the whole build.
    api(enforcedPlatform(libs.google.cloud.libraries.bom))

    api(libs.grpc.kotlin.stub)
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.protobuf.kotlin)
    api(libs.protobuf.java)
    api("javax.annotation:javax.annotation-api:1.3.2")
}

// Regenerate Java/Kotlin stubs from .proto sources if they're missing or stale.
val generateProto = tasks.register<Exec>("generateProto") {
    description = "Runs `buf generate` to regenerate Java/Kotlin stubs."
    group = "build"
    workingDir = projectDir
    commandLine("buf", "generate")
    inputs.files(fileTree(projectDir) {
        include("**/*.proto")
        include("buf.yaml")
        include("buf.gen.yaml")
    })
    outputs.dirs(file("gen/java"), file("gen/kotlin"))
}

tasks.named("compileKotlin") { dependsOn(generateProto) }
tasks.named("compileJava") { dependsOn(generateProto) }
