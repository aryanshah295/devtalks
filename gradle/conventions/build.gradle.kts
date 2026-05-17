plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen.plugin)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.dm.plugin)
    implementation(libs.jib.gradle.plugin)
}
