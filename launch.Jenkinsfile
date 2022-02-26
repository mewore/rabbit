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
        LAUNCH_COMMAND = "nohup bash -c \"java -jar '${DOWNLOADED_JAR_NAME}' --rabbit.port=${APP_PORT} --rabbit.static.external=./static\" > '${LOG_FILE}' &"
        LAUNCH_COMMAND_IDENTIFYING_STRING = "rabbit.port="
        EXPECTED_RESPONSE = "<title>rabbit-frontend</title>"
        EDITOR_CHECKSUM_FILE = "editor.md5"
        TMP_EDITOR_CHECKSUM_FILE = "editor-tmp.md5"
    }

    stages {
        stage('Prepare') {
            parallel {
                stage('Prepare Server') {
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
                stage('WorldEditor Executables') {
                    steps {
                        script {
                            copyArtifacts([
                                projectName: "${SOURCE_BUILD_JOBNAME}",
                                selector: specific("${SOURCE_BUILD_NUMBER}"),
                                filter: "editor/build/**",
                            ])

                            sh 'md5sum \'editor/build/libs/editor.jar\' | awk \'{print $1;}\' > \'' +
                                TMP_EDITOR_CHECKSUM_FILE + '\''
                            if (!fileExists(EDITOR_CHECKSUM_FILE)
                                    || readFile(TMP_EDITOR_CHECKSUM_FILE) != readFile(EDITOR_CHECKSUM_FILE)) {
                                TARGET_DIR = "./static/editors/${SOURCE_BUILD_NUMBER}"
                                sh 'if [ -e ./static/editor ] && ! [ -e ./static/editors ]; then mv ./static/editor ./static/editors; fi'
                                sh 'if ! [ -e \'' + TARGET_DIR + '\' ]; then mkdir -p \'' + TARGET_DIR + '\'; fi'
                                sh 'mv editor/build/libs/editor.jar \'' + TARGET_DIR + '/rabbit-world-editor.jar\''
                                sh 'mv editor/build/executable-jar/linux64/editor-lin64.tar.gz \'' + TARGET_DIR +
                                    '/rabbit-world-editor-v' + SOURCE_BUILD_NUMBER + '-lin64.tar.gz\''
                                sh 'mv editor/build/executable-jar/win64/editor-win64.zip \'' + TARGET_DIR +
                                    '/Rabbit World Editor v' + SOURCE_BUILD_NUMBER + ' - win64.zip\''
                            }
                            sh 'mv \'' + TMP_EDITOR_CHECKSUM_FILE + '\' \'' + EDITOR_CHECKSUM_FILE + '\''
                        }
                    }
                }
            }
        }
        stage('Stop') {
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
