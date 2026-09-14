// The library. No runtime dependencies — hosts must be able to embed it without
// inheriting a JSON parser, logger or framework.
plugins {
    `java-library`
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.javadoc {
    // Check syntax and references, but do not demand a comment on every enum constant.
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("memory")
    }
}

// K7: one run of 1,000,000 records under a fixed heap. Manual, not part of build — it takes
// longer than the unit tests and its limit is a measured number, see DESIGN.md §2:
// the smallest passing heap was 8m, the limit is twice that. -PmemoryTest.heap=… overrides it.
val memoryTest by tasks.registering(Test::class) {
    description = "Runs 1,000,000 records under a fixed heap limit."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("memory")
    }
    maxHeapSize = providers.gradleProperty("memoryTest.heap").getOrElse("16m")
    outputs.upToDateWhen { false }
    testLogging {
        showStandardStreams = true
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
