import java.io.FileOutputStream
import java.io.OutputStream

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

val determineJarDependencies = tasks.create("determineJarDependencies") {
    setDependsOn(listOf(tasks.jar))
    val jarFile = tasks.jar.get().outputs.files.filter { it.extension == "jar" }.singleFile
    inputs.files(jarFile)
    val outputDir = projectDir.resolve("build/jar-dependencies")
    val outputFile = outputDir.resolve("dependencies.txt")
    outputs.files(outputFile)

    doLast("Get .jar file dependencies") {
        exec {
            val javaPathVarName = "LINUX_JAVA_PATH"
            val javaPath = System.getenv()[javaPathVarName]
                ?: throw kotlin.Error("The environment variable '$javaPathVarName' has not been set!")
            setCommandLine("${javaPath}/bin/jdeps", jarFile.absolutePath)
            standardOutput = JarDependencyFilteringStream(FileOutputStream(outputFile))
        }
    }
}

class JarDependencyFilteringStream(private val targetStream: OutputStream) : OutputStream() {
    private val writtenDependencies = HashSet<String>()
    private val buffer = StringBuilder()
    private var hasPrevious = false

    override fun write(b: Int) {
        val c = b.toChar()
        if (c == '\n') {
            val dependency = buffer.toString()
            buffer.clear()
            if (!writtenDependencies.contains(dependency) && dependency.startsWith("java.")) {
                if (hasPrevious) {
                    targetStream.write(','.toInt())
                }
                targetStream.write(dependency.toByteArray())
                writtenDependencies.add(dependency)
                hasPrevious = true
            }
        } else if (c == ' ' || c == '\t') {
            buffer.clear()
        } else {
            buffer.append(c)
        }
    }
}

val createLinuxJre = tasks.create("createLinuxJre") {
    setDependsOn(listOf(determineJarDependencies))
    val dependencyFile = determineJarDependencies.outputs.files.singleFile
    inputs.files(tasks.jar.get().outputs.files.plus(dependencyFile))
    val outputDir = projectDir.resolve("build/jre/linux")
    outputs.dir(outputDir)

    doLast("Remove the Linux JRE") {
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
    }
    doLast("Create the Linux JRE") {
        exec {
            val javaPathVarName = "LINUX_JAVA_PATH"
            val javaPath = System.getenv()[javaPathVarName]
                ?: throw kotlin.Error("The environment variable '$javaPathVarName' has not been set!")
            setCommandLine(
                "${javaPath}/bin/jlink",
                "--add-modules",
                dependencyFile.readText(),
                "--strip-debug",
                "--no-man-pages",
                "--no-header-files",
                "--output",
                outputDir
            )
        }
    }
}

val createWindowsJre = tasks.create("createWindowsJre") {
    setDependsOn(listOf(determineJarDependencies))
    val dependencyFile = determineJarDependencies.outputs.files.singleFile
    inputs.files(tasks.jar.get().outputs.files.plus(dependencyFile))
    val outputDir = projectDir.resolve("build/jre/windows")
    outputs.dir(outputDir)

    doLast("Remove the Windows JRE") {
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
    }
    doLast("Create the Windows JRE") {
        exec {
            val javaPathVarName = "WINDOWS_JAVA_PATH"
            val javaPath = System.getenv()[javaPathVarName]
                ?: throw kotlin.Error("The environment variable '$javaPathVarName' has not been set!")
            setCommandLine(
                "wine",
                "${javaPath}/bin/jlink.exe",
                "--add-modules",
                dependencyFile.readText(),
                "--strip-debug",
                "--no-man-pages",
                "--no-header-files",
                "--output",
                outputDir
            )
        }
    }
}

val createWindowsExecutable = tasks.create("createWindowsExecutable") {
    setDependsOn(listOf(tasks.jar))
    val configFileName = "launch4j-config.xml"
    val sourceConfigFile = projectDir.resolve(configFileName)
    val jarFile = tasks.jar.get().outputs.files.singleFile
    inputs.files(jarFile, sourceConfigFile)
    val outputDir = projectDir.resolve("build/windows-executable")
    val exeFile = outputDir.resolve("${jarFile.nameWithoutExtension}.exe")
    val configFile = outputDir.resolve(configFileName)
    outputs.files(exeFile, configFile)

    doLast("Prepare the Launch4J config file") {
        copy {
            from(sourceConfigFile)
            filter {
                it.replace("<jar/>", "<jar>$jarFile</jar>").replace("<outfile/>", "<outfile>$exeFile</outfile>")
            }
            into(outputDir)
        }
    }
    doLast("Use Launch4J to turn a .jar into an .exe") {
        exec {
            val launch4jPathVarName = "LAUNCH4J_PATH"
            val launch4jPath = System.getenv()[launch4jPathVarName]
                ?: throw kotlin.Error("The environment variable '$launch4jPathVarName' has not been set!")
            setCommandLine("$launch4jPath/launch4jc", configFile)
        }
    }
}

