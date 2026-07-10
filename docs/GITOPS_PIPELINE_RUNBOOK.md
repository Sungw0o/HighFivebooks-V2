# GitOps Pipeline Runbook: Jenkins CI + Argo CD

## Goal

HighFiveBooks V2 uses Jenkins for CI and GitOps preparation only.

Jenkins is responsible for:

- detecting changed services in the monorepo
- running the selected service builds and tests
- building and pushing service images
- updating image tags in `k8s/base/kustomization.yaml`

Jenkins must not deploy directly to Kubernetes. Argo CD watches the Git state and performs the actual sync.

## Jenkins Prerequisites

Install these on the Jenkins agent:

- JDK 21
- Docker
- Git
- Kustomize CLI

Register these Jenkins credentials:

| Credential ID | Type | Purpose |
| --- | --- | --- |
| `ghcr-creds` | Username with password | GHCR username and PAT with `write:packages` |
| `git-creds` | Username with password | GitHub username and PAT with repository push permission |

## Pipeline Behavior

1. Jenkins checks out the repository.
2. Jenkins derives `IMAGE_TAG` from the short Git SHA.
3. Jenkins detects changed service paths under `services/*-server`.
4. Jenkins runs `mvnw.cmd -B clean package` only for selected services.
5. Jenkins builds and pushes images to `ghcr.io/sungw0o/highfivebooks-v2/<service>-server:<IMAGE_TAG>`.
6. Jenkins runs `kustomize edit set image` in `k8s/base`.
7. Jenkins commits the changed `kustomization.yaml` back to `main`.
8. Argo CD detects the Git change and syncs the cluster.

The pipeline skips Jenkins-authored commits that only update `k8s/base/kustomization.yaml`, preventing an infinite tag-update loop.

## Image Tag Update Point

`k8s/base/kustomization.yaml` owns the deployable image tags:

```yaml
images:
  - name: ghcr.io/sungw0o/highfivebooks-v2/order-server
    newTag: latest
```

Jenkins updates `newTag` to the short Git SHA after the image is pushed.

## Argo CD Setup

Install Argo CD:

```powershell
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl get pods -n argocd
```

Apply the HighFiveBooks application manifest:

```powershell
kubectl apply -f k8s/gitops/argocd-application.yaml
```

The application points to `k8s/base`, so manifest tag commits from Jenkins become the deployment source of truth.

## Evidence Checklist

- Jenkins build log showing changed service detection
- Maven build and test logs
- Docker build and push logs
- Git commit where Jenkins updates `k8s/base/kustomization.yaml`
- Argo CD sync status after the tag update
- Kubernetes rollout or pod readiness output after sync

## Interview Summary

Jenkins performs build, test, image publishing, and manifest tag updates only. Deployment authority stays with Argo CD, which syncs the cluster from Git. This keeps Git as the single source of truth and prevents the CI server from becoming an uncontrolled deployment actor.
