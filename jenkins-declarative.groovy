pipeline {
    agent {
        docker {
            image 'rahmnathan/jnlp-slave:4.6-1'
            label 'builder'
            args  '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    def mvnHome
    def jdk
    def server
    def buildInfo
    def rtMaven

    stages {
        stage('Setup') {
            steps {
                script {
                    mvnHome = tool 'Maven'
                    jdk = tool name: 'Java 11'
                    env.JAVA_HOME = "${jdk}"

                    server = Artifactory.server 'Artifactory'

                    rtMaven = Artifactory.newMavenBuild()
                    rtMaven.tool = 'Maven'
                    rtMaven.deployer releaseRepo: 'rahmnathan-services', snapshotRepo: 'rahmnathan-services', server: server

                    buildInfo = Artifactory.newBuildInfo()
                }
            }
        }
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
                            script: "'${mvnHome}/bin/mvn' help:evaluate -Dexpression=project.version -q -DforceStdout",
                            returnStdout: true
                    ).trim()
                    env.NEW_VERSION = "${PROJECT_VERSION}.${BUILD_NUMBER}"
                    sh "'${mvnHome}/bin/mvn' -DnewVersion='${NEW_VERSION}' versions:set"
                }
            }
        }
        stage('Tag') {
            steps {
                script {
                    sh 'git config --global user.email "rahm.nathan@gmail.com"'
                    sh 'git config --global user.name "rahmnathan"'
                    sshagent(credentials: ['Github-Git']) {
                        sh 'mkdir -p /home/jenkins/.ssh'
                        sh 'ssh-keyscan  github.com >> ~/.ssh/known_hosts'
                        sh "'${mvnHome}/bin/mvn' -Dtag=${NEW_VERSION} scm:tag"
                    }
                }
            }
        }
        stage('Tag') {
            steps {
                script {
                    sh 'git config --global user.email "rahm.nathan@gmail.com"'
                    sh 'git config --global user.name "rahmnathan"'
                    sshagent(credentials: ['Github-Git']) {
                        sh 'mkdir -p /home/jenkins/.ssh'
                        sh 'ssh-keyscan  github.com >> ~/.ssh/known_hosts'
                        sh "'${mvnHome}/bin/mvn' -Dtag=${NEW_VERSION} scm:tag"
                    }
                }
            }
        }
        stage('Unit Test') {
            steps {
                script {
                    sh "'${mvnHome}/bin/mvn' test"
                }
            }
        }
        stage('Package & Deploy Jar to Artifactory') {
            steps {
                script {
                    rtMaven.run pom: 'pom.xml', goals: 'clean install -DskipTests', buildInfo: buildInfo
                }
            }
        }
        stage('Docker Build') {
            steps {
                script {
                    sh "'${mvnHome}/bin/mvn' dockerfile:build"
                }
            }
        }
        stage('Docker Push') {
            steps {
                script {
                    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'Dockerhub',
                                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        sh "'${mvnHome}/bin/mvn' dockerfile:push -Ddockerfile.username=$USERNAME -Ddockerfile.password='$PASSWORD'"
                    }
                }
            }
        }
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    withCredentials([
                            file(credentialsId: 'Kubeconfig', variable: 'KUBE_CONFIG'),
                            string(credentialsId: 'VaultToken', variable: 'VAULT_TOKEN')
                    ]) {
                        sh 'helm upgrade --install -n localmovies localmovies ./target/classes/localmovies/ --set localmovies.vaultToken=$VAULT_TOKEN --kubeconfig $KUBE_CONFIG'
                    }
                }
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
}