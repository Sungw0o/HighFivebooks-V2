param(
    [string]$Namespace = "highfivebooks",
    [string]$Timeout = "240s",
    [string]$CurlImage = "curlimages/curl:8.10.1"
)

$ErrorActionPreference = "Stop"

function Invoke-Kubectl {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$KubectlArgs)

    Write-Host "`n> kubectl $($KubectlArgs -join ' ')" -ForegroundColor Cyan
    & kubectl @KubectlArgs
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl command failed: kubectl $($KubectlArgs -join ' ')"
    }
}

function Assert-Endpoint {
    param([string]$Name)

    $addresses = & kubectl -n $Namespace get endpointslice -l "kubernetes.io/service-name=$Name" -o jsonpath="{.items[*].endpoints[*].addresses[*]}"
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($addresses)) {
        throw "Endpoint has no ready addresses: $Name"
    }

    Write-Host "endpointslice/$Name -> $addresses" -ForegroundColor Green
}

$deployments = @(
    "book-server",
    "member-server",
    "coupon-server",
    "payment-server",
    "order-server"
)

$statefulSets = @(
    "mysql",
    "redis",
    "rabbitmq",
    "elasticsearch",
    "minio"
)

$services = $deployments + $statefulSets

Invoke-Kubectl get namespace $Namespace

foreach ($name in $statefulSets) {
    Invoke-Kubectl -n $Namespace rollout status "statefulset/$name" "--timeout=$Timeout"
}

foreach ($name in $deployments) {
    Invoke-Kubectl -n $Namespace rollout status "deployment/$name" "--timeout=$Timeout"
}

foreach ($name in $services) {
    Assert-Endpoint $name
}

$bookServiceUrl = & kubectl -n $Namespace exec deploy/order-server -- printenv BOOK_SERVICE_URL
if ($LASTEXITCODE -ne 0 -or $bookServiceUrl -ne "http://book-server:8080") {
    throw "Unexpected BOOK_SERVICE_URL in order-server: $bookServiceUrl"
}
Write-Host "order-server BOOK_SERVICE_URL=$bookServiceUrl" -ForegroundColor Green

$ddlAuto = & kubectl -n $Namespace exec deploy/order-server -- printenv SPRING_JPA_HIBERNATE_DDL_AUTO
if ($LASTEXITCODE -ne 0 -or $ddlAuto -ne "update") {
    throw "Unexpected SPRING_JPA_HIBERNATE_DDL_AUTO in order-server: $ddlAuto"
}
Write-Host "order-server SPRING_JPA_HIBERNATE_DDL_AUTO=$ddlAuto" -ForegroundColor Green

$orderLogs = & kubectl -n $Namespace logs deploy/order-server --tail=-1
if ($LASTEXITCODE -ne 0) {
    throw "Failed to read order-server logs"
}
$orderLogText = $orderLogs -join "`n"
if ($orderLogText -notmatch "HikariPool-1 - Start completed") {
    throw "order-server log does not show MySQL connection pool startup"
}
if ($orderLogText -notmatch "Created new connection: rabbitConnectionFactory") {
    throw "order-server log does not show RabbitMQ connection"
}
Write-Host "order-server MySQL/RabbitMQ connection logs found" -ForegroundColor Green

$plugins = & kubectl -n $Namespace exec statefulset/elasticsearch -- curl -sS localhost:9200/_cat/plugins
if ($LASTEXITCODE -ne 0 -or (($plugins -join "`n") -notmatch "analysis-nori")) {
    throw "Elasticsearch analysis-nori plugin is not installed"
}
Write-Host "elasticsearch analysis-nori plugin found" -ForegroundColor Green

$analyzePayload = '{\"analyzer\":\"korean_html_analyzer\",\"text\":\"java\"}'
$analyzeResponse = & kubectl -n $Namespace exec statefulset/elasticsearch -- curl -sS -X POST localhost:9200/high-five/_analyze -H "Content-Type: application/json" -d $analyzePayload
$analyzeResponseText = $analyzeResponse -join "`n"
Write-Host "elasticsearch korean_html_analyzer response=$analyzeResponseText" -ForegroundColor Green
if ($LASTEXITCODE -ne 0 -or ($analyzeResponseText -notmatch '"token":"java"')) {
    throw "Elasticsearch korean_html_analyzer did not return expected token"
}
Write-Host "elasticsearch korean_html_analyzer returned expected token" -ForegroundColor Green

$readinessTargets = @(
    "book-server",
    "member-server",
    "coupon-server",
    "payment-server",
    "order-server"
)

foreach ($service in $readinessTargets) {
    $url = "http://${service}:8080/actuator/health/readiness"
    Write-Host "`n> kubectl -n $Namespace exec deploy/order-server -- curl -sS -o /tmp/readiness-body -w %{http_code} $url" -ForegroundColor Cyan
    $code = & kubectl -n $Namespace exec deploy/order-server -- curl -sS -o /tmp/readiness-body -w "%{http_code}" $url
    $body = & kubectl -n $Namespace exec deploy/order-server -- cat /tmp/readiness-body
    Write-Host "$service readiness -> $code $body" -ForegroundColor Green
    if ($LASTEXITCODE -ne 0 -or $code -ne "200") {
        throw "$service readiness check failed: $code $body"
    }
}

Write-Host "`nK8s smoke check passed." -ForegroundColor Green
