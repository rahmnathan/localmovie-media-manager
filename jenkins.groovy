node {
    def mvnHome
    def jdk
    stage('Setup') {
        mvnHome = tool 'Maven'
        jdk = tool name: 'Java 11'
        env.JAVA_HOME = "${jdk}"
    }
    stage('Checkout') {
        git 'https://github.com/rahmnathan/localmovie-media-manager.git'
    }
    stage('Set Version') {
        PROJECT_VERSION = sh(
                script: "'${mvnHome}/bin/mvn' help:evaluate -Dexpression=project.version -q -DforceStdout",
                returnStdout: true
        ).trim()
        env.NEW_VERSION = "${PROJECT_VERSION}.${BUILD_NUMBER}"
        sh "'${mvnHome}/bin/mvn' -DnewVersion='${NEW_VERSION}' versions:set"
    }
    stage('Tag') {
        sh 'git config --global user.email "rahm.nathan@gmail.com"'
        sh 'git config --global user.name "rahmnathan"'
        sshagent(credentials: ['Github-Git']) {
            sh 'mkdir -p /home/jenkins/.ssh'
            sh 'ssh-keyscan  github.com >> ~/.ssh/known_hosts'
            sh "'${mvnHome}/bin/mvn' -Dtag=${NEW_VERSION} scm:tag"
        }
    }
    stage('Package') {
        sh "'${mvnHome}/bin/mvn' clean package -DskipTests"
    }
    stage('Test') {
        sh "'${mvnHome}/bin/mvn' test"
    }
    stage('Docker Build') {
        sh "'${mvnHome}/bin/mvn' dockerfile:build"
    }
    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'Dockerhub',
                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        stage('Docker Push') {
            sh "'${mvnHome}/bin/mvn' dockerfile:push -Ddockerfile.username=$USERNAME -Ddockerfile.password='$PASSWORD'"
        }
    }
    stage('Deploy to Kubernetes') {
        withKubeConfig([credentialsId: 'Kube', serverUrl: 'https://192.168.1.8']) {
            sh 'kubectl apply -f my-kubernetes-directory/'
        }
    }
}