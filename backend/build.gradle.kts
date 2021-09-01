plugins {
    application
    id("com.github.spotbugs").version("4.7.3")
    id("jacoco")
}

repositories {
    mavenCentral()
}

val lombokArtifact = "org.projectlombok:lombok:1.18.20"

dependencies {
    annotationProcessor(lombokArtifact)
    compileOnly(lombokArtifact)
    compileOnly("org.checkerframework:checker-qual:3.16.0")
    implementation("io.javalin:javalin:3.13.11")
    runtimeOnly("org.slf4j:slf4j-api:1.7.32")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.32")

    testAnnotationProcessor(lombokArtifact)
    testCompileOnly(lombokArtifact)
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
    testImplementation("org.mockito:mockito-core:3.12.4")
}

tasks.spotbugsMain {
    reports.create("html")
    excludeFilter.fileValue(projectDir.toPath().resolve("spotbugs-exclude.xml").toFile())
}

val serverClass = "moe.mewore.rabbit.Server"

application {
    mainClass.set(serverClass)
}

tasks.test {
    useJUnitPlatform()
    setFinalizedBy(listOf(tasks.jacocoTestReport))
}

jacoco {
    toolVersion = "0.8.6"
}

tasks.jacocoTestReport {
    dependsOn.add(tasks.test)
    reports.xml.required.set(true)
}

tasks.jar {
    dependsOn.addAll(listOf(tasks.spotbugsMain, tasks.test))
    manifest {
        attributes["Main-Class"] = serverClass
    }

    duplicatesStrategy = DuplicatesStrategy.WARN

    doFirst("Include dependency jars in the jar (make a fat jar)") {
        from(configurations.runtimeClasspath.get().files.filter { it.isFile && it.name.endsWith(".jar") }.map {
            zipTree(it).matching { exclude("about.html", "META-INF/**") }
        })
    }
}
