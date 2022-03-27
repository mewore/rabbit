pipeline {
    agent any
    tools {
        jdk 'openjdk-15.0.2'
    }

    environment {
        LINUX_JAVA_PATH = "${env.HOME}/.jdks/jdk-15.0.2-lin64"
        WINDOWS_JAVA_PATH = "${env.HOME}/.jdks/jdk-15.0.2-win64"
        LAUNCH4J_PATH = "${env.HOME}/launch4j"
        LINUX_ROOT_DIR = "rabbit-world-editor"
        LINUX_ARCHIVE_NAME = "editor-lin64"
        WINDOWS_ROOT_DIR = "Rabbit World Editor"
        WINDOWS_ARCHIVE_NAME = "editor-win64"
    }

    stages {
        stage('Prepare') {
            steps {
                git([
                    branch: env.BRANCH == null ? 'main' : env.BRANCH,
                    credentialsId: 'mewore',
                    url: 'git@github.com:mewore/rabbit.git',
                ])
                sh 'java -version'
            }
        }
        stage('Build + Test') {
            steps {
                script {
                    tasksToRun = ['frontend:frontendCheckDisabledLintRules', 'frontend:frontendLint',
                        'editor:jar', 'jar', 'frontend:frontendTest', 'editor:packageAll']
                    spotbugsCommands = []
                    for (javaModule in ['core', 'backend', 'editor']) {
                        tasksToRun.add(javaModule + ':spotbugsMain')
                        tasksToRun.add(javaModule + ':test')
                        spotbugsCommands.add(copySpotbugsReportCmd(javaModule))
                    }

                    sh './gradlew --parallel ' + tasksToRun.join(' ') + ' --no-daemon && ' +
                        spotbugsCommands.join(' && ')
                }
            }
        }
        stage('Post-build') {
        parallel {
            stage('JaCoCo Report') {
                steps {
                    jacoco([
                        classPattern: '**/build/classes',
                        execPattern: '**/**.exec',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: [
                            '**/test/**/*.class',
                            '**/WorldEditor.class',
                            '**/ServerWithPreview.class',
                            '**/PlayerPreview.class',
                        ].join(','),

                        // 100% health at:
                        maximumBranchCoverage: '90',
                        maximumClassCoverage: '95',
                        maximumComplexityCoverage: '90',
                        maximumLineCoverage: '95',
                        maximumMethodCoverage: '95',
                        // 0% health at:
                        minimumBranchCoverage: '70',
                        minimumClassCoverage: '80',
                        minimumComplexityCoverage: '70',
                        minimumLineCoverage: '80',
                        minimumMethodCoverage: '80',
                    ])
                }
            }
            stage('Cobertura Report') {
                cobertura([
                    coberturaReportFile: '**/frontend/tests/coverage/cobertura-coverage.xml',
                    conditionalCoverageTargets: '90, 50, 0',
                    lineCoverageTargets: '95, 60, 0',
                    methodCoverageTargets: '95, 60, 0',
                ])
            }
        } // parallel { ... }
        } // Post-build { ... }
    }

    post {
        always {
            archiveArtifacts([
                artifacts: [
                    'build/libs/**/*.jar',
                    'editor/build/libs/**/*.jar',
                    'editor/build/executable/**/*.tar.gz',
                    'editor/build/executable/**/*.zip',
                    ['core', 'editor', 'backend'].collect({it + '/build/reports/spotbugs/spotbugs-' + it + '.html'})
                ].flatten().join(','),
                fingerprint: true,
            ])
            publishHTML([
                allowMissing: true,
                alwaysLinkToLastBuild: false,
                keepAll: true,
                reportDir: 'build/reports/task-durations',
                reportFiles: 'index.html',
                reportName: 'Task Durations',
                reportTitles: 'Task Durations'
            ])
        }
    }
}

def copySpotbugsReportCmd(module) {
    String dir = module + '/build/reports/spotbugs'
    return 'cp ' + dir + '/main.html ' + dir + '/spotbugs-' + module + '.html'
}
