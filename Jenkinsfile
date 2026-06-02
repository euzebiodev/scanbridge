pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Test') {
            steps {
                sh './mvnw test'
            }
        }

        stage('Package') {
            steps {
                sh './mvnw -DskipTests package'
            }
        }

        stage('Deploy') {
            steps {
                sh 'sudo /opt/scanbridge/deploy-scanbridge.sh target/scanbridge-0.0.1-SNAPSHOT.jar'
            }
        }

        stage('Health') {
            steps {
                sh 'curl -fsS http://127.0.0.1:8080/actuator/health'
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
        }
    }
}
