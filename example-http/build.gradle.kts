// Example host: a small HTTP API embedding the library.
// Only the streaming JSON parser is added — no databind, no web framework (the server is the JDK's).
plugins {
    application
}

application {
    mainClass = "io.github.radek11.dq.example.http.Main"
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    // Jackson 3: JacksonException is unchecked, so the parser fits behind Iterator.next().
    implementation("tools.jackson.core:jackson-core:3.2.2")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
