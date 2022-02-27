plugins {
    id("java")
    id("com.github.node-gradle.node") version "3.0.0-rc5"
}

group = "moe.mewore.rabbit"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

node {
    version.set("16.14.0")
    npmVersion.set("8.5.2")
    download.set(true)
}

val commonRootSourceFiles = listOf("build.gradle.kts", "babel.config.json", "package.json", "tsconfig.json")

val frontendLint = tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendLint") {
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
    inputs.files(commonRootSourceFiles)
    inputs.files(".eslintrc.json")
    outputs.upToDateWhen { true }
    args.set(listOf("run", "lint"))
    description = "Lints the frontend code."
}

tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendBuild") {
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
    inputs.dir("public")
    inputs.files(commonRootSourceFiles)
    outputs.dir("dist")
    args.set(listOf("run", "build"))
    description = "Builds the the frontend after ensuring the NPM dependencies are present."
}

val frontendBuildProd = tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendBuildProd") {
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
    inputs.dir("public")
    inputs.files(commonRootSourceFiles)
    outputs.dir("dist")
    args.set(listOf("run", "build-prod"))
    description = "Builds the the frontend in PRODUCTION mode after ensuring the NPM dependencies are present."
}

tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendBuildWatch") {
    setDependsOn(listOf(tasks.npmInstall))
    args.set(listOf("run", "build-watch"))
    description = "Builds the the frontend after ensuring the NPM dependencies are present and watches for changes."
}

val frontendTest = tasks.create<com.github.gradle.node.npm.task.NpmTask>("frontendTest") {
    setDependsOn(listOf(tasks.npmInstall))
    inputs.dir("src")
    inputs.dir("tests")
    inputs.files(commonRootSourceFiles)
    // TODO: Enable unit test coverage (https://github.com/vuejs/vue-test-utils/issues/165)
//    outputs.dir("coverage")
    outputs.upToDateWhen { true }
    args.set(listOf("run", "test"))
    description = "Runs the frontend unit tests."
}

tasks.jar {
    setDependsOn(listOf(frontendBuildProd))
    inputs.dir("dist")
    outputs.dir("build")
    from("dist")
    into("static")
}
