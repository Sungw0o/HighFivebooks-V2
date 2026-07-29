// HighFiveBooks V2 Jenkins CI
//
// Jenkins owns CI and Git image-tag updates.
// Argo CD owns cluster deployment. Jenkins must not run kubectl apply.

pipeline {
  agent {
    kubernetes {
      defaultContainer 'maven'
      yaml '''
apiVersion: v1
kind: Pod
spec:
  serviceAccountName: jenkins
  containers:
    - name: maven
      image: maven:3.9.11-eclipse-temurin-21
      command: ["sleep"]
      args: ["99d"]
      env:
        - name: MAVEN_OPTS
          value: "-Xmx256m -XX:MaxMetaspaceSize=192m"
      resources:
        requests:
          cpu: 250m
          memory: 512Mi
        limits:
          cpu: "1"
          memory: 1Gi
    - name: kaniko
      image: gcr.io/kaniko-project/executor:v1.23.2-debug
      command: ["/busybox/cat"]
      tty: true
      env:
        - name: AWS_REGION
          value: ap-northeast-2
        - name: AWS_SDK_LOAD_CONFIG
          value: "true"
      resources:
        requests:
          cpu: 100m
          memory: 128Mi
        limits:
          cpu: "1"
          memory: 1Gi
    - name: gitops
      image: alpine:3.21
      command: ["sleep"]
      args: ["99d"]
      resources:
        requests:
          cpu: 50m
          memory: 32Mi
        limits:
          cpu: 250m
          memory: 256Mi
'''
    }
  }

  options {
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
  }

  triggers {
    githubPush()
  }

  parameters {
    booleanParam(
      name: 'FORCE_BUILD_ALL',
      defaultValue: false,
      description: '변경 감지와 관계없이 모든 백엔드 서비스를 빌드하고 배포합니다'
    )
  }

  environment {
    AWS_ACCOUNT_ID = '756090160762'
    AWS_REGION = 'ap-northeast-2'
    ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
    IMAGE_NAMESPACE = 'highfivebooks-v2'
    GITOPS_DIR = 'k8s/overlays/aws'
    GIT_REPO = 'github.com/Sungw0o/HighFivebooks-V2.git'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        sh 'git config --global --add safe.directory "$WORKSPACE"'
        script {
          env.IMAGE_TAG = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
          echo "IMAGE_TAG=${env.IMAGE_TAG}"
        }
      }
    }

    stage('Detect changed services') {
      steps {
        script {
          def allServices = ['order', 'book', 'member', 'coupon', 'payment']
          def changedFiles = []

          try {
            changedFiles = sh(
              returnStdout: true,
              script: 'git diff --name-only HEAD~1 HEAD'
            ).readLines().collect { it.trim() }.findAll { it }
          } catch (ignored) {
            echo 'Could not detect the previous revision; building every service.'
          }

          def author = sh(returnStdout: true, script: 'git log -1 --pretty=%an').trim()
          def manifestOnly = changedFiles &&
            changedFiles.every { it == "${env.GITOPS_DIR}/kustomization.yaml" } &&
            author == 'jenkins-ci'

          if (manifestOnly) {
            env.SERVICES = ''
            currentBuild.result = 'NOT_BUILT'
            echo 'Skipping the Jenkins image-tag commit.'
            return
          }

          if (params.FORCE_BUILD_ALL) {
            env.SERVICES = allServices.join(',')
            echo "Forced full build: ${env.SERVICES}"
            return
          }

          if (!changedFiles) {
            env.SERVICES = allServices.join(',')
            return
          }

          def changedServices = allServices.findAll { svc ->
            changedFiles.any { path -> path.startsWith("services/${svc}-server/") }
          }

          if (changedFiles.any { it == 'Jenkinsfile' }) {
            changedServices = allServices
          }

          env.SERVICES = changedServices.join(',')
          echo changedServices ? "Building services: ${env.SERVICES}" : 'No backend service changes detected.'
        }
      }
    }

    stage('Build and test') {
      when {
        expression { env.SERVICES?.trim() }
      }
      steps {
        script {
          for (svc in env.SERVICES.split(',')) {
            dir("services/${svc}-server") {
              sh 'mvn -B -DargLine=-Xmx384m clean package'
            }
          }
        }
      }
    }

    stage('Build and push images') {
      when {
        expression { env.SERVICES?.trim() }
      }
      steps {
        script {
          for (svc in env.SERVICES.split(',')) {
            def image = "${env.ECR_REGISTRY}/${env.IMAGE_NAMESPACE}/${svc}-server:${env.IMAGE_TAG}"
            container('kaniko') {
              sh """
                /kaniko/executor \
                  --context '${env.WORKSPACE}/services/${svc}-server' \
                  --dockerfile '${env.WORKSPACE}/services/${svc}-server/Dockerfile' \
                  --destination '${image}' \
                  --cache=true \
                  --cache-repo '${env.ECR_REGISTRY}/${env.IMAGE_NAMESPACE}/kaniko-cache'
              """
            }
          }
        }
      }
    }

    stage('Commit image tags') {
      when {
        expression { env.SERVICES?.trim() }
      }
      steps {
        container('gitops') {
          withCredentials([
            usernamePassword(
              credentialsId: 'github-token',
              usernameVariable: 'GIT_USER',
              passwordVariable: 'GIT_TOKEN'
            )
          ]) {
            sh '''
              set -eu
              apk add --no-cache curl git
              git config --global --add safe.directory "$WORKSPACE"
              curl -fsSL \
                https://github.com/kubernetes-sigs/kustomize/releases/download/kustomize%2Fv5.7.1/kustomize_v5.7.1_linux_amd64.tar.gz \
                | tar -xz -C /usr/local/bin

              for svc in $(echo "$SERVICES" | tr ',' ' '); do
                (
                  cd "$GITOPS_DIR"
                  kustomize edit set image \
                    "ghcr.io/sungw0o/highfivebooks-v2/${svc}-server=${ECR_REGISTRY}/${IMAGE_NAMESPACE}/${svc}-server:${IMAGE_TAG}"
                )
              done

              git config user.email "jenkins@ci.local"
              git config user.name "jenkins-ci"
              git add "$GITOPS_DIR/kustomization.yaml"

              if git diff --cached --quiet; then
                echo "No manifest change."
                exit 0
              fi

              git commit -m "🚀 deploy: 이미지 태그를 ${IMAGE_TAG}로 갱신"
              git push "https://${GIT_USER}:${GIT_TOKEN}@${GIT_REPO}" HEAD:main
            '''
          }
        }
      }
    }
  }
}
