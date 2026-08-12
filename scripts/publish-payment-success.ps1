param(
    [long] $OrderId = 1,
    [long] $TotalAmount = 18000,
    [string] $PaymentKey = "k6-payment-success",
    [string] $RabbitUser = $env:RABBITMQ_USERNAME,
    [string] $RabbitPassword = $env:RABBITMQ_PASSWORD,
    [string] $RabbitApiBaseUrl = "http://localhost:15672",
    [string] $Queue = "payment-success-queue"
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RabbitUser)) {
    $RabbitUser = "highfive"
}

if ([string]::IsNullOrWhiteSpace($RabbitPassword)) {
    $RabbitPassword = "highfive"
}

$message = @{
    orderId = $OrderId
    paymentKey = $PaymentKey
    totalAmount = $TotalAmount
    approvedAt = (Get-Date).ToUniversalTime().ToString("o")
} | ConvertTo-Json -Compress

$body = @{
    properties = @{
        content_type = "application/json"
    }
    routing_key = $Queue
    payload = $message
    payload_encoding = "string"
} | ConvertTo-Json -Depth 5

$pair = "{0}:{1}" -f $RabbitUser, $RabbitPassword
$basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))

Invoke-RestMethod `
    -Method Post `
    -Uri "$RabbitApiBaseUrl/api/exchanges/%2F/amq.default/publish" `
    -Headers @{ Authorization = "Basic $basic" } `
    -ContentType "application/json" `
    -Body $body
