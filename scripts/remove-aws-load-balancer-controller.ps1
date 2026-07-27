[CmdletBinding()]
param(
    [string] $IngressName = "highfivebooks-api",
    [string] $Namespace = "highfivebooks",
    [string] $AwsRegion = "ap-northeast-2",
    [int] $TimeoutMinutes = 10
)

$ErrorActionPreference = "Stop"

$albHost = kubectl get ingress $IngressName `
    --namespace $Namespace `
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' `
    2>$null

kubectl delete ingress $IngressName `
    --namespace $Namespace `
    --ignore-not-found

if ($albHost) {
    $deadline = (Get-Date).AddMinutes($TimeoutMinutes)

    do {
        $remaining = aws elbv2 describe-load-balancers `
            --region $AwsRegion `
            --query "LoadBalancers[?DNSName=='$albHost'].DNSName" `
            --output text `
            2>$null

        if (-not $remaining) {
            break
        }

        Start-Sleep -Seconds 10
    } while ((Get-Date) -lt $deadline)

    if ($remaining) {
        throw "ALB가 제한 시간 안에 삭제되지 않았습니다: $albHost"
    }
}

helm uninstall aws-load-balancer-controller `
    --namespace kube-system `
    --ignore-not-found

Write-Output "ALB Ingress와 AWS Load Balancer Controller를 정리했습니다."
