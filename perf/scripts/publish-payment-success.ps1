param(
    [long]$OrderId,
    [long]$TotalAmount,
    [string]$PaymentKey = "",
    [string]$RabbitManagementUrl = "http://localhost:15672",
    [string]$RabbitUser = "highfive",
    [string]$RabbitPassword = "highfive",
    [string]$Queue = "payment-success-queue"
)

$ErrorActionPreference = "Stop"

if ($OrderId -le 0) {
    throw "OrderId is required."
}

if ($TotalAmount -lt 0) {
    throw "TotalAmount must be zero or greater."
}

if ([string]::IsNullOrWhiteSpace($PaymentKey)) {
    $PaymentKey = "perf-payment-$OrderId-$([Guid]::NewGuid().ToString('N'))"
}

$pair = "${RabbitUser}:${RabbitPassword}"
$encoded = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$headers = @{ Authorization = "Basic $encoded" }

$message = @{
    orderId = $OrderId
    paymentKey = $PaymentKey
    totalAmount = $TotalAmount
    approvedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
} | ConvertTo-Json -Compress

$body = @{
    properties = @{
        content_type = "application/json"
    }
    routing_key = $Queue
    payload = $message
    payload_encoding = "string"
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
    -Method Post `
    -Uri "$RabbitManagementUrl/api/exchanges/%2F/amq.default/publish" `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $body

