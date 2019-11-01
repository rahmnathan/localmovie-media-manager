node {
   def mvnHome
   stage('Checkout') {
      git 'https://github.com/rahmnathan/localmovie-media-manager.git'  
      mvnHome = tool 'Maven'
   }
   stage('Compile') {
      jdk = tool name: 'Java 11'
      env.JAVA_HOME = "${jdk}"
      sh "'${mvnHome}/bin/mvn' clean install"
   }
   stage('Docker Build') {
      jdk = tool name: 'Java 11'
      env.JAVA_HOME = "${jdk}"
      sh "'${mvnHome}/bin/mvn' dockerfile:build"
   }
    withCredentials([[$class          : 'UsernamePasswordMultiBinding', credentialsId: 'Dockerhub',
                      usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
        stage('Docker Push') {

            sh "'${mvnHome}/bin/mvn' dockerfile:push -Ddockerfile.username=$USERNAME -Ddockerfile.password='$PASSWORD'"
        }
    }
}
