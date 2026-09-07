plugins {
    application
    id("com.gradleup.shadow") version "9.6.1"
}

group = "ca.maximilian.extraction_game"

dependencies {
    implementation("net.minestom:minestom:2026.08.28-26.2")
    implementation ("dev.hollowcube:schem:2.0.1")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.26.0")
    implementation("org.apache.logging.log4j:log4j-core:2.26.0")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "ca.maximilian.extraction_game.ExtractionGame"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "ca.maximilian.extraction_game.Main"
    }
}
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
    mergeServiceFiles()
    archiveClassifier.set("")
}

tasks.named("startScripts") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named("startShadowScripts") {
    dependsOn(tasks.named("jar"))
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
