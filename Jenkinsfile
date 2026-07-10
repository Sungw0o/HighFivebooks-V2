// HighFiveBooks V2 Jenkins CI
//
// Jenkins owns CI only:
//   1. detect changed services
//   2. build and test those services
//   3. build and push container images
//   4. commit Kubernetes manifest image tag updates
//
// Argo CD owns deployment. This pipeline must not run kubectl apply.

pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  environment {
    REGISTRY = 'ghcr.io'
    IMAGE_NAMESPACE = 'sungw0o/highfivebooks-v2'
    GITOPS_DIR = 'k8s/base'
    GIT_REPO = 'github.com/Sungw0o/HighFivebooks-V2.git'
    GIT_BRANCH = 'main'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        script {
          env.IMAGE_TAG = bat(returnStdout: true, script: '@git rev-parse --short HEAD').trim()
          echo "IMAGE_TAG=${env.IMAGE_TAG}"
        }
      }
    }

    stage('Detect changed services') {
      steps {
        script {
          def allServices = ['order', 'book', 'member', 'coupon', 'payment']
          def changedFiles = []
          def detectionFailed = false

          try {
            changedFiles = bat(returnStdout: true, script: '@git diff --name-only HEAD~1 HEAD')
              .readLines()
              .collect { it.trim().replace('\\', '/') }
              .findAll { it }
          } catch (ignored) {
            detectionFailed = true
            echo 'Could not detect changed files; building every service.'
          }

          def author = bat(returnStdout: true, script: '@git log -1 --pretty=%an').trim()
          def manifestOnly = changedFiles &&
            changedFiles.every { it == "${env.GITOPS_DIR}/kustomization.yaml" } &&
            author == 'jenkins-ci'

          if (manifestOnly) {
            env.SERVICES = ''
            currentBuild.result = 'NOT_BUILT'
            echo 'Skipping Jenkins manifest tag update commit.'
            return
          }

          if (detectionFailed || !changedFiles) {
            env.SERVICES = allServices.join(',')
            echo "Building services: ${env.SERVICES}"
            return
          }

          def changedServices = allServices.findAll { svc ->
            changedFiles.any { path -> path.startsWith("services/${svc}-server/") }
          }

          if (changedFiles.any { it == 'Jenkinsfile' }) {
            changedServices = allServices
          }

          env.SERVICES = changedServices.join(',')
          echo changedServices ? "Building services: ${env.SERVICES}" : 'No service changes detected.'
        }
      }
    }

    stage('Build & Test') {
      when {
        expression { return env.SERVICES?.trim() }
      }
      steps {
        script {
          for (svc in env.SERVICES.split(',')) {
            dir("services/${svc}-server") {
              bat '.\\mvnw.cmd -B clean package'
            }
          }
        }
      }
    }

    stage('Image Build & Push') {
      when {
        allOf {
          branch 'main'
          expression { return env.SERVICES?.trim() }
        }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'ghcr-creds', usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
          script {
            bat 'echo %REG_PASS%| docker login %REGISTRY% -u %REG_USER% --password-stdin'
            for (svc in env.SERVICES.split(',')) {
              def image = "${env.REGISTRY}/${env.IMAGE_NAMESPACE}/${svc}-server:${env.IMAGE_TAG}"
              dir("services/${svc}-server") {
                bat "docker build -t ${image} ."
                bat "docker push ${image}"
              }
            }
          }
        }
      }
    }

    stage('Manifest Tag Update') {
      when {
        allOf {
          branch 'main'
          expression { return env.SERVICES?.trim() }
        }
      }
      steps {
        withCredentials([usernamePassword(credentialsId: 'git-creds', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
          script {
            dir(env.GITOPS_DIR) {
              for (svc in env.SERVICES.split(',')) {
                def name = "${env.REGISTRY}/${env.IMAGE_NAMESPACE}/${svc}-server"
                bat "kustomize edit set image ${name}=${name}:${env.IMAGE_TAG}"
              }
            }

            bat 'git config user.email "jenkins@ci.local"'
            bat 'git config user.name "jenkins-ci"'
            bat "git add ${env.GITOPS_DIR}/kustomization.yaml"
            bat "git diff --cached --quiet && echo no manifest change || git commit -m \"deploy: update image tags to ${env.IMAGE_TAG}\""
            bat "git push https://%GIT_USER%:%GIT_PASS%@${env.GIT_REPO} HEAD:${env.GIT_BRANCH}"
          }
        }
      }
    }
  }

  post {
    always {
      bat 'docker logout %REGISTRY% || echo skip'
    }
  }
}
