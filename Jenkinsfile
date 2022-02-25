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
        stage('Core') {
            steps {
                script {
                    sh './gradlew core:spotbugsMain core:test --no-daemon && ' + copySpotbugsReportCmd('core')
                }
                jacoco(jacocoConfig('core'))
            }
        }
        stage('Editor') {
            steps {
                script {
                    sh './gradlew editor:spotbugsMain editor:test editor:jar --no-daemon && ' +
                        copySpotbugsReportCmd('editor')
                }
                jacoco(jacocoConfig('editor', ['**/WorldEditor.class']))
            }
        }
        stage('Backend') {
            steps {
                script {
                    sh './gradlew backend:spotbugsMain backend:test --no-daemon && ' + copySpotbugsReportCmd('backend')
                }
                jacoco(jacocoConfig('backend'))
            }
        }
        stage('Frontend') {
            steps {
                script {
                    sh './gradlew frontend:frontendLint frontend:frontendBuildProd frontend:frontendTest' +
                        ' --no-daemon'
                }
            }
        }
        stage('Jar') {
            steps {
                script {
                    sh './gradlew jar --no-daemon'
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts([
                artifacts: [
                    'build/libs/**/*.jar',
                    'editor/build/libs/**/*.jar',
                    ['core', 'editor', 'backend'].map({it + '/build/reports/spotbugs/spotbugs-' + it + '.html'})
                ].flatten().join(','),
                fingerprint: true,
            ])
        }
    }
}

def jacocoConfig(module, excluded = []) {
    return [
        classPattern: '**/' + module + '/build/classes',
        execPattern: '**/**.exec',
        sourcePattern: '**/' + module + '/src/main/java',
        exclusionPattern: [
            '**/test/**/*.class',
        ].plus(excluded).join(','),

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
    ]
}

def copySpotbugsReportCmd(module) {
    String dir = module + '/build/reports/spotbugs'
    return 'cp ' + dir + '/main.html ' + dir + '/spotbugs-' + module + '.html'
}
