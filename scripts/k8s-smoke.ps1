param(
    [string]$Namespace = "highfivebooks",
    [string]$Timeout = "240s",
    [string]$CurlImage = "curlimages/curl:8.10.1"
)

$ErrorActionPreference = "Stop"

function Invoke-Kubectl {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)

    Write-Host "`n> kubectl $($Args -join ' ')" -ForegroundColor Cyan
    & kubectl @Args
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl command failed: kubectl $($Args -join ' ')"
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

Invoke-Kubectl "get", "namespace", $Namespace

foreach ($name in $statefulSets) {
    Invoke-Kubectl "-n", $Namespace, "rollout", "status", "statefulset/$name", "--timeout=$Timeout"
}

foreach ($name in $deployments) {
    Invoke-Kubectl "-n", $Namespace, "rollout", "status", "deployment/$name", "--timeout=$Timeout"
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

& kubectl -n $Namespace delete pod k8s-smoke-curl --ignore-not-found | Out-Null

$curlScript = @'
set -eu
sleep 1

required="book-server:/actuator/health/readiness member-server:/actuator/health/readiness coupon-server:/actuator/health/readiness order-server:/actuator/health/readiness"
for item in $required; do
  svc="${item%%:*}"
  path="${item#*:}"
  code="$(curl -sS -o /tmp/body -w "%{http_code}" "http://$svc:8080$path" || true)"
  body="$(cat /tmp/body 2>/dev/null || true)"
  echo "$svc$path -> $code $body"
  test "$code" = "200"
done

code="$(curl -sS -o /tmp/body -w "%{http_code}" "http://payment-server:8080/actuator/health" || true)"
body="$(cat /tmp/body 2>/dev/null || true)"
echo "payment-server/actuator/health optional -> $code $body"
'@

Invoke-Kubectl "-n", $Namespace, "run", "k8s-smoke-curl", "--rm", "-i", "--restart=Never", "--image=$CurlImage", "--command", "--", "sh", "-c", $curlScript

Write-Host "`nK8s smoke check passed." -ForegroundColor Green
