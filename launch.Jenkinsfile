pipeline {
    agent {
        node LAUNCH_NODE
    }

    tools {
        jdk 'openjdk-15.0.2'
    }

    environment {
        DOWNLOADED_JAR_NAME = "${SOURCE_BUILD_JOBNAME}-${SOURCE_BUILD_NUMBER}-${JAR_NAME}"
        LOG_FILE_PREFIX = "rabbit"
        LOG_FILE = "${LOG_FILE_PREFIX}-${env.BUILD_NUMBER}.log"
        APP_PROTOCOL = "http"
        APP_PORT = 8100
        LAUNCH_COMMAND = "nohup bash -c \"java -jar '${DOWNLOADED_JAR_NAME}' --rabbit.port=${APP_PORT}\" > '${LOG_FILE}' &"
        LAUNCH_COMMAND_IDENTIFYING_STRING = "rabbit.port="
        EXPECTED_RESPONSE = "<title>rabbit-frontend</title>"
        BACKEND_JAR_CHECKSUM_FILE = "backend-checksum.txt"
        NEEDS_TO_RUN = true
    }

    stages {
        stage('Prepare') {
            when {
                expression {
                    return !fileExists("${DOWNLOADED_JAR_NAME}");
                }
            }
            steps {
                script {
                    sh 'pwd'
                    copyArtifacts([
                        projectName: "${SOURCE_BUILD_JOBNAME}",
                        selector: specific("${SOURCE_BUILD_NUMBER}"),
                        filter: "build/libs/${JAR_NAME}",
                    ])
                    sh 'cp "build/libs/${JAR_NAME}" "${DOWNLOADED_JAR_NAME}"'
                    sh 'rm -rf "build"'
                }
            }
        }
        stage('Check if same') {
        steps {
            script {
                isRunning = sh(
                    label: 'Check if the server is running',
                    script: "curl --insecure ${APP_PROTOCOL}://localhost:${APP_PORT} | grep '${EXPECTED_RESPONSE}'",
                    returnStatus: true
                ) == 0
                needsToRun = !isRunning
                if (!needsToRun) {
                    if (fileExists(BACKEND_JAR_CHECKSUM_FILE)) {
                        lastChecksum = readFile(BACKEND_JAR_CHECKSUM_FILE).trim()
                        currentChecksum = sh(
                            label: 'Get MD5 checksum of current .jar file',
                            script: "md5sum '${DOWNLOADED_JAR_NAME}' | awk '{print \$1;}'",
                            returnStdout: true,
                            encoding: 'UTF-8'
                        )
                        needsToRun = lastChecksum != currentChecksum
                        env {
                            NEEDS_TO_RUN = needsToRun
                        }
                    } else {
                        needsToRun = true
                    }
                }
            }
        }
        }
        stage('Stop') {
            when {
                environment name: 'NEEDS_TO_RUN', value: 'true'
            }
            steps {
                script {
                    processOutput = sh returnStdout: true, script: "ps -C java -u '${env.USER}' -o pid=,command= | grep '${LAUNCH_COMMAND_IDENTIFYING_STRING}' | awk '{print \$1;}'"
                    processOutput.split('\n').each { pid ->
                        if (pid.length() > 0) {
                            echo "Killing: ${pid}"
                            killStatus = sh returnStatus: true, script: "kill ${pid}"
                            if (killStatus != 0) {
                                echo "Process ${pid} must have already been stopped."
                            }
                        }
                    }
                    sleep 5
                    curlStatus = sh returnStatus: true, script: "curl --insecure ${APP_PROTOCOL}://localhost:${APP_PORT}"
                    if (curlStatus == 0) {
                        error "The app is still running or something else has taken up port :${APP_PORT}! Kill it manually."
                    }
                    sh "mkdir -p 'old-logs' && mv ${LOG_FILE_PREFIX}-*.log ./old-logs || echo 'No logs to move.'"
                }
            }
        }
        stage('Launch') {
            when {
                environment name: 'NEEDS_TO_RUN', value: 'true'
            }
            steps {
                // https://devops.stackexchange.com/questions/1473/running-a-background-process-in-pipeline-job
                withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
                    script {
                        sh "md5sum '${DOWNLOADED_JAR_NAME}' | awk '{print \$1;}' > '${BACKEND_JAR_CHECKSUM_FILE}'"
                        sh LAUNCH_COMMAND
                    }
                }
            }
        }
        stage('Verify') {
            when {
                environment name: 'NEEDS_TO_RUN', value: 'true'
            }
            steps {
                sleep 20
                script {
                    if (fileExists(LOG_FILE)) {
                        sh "tail -n 100 '${LOG_FILE}'"
                    } else {
                        error "The app does not have an output file '${LOG_FILE}'!"
                    }
                    sh "curl --insecure ${APP_PROTOCOL}://localhost:${APP_PORT} | grep '${EXPECTED_RESPONSE}'"
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: "${LOG_FILE}", fingerprint: true
        }
    }
}
