pipeline {
    agent {
        kubernetes {
            yaml """
kind: Pod
metadata:
  name: jenkins-agent
spec:
  containers:
  - name: jnlp
    image: rahmnathan/jnlp-slave:4.6-1
    imagePullPolicy: Always
    tty: true
    volumeMounts:
      - name: docker
        mountPath: /var/run/docker.sock
  volumes:
    - name: docker
      hostPath:
        path: '/var/run/docker.sock'
"""
        }
    }

    tools {
        maven 'Maven'
        jdk 'Java 11'
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    git 'https://github.com/rahmnathan/localmovie-media-manager.git'
                }
            }
        }
        stage('Set Version') {
            steps {
                script {
                    PROJECT_VERSION = sh(
                            script: "mvn help:evaluate -Dexpression=project.version -q -DforceStdout",
                            returnStdout: true
                    ).trim()
                    env.NEW_VERSION = "${PROJECT_VERSION}.${BUILD_NUMBER}"
                    sh "mvn -DnewVersion='${NEW_VERSION}' versions:set"
                }
            }
        }
        stage ('Parallel') {
            parallel {
                stage('Tag') {
                    steps {
                        script {
                            sh 'git config --global user.email "rahm.nathan@gmail.com"'
                            sh 'git config --global user.name "rahmnathan"'
                            sshagent(credentials: ['Github-Git']) {
                                sh 'mkdir -p /home/jenkins/.ssh'
                                sh 'ssh-keyscan  github.com >> ~/.ssh/known_hosts'
                                sh "mvn -Dtag=${NEW_VERSION} scm:tag"
                            }
                        }
                    }
                }
                stage('Unit Test') {
                    steps {
                        sh 'mvn test'
                    }
                }
            }
        }
        stage('Package & Deploy Jar to Artifactory') {
            steps {
                script {
                    server = Artifactory.server 'Artifactory'
                    rtMaven = Artifactory.newMavenBuild()
                    rtMaven.tool = 'Maven'
                    rtMaven.deployer releaseRepo: 'rahmnathan-services', snapshotRepo: 'rahmnathan-services', server: server

                    buildInfo = Artifactory.newBuildInfo()

                    rtMaven.run pom: 'pom.xml', goals: 'clean install -DskipTests', buildInfo: buildInfo
                }
            }
        }
        stage('Docker Build') {
            steps {
                sh "mvn dockerfile:build"
            }
        }
        stage('Docker Push') {
            environment {
                DOCKERHUB = credentials('Dockerhub')
                VAULT_TOKEN = credentials('VaultToken')
            }
            steps {
                sh "mvn dockerfile:push -Ddockerfile.username=$DOCKERHUB_USR -Ddockerfile.password='$DOCKERHUB_PSW'"
            }
        }
        stage('Deploy to Kubernetes') {
            environment {
                KUBE_CONFIG = credentials('Kubeconfig')
                VAULT_TOKEN = credentials('VaultToken')
            }
            steps {
                sh 'helm upgrade --install -n localmovies localmovies ./target/classes/localmovies/ --set localmovies.vaultToken=$VAULT_TOKEN --kubeconfig $KUBE_CONFIG'
            }
        }
        stage('Wait for Deployment') {
            steps {
                script {
                    sh 'sleep 60s'
                }
            }
        }
        stage('Functional Test') {
            steps {
                script {
                    try {
                        withCredentials([usernamePassword(credentialsId: 'LocalMoviesCredentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            sh 'jmeter -n -t src/test/jmeter/localmovie-web-test.jmx -Jusername=$USERNAME -Jpassword=$PASSWORD'
                        }
                    } catch (e) {
                        stage('Rollback') {
                            withCredentials([file(credentialsId: 'Kubeconfig', variable: 'KUBE_CONFIG')]) {
                                sh 'helm -n localmovies rollback localmovies 0 --kubeconfig $KUBE_CONFIG'
                            }
                        }

                        throw e
                    }
                }
            }
        }
    }
    post {
        always {
            junit 'target/surefire-reports/*.xml'
        }
    }
}