param(
    [switch]$WithTests
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$services = @(
    "book-server",
    "member-server",
    "coupon-server",
    "payment-server",
    "order-server"
)

foreach ($service in $services) {
    $servicePath = Join-Path $repoRoot "services\$service"
    Write-Host "Building $service" -ForegroundColor Cyan

    Push-Location $servicePath
    try {
        if ($WithTests) {
            .\mvnw.cmd package
        } else {
            .\mvnw.cmd -DskipTests package
        }
    } finally {
        Pop-Location
    }
}

Write-Host "All service jars are ready." -ForegroundColor Green
