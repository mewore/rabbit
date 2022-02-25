pipeline {
    agent any
    tools {
        jdk 'openjdk-15.0.2'
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
                    tasksToRun = ['frontend:frontendLint', 'frontend:frontendTest', 'editor:jar', 'jar']
                    spotbugsCommands = []
                    for (javaModule in ['core', 'backend', 'editor']) {
                        tasksToRun.add(javaModule + ':spotbugsMain')
                        tasksToRun.add(javaModule + ':test')
                        spotbugsCommands.add(copySpotbugsReportCmd(javaModule))
                    }

                    sh './gradlew --parallel ' + tasksToRun.join(' ') + ' --no-daemon && ' +
                        spotbugsCommands.join(' && ')

                    taskTimeFile = 'build/task-time.txt'
                    if (fileExists(taskTimeFile)) {
                        currentBuild.description = readFile([encoding: 'UTF-8', file: taskTimeFile])
                    }
                }
            }
        }
        stage('JaCoCo Report') {
            steps {
                jacoco([
                    classPattern: '**/build/classes',
                    execPattern: '**/**.exec',
                    sourcePattern: '**/src/main/java',
                    exclusionPattern: [
                        '**/test/**/*.class',
                        '**/WorldEditor.class',
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
    }

    post {
        always {
            archiveArtifacts([
                artifacts: [
                    'build/libs/**/*.jar',
                    'editor/build/libs/**/*.jar',
                    ['core', 'editor', 'backend'].collect({it + '/build/reports/spotbugs/spotbugs-' + it + '.html'})
                ].flatten().join(','),
                fingerprint: true,
            ])
            publishHTML([
                keepAll: true,
                reportDir: 'build/reports/task-durations',
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
