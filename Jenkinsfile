import groovy.json.JsonOutput

def version = "UNKNOWN"
def commit = "UNKNOWN"
def repo = "UNKNOWN"

def pom(path, target) {
    return [pattern: "${path}/pom.xml", target: "${target}.pom"]
}

def jar(path, target) {
    return [pattern: "${path}/target/*.jar",
            target         : "${target}.jar",
            excludePatterns: ["*-exec.jar"]
            ]
}

def tar(path, target) {
    return [pattern: "${path}/target/*.tar.gz",
            target : "${target}-dist.tar.gz"]
}

def runjar(path, target) {
    return [pattern: "${path}/target/*-exec.jar",
            target : "${target}-exec.jar"]
}

pipeline {
    agent {
        kubernetes {
            label 'song-executor'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jdk
    tty: true
    image: openjdk:11
    env: 
      - name: DOCKER_HOST 
        value: tcp://localhost:2375 
  - name: dind-daemon 
    image: docker:18.06-dind
    securityContext: 
        privileged: true 
    volumeMounts: 
      - name: docker-graph-storage 
        mountPath: /var/lib/docker 
  - name: helm
    image: alpine/helm:2.12.3
    command:
    - cat
    tty: true
  - name: docker
    image: docker:18-git
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
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
                container('jdk') {
                    sh "./mvnw test package"
                }
            }
        }
        stage('Build & Publish Develop') {
            when {
                branch "develop"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker build --target=server --network=host -f Dockerfile . -t overture/song-server:edge -t overture/song-server:${commit}"
                    sh "docker build --target=client --network=host -f Dockerfile . -t overture/song-client:edge -t overture/song-client:${commit}"
                    sh "docker push overture/song-server:${commit}"
                    sh "docker push overture/song-server:edge"
                    sh "docker push overture/song-client:${commit}"
                    sh "docker push overture/song-client:edge"
                }
            }
        }
        stage('Release & tag') {
          when {
            branch "master"
          }
          steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId: 'OvertureBioGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh "git tag ${version}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/overture-stack/song --tags"
                    }
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker build --target=server --network=host -f Dockerfile . -t overture/song-server:latest -t overture/song-server:${version}"
                    sh "docker build --target=client --network=host -f Dockerfile . -t overture/song-client:latest -t overture/song-client:${version}"
                    sh "docker push overture/song-server:${version}"
                    sh "docker push overture/song-server:latest"
                    sh "docker push overture/song-client:${version}"
                    sh "docker push overture/song-client:latest"
                }
            }
        }

        stage('Destination SNAPSHOT') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'test-develop'
                }
            }
            steps {
                script {
                    repo = "dcc-snapshot/bio/overture"
                }
            }
        }

        stage('Destination release') {
            when {
                anyOf {
                    branch 'master'
                    branch 'test-master'
                }
            }
            steps {
                script {
                    repo = "dcc-release/bio/overture"
                }
            }
        }

        stage('Upload Artifacts') {
            when {
                anyOf {
                    branch 'master'
                    branch 'test-master'
                    branch 'develop'
                    branch 'test-develop'
                }
            }
            steps {
                script {
                    
                    project = "song"
                    versionName = "$version"
                    subProjects = ['client', 'core', 'server']

                    files = []
                    files.add([pattern: "pom.xml", target: "$repo/$project/$versionName/$project-${versionName}.pom"])

                    for (s in subProjects) {
                        name = "${project}-$s"
                        target = "$repo/$name/$versionName/$name-$versionName"
                        files.add(pom(name, target))
                        files.add(jar(name, target))

                        if (s in ['client', 'server']) {
                            files.add(runjar(name, target))
                            files.add(tar(name, target))
                        }
                    }

                    fileSet = JsonOutput.toJson([files: files])
                    pretty = JsonOutput.prettyPrint(fileSet)
                    print("Uploading files=${pretty}")
                }

                rtUpload(serverId: 'artifactory', spec: fileSet)
            }
        }
    }
}
