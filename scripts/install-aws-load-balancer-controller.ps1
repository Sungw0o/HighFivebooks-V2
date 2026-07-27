[CmdletBinding()]
param(
    [string] $ClusterName = "highfivebooks-ephemeral",
    [string] $AwsRegion = "ap-northeast-2",
    [string] $ChartVersion = "1.14.0"
)

$ErrorActionPreference = "Stop"

$vpcId = aws eks describe-cluster `
    --name $ClusterName `
    --region $AwsRegion `
    --query "cluster.resourcesVpcConfig.vpcId" `
    --output text

if (-not $vpcId -or $vpcId -eq "None") {
    throw "EKS VPC ID를 확인하지 못했습니다."
}

helm repo add eks https://aws.github.io/eks-charts --force-update
helm repo update eks

helm upgrade --install aws-load-balancer-controller eks/aws-load-balancer-controller `
    --namespace kube-system `
    --version $ChartVersion `
    --set "clusterName=$ClusterName" `
    --set "region=$AwsRegion" `
    --set "vpcId=$vpcId" `
    --set serviceAccount.create=true `
    --set serviceAccount.name=aws-load-balancer-controller `
    --set replicaCount=1 `
    --wait `
    --timeout 10m

kubectl rollout status deployment/aws-load-balancer-controller `
    --namespace kube-system `
    --timeout 5m
