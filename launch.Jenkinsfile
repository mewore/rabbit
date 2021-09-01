pipeline {
    agent {
        node LAUNCH_NODE
    }

    tools {
        jdk 'openjdk-15.0.2'
    }

    environment {
        LOG_FILE="rabbit-${env.BUILD_NUMBER}.log"
        DOWNLOADED_JAR_NAME = "${RABBIT_BUILD_JOBNAME}-${RABBIT_BUILD_NUMBER}-${JAR_NAME}"
        LAUNCH_COMMAND = "nohup bash -c \"java -jar '${DOWNLOADED_JAR_NAME}' --rabbit.port=8010\" > '${LOG_FILE}' &"
        PROTOCOL = "http"
        PORT = 8100
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
                    copyArtifacts(projectName: "${RABBIT_BUILD_JOBNAME}", selector: specific("${RABBIT_BUILD_NUMBER}"), filter: "build/libs/${JAR_NAME}")
                    sh 'cp "build/libs/${JAR_NAME}" "${DOWNLOADED_JAR_NAME}"'
                    sh 'rm -rf "build"'
                }
            }
        }
        stage('Stop') {
            steps {
                script {
                    processOutput = sh returnStdout: true, script: "ps -C java -u '${env.USER}' -o pid=,command= | grep '--rabbit.port=' | awk '{print \$1;}'"
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
                    curlStatus = sh returnStatus: true, script: "curl --insecure ${PROTOCOL}://localhost:${PORT}"
                    if (curlStatus == 0) {
                        error "The app is still running or something else has taken up port :${PORT}! Kill it manually."
                    }
                    sh "mkdir -p 'old-logs' && mv rabbit-*.log ./old-logs || echo 'No logs to move.'"
                }
            }
        }
        stage('Launch') {
            steps {
                // https://devops.stackexchange.com/questions/1473/running-a-background-process-in-pipeline-job
                withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
                    script {
                        sh LAUNCH_COMMAND
                    }
                }
            }
        }
        stage('Verify') {
            steps {
                sleep 20
                script {
                    if (fileExists(LOG_FILE)) {
                        sh "tail -n 100 '${LOG_FILE}'"
                    } else {
                        error "The app does not have an output file '${LOG_FILE}'!"
                    }
                    sh "curl --insecure ${PROTOCOL}://localhost:${PORT} | grep 'ZUN'"
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