val prepareWindowsExecutable = tasks.create("prepareWindowsExecutable") {
    setDependsOn(listOf(createWindowsExecutable, createWindowsJre))
    val exeFile = createWindowsExecutable.outputs.files.filter { it.extension == "exe" }.singleFile
    val jreDir = createWindowsJre.outputs.files.filter { it.extension == "" }.single()
    inputs.dir(jreDir)
    inputs.files(exeFile)
    val outputDir = projectDir.resolve("build/executable/windows")
    val archiveRootDir = outputDir.resolve(System.getenv()["WINDOWS_ROOT_DIR"] ?: exeFile.nameWithoutExtension)
    outputs.dir(archiveRootDir)

    doLast("Create the directory") {
        if (!archiveRootDir.exists() && !archiveRootDir.mkdirs()) {
            throw kotlin.Error("Failed to create ${archiveRootDir.absolutePath}")
        }
    }
    doLast("Copy the .exe file") {
        copy {
            from(exeFile)
            into(archiveRootDir)
        }
    }
    doLast("Copy the JRE directory") {
        copy {
            from(jreDir)
            include { it.file.canRead() && it.file.canWrite() }
            into(archiveRootDir.resolve("jre"))
        }
    }
}

val windowsExecutableZip = tasks.create<Zip>("windowsExecutableZip") {
    setDependsOn(listOf(prepareWindowsExecutable))
    val dirToCompress = prepareWindowsExecutable.outputs.files.filter { it.extension == "" }.single()
    inputs.dir(dirToCompress)
    val destinationDir = projectDir.resolve("build/executable/windows")
    val archiveName = "${System.getenv()["WINDOWS_ARCHIVE_NAME"] ?: dirToCompress.name}.zip"
    outputs.files(destinationDir.resolve(archiveName))

    from(dirToCompress.parentFile)
    include("${dirToCompress.name}/**")
    destinationDirectory.set(destinationDir)
    archiveFileName.set(archiveName)
}

val prepareLinuxExecutable = tasks.create("prepareLinuxExecutable") {
    val jarTask = tasks.jar.get()
    setDependsOn(listOf(createLinuxJre, jarTask))
    val jarFile = jarTask.outputs.files.filter { it.extension == "jar" }.singleFile
    val jreDir = createLinuxJre.outputs.files.filter { it.extension == "" }.single()
    inputs.dir(jreDir)
    inputs.files(jarFile)
    val outputDir = projectDir.resolve("build/executable/linux")
    val archiveRootDir = outputDir.resolve(System.getenv()["LINUX_ROOT_DIR"] ?: jarFile.nameWithoutExtension)
    outputs.dir(archiveRootDir)

    doLast("Create the directory") {
        if (!archiveRootDir.exists() && !archiveRootDir.mkdirs()) {
            throw kotlin.Error("Failed to create ${archiveRootDir.absolutePath}")
        }
    }
    doLast("Copy the .jar file") {
        copy {
            from(jarFile)
            into(archiveRootDir)
        }
    }
    doLast("Copy the JRE directory") {
        copy {
            from(jreDir)
            include { it.file.canRead() && it.file.canWrite() }
            into(archiveRootDir.resolve("jre"))
        }
    }
    doLast("Create a .sh script that runs the JRE") {
        val scriptFile = archiveRootDir.resolve(jarFile.nameWithoutExtension + ".sh")
        scriptFile.writeText("./jre/bin/java -jar ./${jarFile.name}")
        scriptFile.setExecutable(true)
    }
}

val linuxExecutableTar = tasks.create<Tar>("linuxExecutableTar") {
    setDependsOn(listOf(prepareLinuxExecutable))
    val dirToCompress = prepareLinuxExecutable.outputs.files.filter { it.extension == "" }.single()
    inputs.dir(dirToCompress)
    val destinationDir = projectDir.resolve("build/executable/linux")
    val archiveName = "${System.getenv()["LINUX_ARCHIVE_NAME"] ?: dirToCompress.name}.tar.gz"
    outputs.files(destinationDir.resolve(archiveName))

    from(dirToCompress.parentFile)
    include("${dirToCompress.name}/**")
    destinationDirectory.set(destinationDir)
    archiveFileName.set(archiveName)
}

val packageAll = tasks.create("packageAll") {
    setDependsOn(listOf(windowsExecutableZip, linuxExecutableTar))
}
