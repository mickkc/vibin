rootProject.name = "vibin"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven {
            name = "jitpack"
            url = uri("https://jitpack.io")
        }
    }
}
