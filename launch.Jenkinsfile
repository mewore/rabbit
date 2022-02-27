def shouldLaunch = true;

pipeline {
    agent {
        node LAUNCH_NODE
    }

    tools {
        jdk 'openjdk-15.0.2'
    }

    environment {
        DOWNLOADED_JAR_NAME = "${SOURCE_BUILD_JOBNAME}-${SOURCE_BUILD_NUMBER}-${JAR_NAME}"
        OLD_SERVER_CHECKSUM_FILE = "server-jar.md5"
        NEW_SERVER_CHECKSUM_FILE = "${OLD_SERVER_CHECKSUM_FILE}.tmp"
        LOG_FILE_PREFIX = "rabbit"
        LOG_FILE = "${LOG_FILE_PREFIX}-${env.BUILD_NUMBER}.log"
        APP_PROTOCOL = "http"
        APP_PORT = 8100
        LAUNCH_COMMAND = "nohup bash -c \"java -jar '${DOWNLOADED_JAR_NAME}' --rabbit.port=${APP_PORT} --rabbit.static.external=./static\" > '${LOG_FILE}' &"
        LAUNCH_COMMAND_IDENTIFYING_STRING = "rabbit.port="
        EXPECTED_RESPONSE = "<title>rabbit-frontend</title>"
        EDITOR_JAR_FILE = "editor.jar"
        OLD_SERVER_JAR_DIR = "./server-jars"
    }

    stages {
        stage('Prepare') {
            parallel {
                stage('Prepare Server') {
                    steps {
                        sh 'pwd'
                        copyArtifacts([
                            projectName: "${SOURCE_BUILD_JOBNAME}",
                            selector: specific("${SOURCE_BUILD_NUMBER}"),
                            filter: "build/libs/${JAR_NAME}",
                        ])
                        sh 'cp "build/libs/${JAR_NAME}" "${DOWNLOADED_JAR_NAME}"'
                        sh 'rm -rf "build"'
                        sh "md5sum '${DOWNLOADED_JAR_NAME}'" + ' | awk \'{print $1;}\'' +
                            " | tee '${NEW_SERVER_CHECKSUM_FILE}'"
                        script {
                            shouldLaunch = !fileExists(OLD_SERVER_CHECKSUM_FILE) \
                                || readFile(NEW_SERVER_CHECKSUM_FILE) != readFile(OLD_SERVER_CHECKSUM_FILE) \
                                || sh([
                                    label: 'Check if the server is running',
                                    script: "curl --insecure ${APP_PROTOCOL}://localhost:${APP_PORT} | grep '${EXPECTED_RESPONSE}'",
                                    returnStatus: true
                                ]) != 0
                            if (!shouldLaunch) {
                                sh 'rm "${NEW_SERVER_CHECKSUM_FILE}" && rm "${DOWNLOADED_JAR_NAME}"'
                            }
                        }
                    }
                }
                stage('WorldEditor Executables') {
                    steps {
                        sh 'rm -rf "editor/build"'
                        copyArtifacts([
                            projectName: "${SOURCE_BUILD_JOBNAME}",
                            selector: specific("${SOURCE_BUILD_NUMBER}"),
                            filter: "editor/build/**",
                        ])

                        sh './launch/add-world-editor-version.sh'
                        sh 'rm -rf "editor/build"'
                    }
                }
            }
        }
        stage('Stop') {
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
                        script: "mv '${NEW_SERVER_CHECKSUM_FILE}' '${OLD_SERVER_CHECKSUM_FILE}'"
                    ])
                }
                sh "if [ -e '${LOG_FILE}' ]; then tail -n 100 '${LOG_FILE}'; else " +
                    "echo 'The app does not have an output file ${LOG_FILE}!'; exit 1; fi"
            }
        }
        stage('Save server JAR') {
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
