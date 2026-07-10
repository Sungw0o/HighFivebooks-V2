# Argo CD and Argo Rollouts Runbook

## Goal

This runbook proves the HighFiveBooks deployment flow:

1. Git is the source of truth.
2. Argo CD syncs `k8s/base` from `main`.
3. `order-server` uses Argo Rollouts canary delivery.
4. Promote and abort commands are reproducible evidence for the portfolio.

Jenkins must not run `kubectl apply` for application deployment. Jenkins only updates image tags in Git.

## Cluster Prerequisites

Install Argo CD:

```powershell
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl get pods -n argocd
```

Install Argo Rollouts:

```powershell
kubectl create namespace argo-rollouts
kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
kubectl get pods -n argo-rollouts
```

Install the Rollouts kubectl plugin when you want CLI evidence:

```powershell
kubectl argo rollouts version
```

## Apply GitOps Application

Register the HighFiveBooks Argo CD application:

```powershell
kubectl apply -f k8s/gitops/argocd-application.yaml
kubectl -n argocd get application highfivebooks
```

Expected target:

- repository: `https://github.com/Sungw0o/HighFivebooks-V2.git`
- revision: `main`
- path: `k8s/base`
- namespace: `highfivebooks`

## Sync Evidence

Capture these commands after Argo CD syncs:

```powershell
kubectl -n argocd get application highfivebooks
kubectl -n highfivebooks get rollouts.argoproj.io
kubectl -n highfivebooks get pods -l app=order-server
kubectl -n highfivebooks get svc order-server
```

The `order-server` Service remains stable while the workload changes from a Deployment to a Rollout.

## Canary Promote Flow

Watch the rollout:

```powershell
kubectl argo rollouts get rollout order-server -n highfivebooks --watch
```

Promote to the next step:

```powershell
kubectl argo rollouts promote order-server -n highfivebooks
```

Useful capture points:

- initial 20 percent step
- 50 percent step after first promote or pause completion
- completed 100 percent rollout
- pods during the rollout

## Abort Flow

Use abort evidence on a non-production local cluster:

```powershell
kubectl argo rollouts abort order-server -n highfivebooks
kubectl argo rollouts get rollout order-server -n highfivebooks
```

This demonstrates rollback control without giving Jenkins direct cluster deployment authority.

## Done Evidence

- Argo CD application is `Synced` and `Healthy`.
- `order-server` appears as `Rollout`, not `Deployment`.
- `kubectl argo rollouts get rollout order-server` shows canary steps.
- Promote and abort commands are captured.
- Smoke test still passes through the stable `order-server` Service.
