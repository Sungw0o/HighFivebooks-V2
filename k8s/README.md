# Kubernetes Manifests

HighFiveBooks V2 keeps MSA boundaries and replaces Spring Cloud runtime pieces
with Kubernetes primitives.

```text
Eureka         -> Kubernetes Service DNS
Config Server  -> ConfigMap and Secret
Gateway        -> Ingress
```

## Structure

```text
base/       Kustomize base for local kind/k3s transition verification
gitops/     ArgoCD Application example
rollouts/   Argo Rollouts canary example for order-server
```

The base target includes:

```text
book-server
member-server
coupon-server
payment-server
order-server
mysql
redis
rabbitmq
elasticsearch
minio
ingress
```

## Validate

```powershell
kubectl kustomize k8s/base
```

## Apply

```powershell
kubectl apply -k k8s/base
```

Read the full runbook before applying real values:

```text
docs/k8s-transition-runbook.md
```
