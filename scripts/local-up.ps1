param(
    [switch]$InfraOnly,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

if (!(Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "Created .env from .env.example. Fill external API keys when needed." -ForegroundColor Yellow
}

if ($InfraOnly) {
    docker compose up -d mysql rabbitmq redis elasticsearch minio minio-init
    docker compose ps
    exit 0
}

if (!$SkipBuild) {
    powershell -ExecutionPolicy Bypass -File ".\scripts\build-services.ps1"
}

docker compose --profile apps up -d --build
docker compose ps
