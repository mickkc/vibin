pipeline {
    agent any

    environment {
        IMAGE = 'mickkc/vibin'
        GRADLE_OPTS = '-Dorg.gradle.daemon=false'
    }

    stages {
        stage('Validate') {
            steps {
                script {
                    if (!env.TAG_NAME) {
                        error('This pipeline must be triggered by a git tag.')
                    }
                }
            }
        }

        stage('Test') {
            steps {
                dir('backend') {
                    sh './gradlew test'
                }
            }
            post {
                always {
                    junit 'backend/**/build/test-results/test/*.xml'
                    recordCoverage tools: [[parser: 'JACOCO', pattern: 'backend/**/build/reports/jacoco/test/jacocoTestReport.xml']],
                                   sourceCodeRetention: 'LAST_BUILD'
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    docker.build("${IMAGE}:${TAG_NAME}", "--build-arg VIBIN_TAG=${TAG_NAME} .")
                }
            }
        }

        stage('Push') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                        def img = docker.image("${IMAGE}:${TAG_NAME}")
                        img.push()
                        img.push('latest')
                    }
                }
            }
        }
    }

    post {
        always {
            sh "docker rmi ${IMAGE}:${TAG_NAME} || true"
        }
    }
}
