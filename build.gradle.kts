plugins {
    id("java")
}

group = "moe.mewore.rabbit"
version = "0.0.1-SNAPSHOT"

tasks.jar {
    val backendJarTask = tasks.getByPath("backend:jar")
    val jarTasksToMerge = listOf(backendJarTask, tasks.getByPath("frontend:jar"))
    setDependsOn(setOf(jarTasksToMerge))
    inputs.files(jarTasksToMerge)

    entryCompression = ZipEntryCompression.STORED

    doFirst("Read .jar files") {
        val zipTrees = mutableListOf<FileTree>()
        for (taskToMerge in jarTasksToMerge) {
            zipTrees.addAll(taskToMerge.outputs.files.files.filter { file -> file.name.endsWith(".jar") }
                .map { jarFile -> zipTree(jarFile) })
        }
        from(zipTrees)

        val manifestFile = backendJarTask.outputs.files.files.filter { file -> file.name.endsWith(".jar") }
            .flatMap { archive -> zipTree(archive).files.filter { jarFile -> jarFile.name == "MANIFEST.MF" } }.single()
        println("Using the manifest file from the .jar output of task ${backendJarTask.path}: $manifestFile")
        manifest = manifest.from(manifestFile)
    }
}
