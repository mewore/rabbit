plugins {
    application
    id("com.github.spotbugs").version("4.7.3")
    id("jacoco")
}

repositories {
    mavenCentral()
}

val lombokArtifact = "org.projectlombok:lombok:1.18.22"

dependencies {
    implementation(project(":core"))

    annotationProcessor(lombokArtifact)
    compileOnly(lombokArtifact)
    compileOnly("org.checkerframework:checker-qual:3.21.2")

    testAnnotationProcessor(lombokArtifact)
    testCompileOnly(lombokArtifact)
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("org.mockito:mockito-core:4.3.1")
}

tasks.spotbugsMain {
    reports.create("html")
    excludeFilter.fileValue(projectDir.toPath().resolve("spotbugs-exclude.xml").toFile())
}

val editorClass = "moe.mewore.rabbit.world.editor.WorldEditor"

application {
    mainClass.set(editorClass)
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
    manifest {
        attributes["Main-Class"] = editorClass
    }

    duplicatesStrategy = DuplicatesStrategy.WARN

    doFirst("Include dependency jars in the jar (make a fat jar)") {
        from(configurations.runtimeClasspath.get().files.filter { it.isFile && it.name.endsWith(".jar") }.map {
            zipTree(it).matching { exclude("about.html", "META-INF/**") }
        })
    }
}
