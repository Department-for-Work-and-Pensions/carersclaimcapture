node ('master') {
    def server = Artifactory.server('newlab-artifactory')
    def artifactoryGradle = Artifactory.newGradleBuild()
    artifactoryGradle.usesPlugin = true
    artifactoryGradle.tool = 'Gradle321' // Tool name from Jenkins configuration
    if (env.BRANCH_NAME == 'integration') {
        artifactoryGradle.deployer repo:'libs-snapshot-local', server: server
    }
    if (env.BRANCH_NAME == 'int-release') {
        artifactoryGradle.deployer repo:'libs-release-local', server: server
    }
    artifactoryGradle.resolver repo:'repo', server: server
    artifactoryGradle.deployer.deployMavenDescriptors = true
    artifactoryGradle.deployer.deployIvyDescriptors = true
    artifactoryGradle.deployer.ivyPattern = '[organisation]/[module]/ivy-[revision].xml'
    artifactoryGradle.deployer.artifactPattern = '[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]'
    artifactoryGradle.deployer.mavenCompatible = true

    def buildInfo = Artifactory.newBuildInfo()
    buildInfo.env.capture = true
    buildInfo.env.filter.addInclude("")
    buildInfo.env.filter.addExclude("DONT_COLLECT*")
    
    def artifactoryMaven = Artifactory.newMavenBuild()
    artifactoryMaven.resolver releaseRepo:'repo', snapshotRepo:'repo', server: server


    stage ('Checkout') {
        checkout scm
    }

    def gradletool = tool name: 'Gradle321', type: 'hudson.plugins.gradle.GradleInstallation'
    def gradlebin = "${gradletool}/bin/gradle -Dgradle.user.home=$JENKINS_HOME/.gradle --no-daemon"

    def app_name = sh (
        script: "${gradlebin} properties | grep name | awk '{print \$2}'",
        returnStdout: true
        ).trim()
    def app_ver = sh (
        script: "${gradlebin} properties | grep version | awk '{print \$2}'",
        returnStdout: true
        ).trim()

    stage ('Gradle build and test') {
        try {
            withEnv(['_JAVA_OPTIONS=-Dcarers.keystore=/opt/carers-keystore/carerskeystore']) {
                artifactoryGradle.run switches: '-Dgradle.user.home=$JENKINS_HOME/.gradle --no-daemon', buildFile: 'build.gradle', tasks: 'clean test build sourcesJar artifactoryPublish', buildInfo: buildInfo, server: server
            }
            publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'build/reports/tests/test/', reportFiles: 'index.html', reportName: 'Test Report'])
            junit keepLongStdio: true, testResults: 'build/test-results/test/*.xml'
        } catch (Exception e) {
            publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'build/reports/tests/test/', reportFiles: 'index.html', reportName: 'Test Report'])
            junit allowEmptyResults: true, keepLongStdio: true, testResults: 'build/test-results/test/*.xml'
            throw e;
        }
        
    }
    stage ('Publish build info') {
        server.publishBuildInfo buildInfo
    }
    stage ('Build service RedHat package') {
        sh "fpm -s dir -t rpm --name ${app_name}-${app_ver} --version ${env.BUILD_NUMBER} --prefix /data/carers/${app_name}/${app_name}-${app_ver} build/libs/${app_name}-${app_ver}-full.war=/"
    }
    if (env.BRANCH_NAME == 'integration') {
        stage ('Deploy to lab') {
            sshagent(['8b4a081b-f1d6-424d-959f-ae9279d08b3b']) {
                sh 'scp build/libs/c3-*-SNAPSHOT-full.war c3javalab@37.26.89.94:c3java-latest-SNAPSHOT-full.war'
                sh 'ssh c3javalab@37.26.89.94 "./deploy.sh restart > output.log 2>&1 &"'
            }
        }
        stage ('Add RedHat package to Lab repo') {
            sh 'cp *.rpm /opt/repo/cads/lab/'
            build job: 'Update repository metadata', parameters: [string(name: 'REPO_NAME', value: 'lab')], wait: false
        }
    }
    if (env.BRANCH_NAME == 'int-release') {
        stage ('Add RedHat package to Preview repo') {
            sh 'cp *.rpm /opt/repo/cads/preview/'
            build job: 'Update repository metadata', parameters: [string(name: 'REPO_NAME', value: 'preview')], wait: false
        }
    }
}
