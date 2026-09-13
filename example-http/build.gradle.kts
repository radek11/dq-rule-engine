// Example host: a small HTTP API embedding the library.
// The `application` plugin and the JSON parser are added in E5, together with the main class.
plugins {
    java
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
}
