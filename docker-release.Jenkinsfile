import java.text.SimpleDateFormat

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

        stage('Build & Push') {
            parallel {
                stage('amd64') {
                    steps {
                        script {
                            def img = docker.build("${IMAGE}:${TAG_NAME}-amd64", "--build-arg VIBIN_TAG=${TAG_NAME} .")
                            docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                                img.push()
                            }
                        }
                    }
                    post {
                        always {
                            sh "docker rmi ${IMAGE}:${TAG_NAME}-amd64 || true"
                        }
                    }
                }

                stage('arm64') {
                    agent { label 'macos' }
                    steps {
                        checkout scm
                        script {
                            def img = docker.build("${IMAGE}:${TAG_NAME}-arm64", "--build-arg VIBIN_TAG=${TAG_NAME} .")
                            docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                                img.push()
                            }
                        }
                    }
                    post {
                        always {
                            sh "docker rmi ${IMAGE}:${TAG_NAME}-arm64 || true"
                        }
                    }
                }
            }
        }

        stage('Create multi-arch manifest') {
            steps {
                withDockerRegistry(url: 'https://index.docker.io/v1/', credentialsId: 'dockerhub-credentials') {
                    sh """
                        docker buildx imagetools create \
                            --tag ${IMAGE}:${TAG_NAME} \
                            --tag ${IMAGE}:latest \
                            ${IMAGE}:${TAG_NAME}-amd64 \
                            ${IMAGE}:${TAG_NAME}-arm64
                    """
                }
            }
        }

        stage("Create Codeberg Release") {
            steps {
                withCredentials([string(credentialsId: 'RELEASE_CODEBERG_TOKEN', variable: 'RELEASE_CODEBERG_TOKEN')]) {
                    script {
                        def date = new Date()
                        def sdf = new SimpleDateFormat("yyyy-MM-dd")

                        def currentHash = env.GIT_COMMIT
                        def tagName = env.TAG_NAME

                        def commitList = sh(
                            script: "git --no-pager log \$(git describe --tags --abbrev=0 \"${currentHash}^\")..${currentHash} --oneline --merges --color=never --reverse | awk '{print \"- \"\$0}'",
                            returnStdout: true
                        ).trim()

                        def body = readFile 'RELEASE.md'
                        body = body.replace('{version}', tagName).replace('{date}', sdf.format(date)).replace('{commits}', commitList)

                        def repoUrl = env.GIT_URL
                        def parts = repoUrl.split('/')
                        def owner = parts[-2]
                        def repo = parts[-1].replace('.git', '')

                        echo "Repo URL: ${repoUrl}, Owner: ${owner}, Repo: ${repo}"

                        def json = groovy.json.JsonOutput.toJson([
                            body: body,
                            draft: true,
                            name: tagName,
                            prerelease: true,
                            tag_name: tagName,
                            target_commitish: currentHash
                        ])

                        writeFile file: 'payload.json', text: json

                        def response = sh(
                            script: """
                                curl -sS --fail-with-body -X POST \
                                    -H "Authorization: token \$RELEASE_CODEBERG_TOKEN" \
                                    -H "Content-Type: application/json" \
                                    -d @payload.json \
                                    "https://codeberg.org/api/v1/repos/${owner}/${repo}/releases"
                            """,
                            returnStdout: true
                        )
                    }
                }
            }
        }
    }

    post {
        always {
            sh "rm -f payload.json"
        }
    }
}
