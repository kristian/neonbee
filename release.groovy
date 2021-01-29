@Library(['piper-lib','piper-lib-os']) _

def getNeonBeeVersion() {
    return sh (
        script: "./gradlew properties -q | grep -w \"version:\" | awk '{print \$2}'",
        returnStdout: true
    ).trim()
}

try {
    milestone 1

    def neonbee_version
    stage('Checkout') {
        node {
            deleteDir()
            env.JAVA_HOME="${tool 'SapMachine Local'}"
            def refSpec = (params.GERRIT_REFSPEC).trim();

            git(
                branch: 'master',
                url: "git@github.wdf.sap.corp:forme/neonbee-core.git",
                credentialsId: 'GithubWDFNeonBeeSSHKey'
            )

            // Update the neonbee_version in build.gradle for the following build. This is needed here, because
            // in case of building an old version, we skip the "./gradlew prepareRelease" command.
            sh "./gradlew updateVersion"
            neonbee_version = getNeonBeeVersion()
            echo "NeonBee version to be build: ${neonbee_version}"
        }
    }
    stage("Test") {
        node {
            sh "./gradlew test javadoc"
            testsPublishResults(
                script: this,
                junit: [pattern: '**/test/*.xml']
            )
        }
    }
    def next_version
    def next_version_proposal = "0.0." + (Integer.parseInt(((neonbee_version.replace('-SNAPSHOT', '').replace('-sap-0', '')).split("[.]")[2])) + 1) + "-sap-0"
    stage("Release") {
        while (true) {
            echo "proposed next version: " + next_version_proposal
            next_version = input(
                message: 'Release and bump to version:',
                parameters: [
                    string(defaultValue: next_version_proposal, description: 'next version', name: 'next_version'),
                ]
            )
            if (neonbee_version == next_version.replace('-SNAPSHOT', '')) {
                echo "Next version must be different from current version"
                continue
            }
            if (!next_version.endsWith('-SNAPSHOT')) {
                next_version += '-SNAPSHOT'
            }
            break
        }
        // only allow one release at a time, to not cause issues with the -SNAPSHOT beeing removed
        lock(resource: 'release-neonbee') {
        milestone 2 // this will cancel older builds, that did not push a release
            node {
                // remove "-SNAPSHOT" from version to prepare release
                sh 'git config user.email "SAPforME_CI/CD@sap.com"'
                sh 'git config user.name "Leeroy Jenkins"'
                sh "./gradlew prepareRelease"
                neonbee_version = getNeonBeeVersion() // is this needed?
                executeBuild(
                    script: this,
                    buildType: 'xMakeStage',
                    xMakeJobName: 'forme-neonbee-core-OD-common',
                    xMakeBuildQuality: 'Milestone',
                    xMakeJobParameters: ['TREEISH=master'],
                    xMakeNovaCredentialsId: '84aa7791-ee36-42c0-b7af-157fd05a7e3c',
                    xMakeDownloadBuildResult: false,
                    gitCommitId: 'master'
                )
                executeBuild(
                    script: this,
                    buildType: 'xMakePromote',
                    xMakeJobName: 'forme-neonbee-core-OD-common',
                    xMakeBuildQuality: 'Milestone',
                    xMakeJobParameters: ['TREEISH=master'],
                    xMakeNovaCredentialsId: '84aa7791-ee36-42c0-b7af-157fd05a7e3c',
                    xMakeDownloadBuildResult: false,
                    gitCommitId: 'master'
                )
                // sh "./gradlew buildTrustStore sonarqube"

                // bump to next version and add -SNAPSHOT again
                sh "./gradlew finishRelease -PnextVersion='${next_version}'"
            }
        }
    }
} finally {
    node {
        // always cleanup workspace after we are done
        deleteDir()
    }
}