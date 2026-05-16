plugins {
    id("devtalks.kotlin-conventions")
}

sourceSets {
    main {
        java.srcDirs("../proto/gen/java")
        kotlin.srcDirs("../proto/gen/kotlin")
    }
}

dependencies {
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.protobuf)
    api(libs.grpc.stub)
    api(libs.protobuf.kotlin)
    api(libs.protobuf.java)
    api("javax.annotation:javax.annotation-api:1.3.2")
}

// Run `buf generate` (in ../proto) before compiling if generated sources are missing or stale.
val generateProto = tasks.register<Exec>("generateProto") {
    description = "Runs `buf generate` in the proto/ workspace to regenerate Java/Kotlin stubs."
    group = "build"
    workingDir = file("../proto")
    commandLine("buf", "generate")
    inputs.files(fileTree("../proto") { include("**/*.proto"); include("buf.yaml"); include("buf.gen.yaml") })
    outputs.dirs(file("../proto/gen/java"), file("../proto/gen/kotlin"))
}

tasks.named("compileKotlin") { dependsOn(generateProto) }
tasks.named("compileJava") { dependsOn(generateProto) }