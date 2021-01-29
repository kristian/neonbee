@Library(['piper-lib','piper-lib-os']) _

try {
    milestone 1

    stage('Checkout') {
        node {
            deleteDir()
            env.JAVA_HOME="${tool 'SapMachine Local'}"
            git(
                branch: 'master',
                url: "git@github.wdf.sap.corp:forme/neonbee-core.git",
                credentialsId: 'GithubWDFNeonBeeSSHKey'
            )
        }
    }
    stage("Test") {
        node {
            sh "./gradlew test javadoc"
            testsPublishResults(script: this,
                junit: [pattern: '**/test/*.xml']
            )
        }
    }
} finally {
    node {
        deleteDir()
    }
}