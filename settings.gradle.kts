rootProject.name = "devtalks"

pluginManagement {
    includeBuild("gradle/conventions")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(
    ":proto-gen",
    ":services:api-gateway",
)