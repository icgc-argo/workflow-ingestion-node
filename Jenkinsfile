def dockerRepo = "ghcr.io/icgc-argo/workflow-graph-ingest-node"
def gitHubRepo = "icgc-argo/workflow-graph-ingest-node"
def commit = "UNKNOWN"
def version = "UNKNOWN"


pipeline {
    agent {
        kubernetes {
            label 'wfg-ingest'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: graalvm
      command: ['cat']
      tty: true
      image: icgcargo/graalvm:java11-20.2.0-extras-1.0.0
    - name: dind-daemon
      image: docker:18.06-dind
      securityContext:
        privileged: true
        runAsUser: 0
      volumeMounts:
        - name: docker-graph-storage
          mountPath: /var/lib/docker
    - name: docker
      image: docker:18-git
      tty: true
      env:
        - name: DOCKER_HOST
          value: tcp://localhost:2375
  securityContext:
    runAsUser: 1000
  volumes:
    - name: docker-graph-storage
      emptyDir: {}
"""
        }
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    commit = sh(returnStdout: true, script: 'git describe --always').trim()
                }
                script {
                    version = readMavenPom().getVersion()
                }
            }
        }
        stage('Test') {
            steps {
                container('graalvm') {
                    sh "./mvnw test"
                }
            }
        }
        stage('Build Artifact & Publish') {
             when {
                anyOf {
                    branch "master"
                    branch "develop"
                }
            }
            steps {
                container('graalvm') {
                    configFileProvider(
                        [configFile(fileId: '894c5ba8-e7cf-4465-98d4-b213eeaa77ef', variable: 'MAVEN_SETTINGS')]) {
                        sh './mvnw -s $MAVEN_SETTINGS clean package deploy'
                    }
                }
            }
        }
        stage('Build & Publish Edge') {
            when {
                branch "develop"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login ghcr.io -u $USERNAME -p $PASSWORD'
                    }

                    // DNS error if --network is default
                    sh "docker build --network=host . -t ${dockerRepo}:edge -t ${dockerRepo}:${commit}"

                    sh "docker push ${dockerRepo}:${commit}"
                    sh "docker push ${dockerRepo}:edge"
                }
            }
        }
        stage('Build & Publish Latest') {
            when {
                branch "master"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'argoContainers', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login ghcr.io -u $USERNAME -p $PASSWORD'
                    }

                    // DNS error if --network is default
                    sh "docker build --network=host . -t ${dockerRepo}:latest -t ${dockerRepo}:${version}"

                    sh "docker push ${dockerRepo}:${version}"
                    sh "docker push ${dockerRepo}:latest"
                }
            }
        }
        stage('Add new tag') {
            when {
                branch "master"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId: 'argoGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh "git tag ${version}"
                      sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${gitHubRepo} --tags"
                    }
                }
            }
        }
    }
}
