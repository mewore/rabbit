def shouldLaunch = true;
def newEditorVersion = true;

pipeline {
    agent {
        node LAUNCH_NODE
    }

    tools {
        jdk 'openjdk-15.0.2'
    }

    environment {
        DOWNLOADED_JAR_NAME = "${SOURCE_BUILD_JOBNAME}-${SOURCE_BUILD_NUMBER}-${JAR_NAME}"
        SERVER_CHECKSUM_FILE = "server-jar.md5"
        LOG_FILE_PREFIX = "rabbit"
        LOG_FILE = "${LOG_FILE_PREFIX}-${env.BUILD_NUMBER}.log"
        APP_PROTOCOL = "http"
        APP_PORT = 8100
        LAUNCH_COMMAND = "nohup bash -c \"java -jar '${DOWNLOADED_JAR_NAME}' --rabbit.port=${APP_PORT} --rabbit.static.external=./static\" > '${LOG_FILE}' &"
        LAUNCH_COMMAND_IDENTIFYING_STRING = "rabbit.port="
        EXPECTED_RESPONSE = "<title>rabbit-frontend</title>"
        EDITOR_JAR_FILE = "editor.jar"
        EDITOR_JAR_PATH = "./editor/build/libs/${EDITOR_JAR_FILE}"
        EDITOR_CHECKSUM_FILE = "${EDITOR_JAR_FILE}.md5"
        OLD_SERVER_JAR_DIR = "./server-jars"
    }

    stages {
        stage('Prepare') {
            parallel {
                stage('Prepare Server') {
                    steps {
                        copyArtifacts([
                            projectName: "${SOURCE_BUILD_JOBNAME}",
                            selector: specific("${SOURCE_BUILD_NUMBER}"),
                            filter: "build/libs/${JAR_NAME}",
                        ])
                        sh 'cp "build/libs/${JAR_NAME}" "${DOWNLOADED_JAR_NAME}"'
                        sh 'rm -rf "build"'
                        script {
                            shouldLaunch = checkFileIsNew([
                                file = DOWNLOADED_JAR_NAME,
                                checksumFile = SERVER_CHECKSUM_FILE
                            ]) || sh([
                                label: 'Check if the server is running',
                                script: "curl --insecure ${APP_PROTOCOL}://localhost:${APP_PORT} | grep '${EXPECTED_RESPONSE}'",
                                returnStatus: true
                            ]) != 0
                            if (!shouldLaunch) {
                                sh 'rm "${DOWNLOADED_JAR_NAME}"'
                            }
                        }
                    }
                }
                stage('Prepare Editor') {
                    steps {
                        copyArtifacts([
                            projectName: "${SOURCE_BUILD_JOBNAME}",
                            selector: specific("${SOURCE_BUILD_NUMBER}"),
                            filter: "editor/build/**",
                        ])
                        script {
                            newEditorVersion = checkFileIsNew([file = EDITOR_JAR_PATH, checksumFile = EDITOR_CHECKSUM_FILE])
                        }
                    }
                }
            }
        }
        stage('Before Launch') {
            parallel {
                stage('Stop Server') {
                    when { expression { shouldLaunch == true } }
                    steps {
                        script {
                            processOutput = sh([
                                returnStdout: true,
                                script: "ps -C java -u '${env.USER}' -o pid=,command= | grep '${LAUNCH_COMMAND_IDENTIFYING_STRING}' | awk '{print \$1;}'"
                            ])
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
                        }
                        sh "mkdir -p 'old-logs' && mv ${LOG_FILE_PREFIX}-*.log ./old-logs || echo 'No logs to move.'"
                    }
                }
                stage('Editor') {
                    when { expression { newEditorVersion == true } }
                    steps {
                        sh './launch/add-world-editor-version.sh'
                        sh 'rm -rf "editor/build"'
                    }
                }
            }
        }
        stage('Launch') {
            when { expression { shouldLaunch == true } }
            steps {
                // https://devops.stackexchange.com/questions/1473/running-a-background-process-in-pipeline-job
                withEnv(['JENKINS_NODE_COOKIE=dontkill']) {
                    sh LAUNCH_COMMAND
                }
                retry(10) {
                    sleep 5
                    sh([
                        label: 'Check if the server is running',
                        script: "curl --insecure ${APP_PROTOCOL}://localhost:${APP_PORT} | grep '${EXPECTED_RESPONSE}'",
                    ])
                    sh([
                        label: 'Update the server checksum file',
                        script: "md5sum '${DOWNLOADED_JAR_NAME}'" + ' | awk \'{print $1;}\'' + " | tee '${SERVER_CHECKSUM_FILE}'"
                    ])
                }
                sh "if [ -e '${LOG_FILE}' ]; then tail -n 100 '${LOG_FILE}'; else " +
                    "echo 'The app does not have an output file ${LOG_FILE}!'; exit 1; fi"
            }
        }
        stage('Save Server JAR') {
            when { expression { shouldLaunch == true } }
            steps {
                sh "if ! [ -e '${OLD_SERVER_JAR_DIR}' ]; then mkdir -p '${OLD_SERVER_JAR_DIR}'; fi"
                sh "gzip '${DOWNLOADED_JAR_NAME}' && mv '${DOWNLOADED_JAR_NAME}.gz' '${OLD_SERVER_JAR_DIR}'"
            }
        }
    }

    post {
        always {
            archiveArtifacts([
                artifacts: LOG_FILE,
                allowEmptyArchive: true,
                fingerprint: true,
            ])
        }
    }
}

def checkFileIsNew(file, checksumFile) {
    status = sh([
        label: 'Check if the file ${file} is new',
        script: "FILE='${file}' CHECKSUM_FILE='${checksumFile}' ./launch/check-file-is-new.sh",
        returnStatus: true
    ])
    if (status == 0) {
        return true
    } else if (status == 10) {
        return false
    }
    error "check-file-is-new.sh has exited with a status other than 0 or 10: [${status}]"
}
