param(
    [string] $RabbitUser = $env:RABBITMQ_USERNAME,
    [string] $RabbitPassword = $env:RABBITMQ_PASSWORD,
    [string] $RabbitApiBaseUrl = "http://localhost:15672"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RabbitUser)) {
    $RabbitUser = "highfive"
}

if ([string]::IsNullOrWhiteSpace($RabbitPassword)) {
    $RabbitPassword = "highfive"
}

$pair = "{0}:{1}" -f $RabbitUser, $RabbitPassword
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))

$queues = Invoke-RestMethod `
    -Method Get `
    -Uri "$RabbitApiBaseUrl/api/queues" `
    -Headers @{ Authorization = "Basic $basic" }

$queues |
    Where-Object { $_.name -in @("payment-success-queue", "high-five-order-payment-dead-letter-queue") } |
    Select-Object name, messages, messages_ready, messages_unacknowledged, consumers, state
