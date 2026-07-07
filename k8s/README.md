# Kubernetes Manifests

This directory will hold the Kubernetes runtime for HighFiveBooks V2.

Target replacement map:

```text
Eureka        -> Kubernetes Service DNS
Config Server -> ConfigMap and Secret
Gateway       -> Ingress
```

Planned structure:

```text
base/
  order/
  coupon/
  book/
  member/
  payment/
  mysql/
  redis/
  rabbitmq/
  ingress/

overlays/
  local/
  prod-like/
```

The first working target is a local cluster through kind or k3s.

