plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("kotlin-kapt")
    id("realm-android")
}
dependencies {
    api(project(mapOf("path" to ":extension:repository")))
    api(project(mapOf("path" to ":extension:repositoryFlow")))
}
