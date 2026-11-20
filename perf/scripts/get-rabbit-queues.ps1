param(
    [string]$RabbitManagementUrl = "http://localhost:15672",
    [string]$RabbitUser = "highfive",
    [string]$RabbitPassword = "highfive"
)

$ErrorActionPreference = "Stop"

$pair = "${RabbitUser}:${RabbitPassword}"
$encoded = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$headers = @{ Authorization = "Basic $encoded" }

$queues = @(
    "payment-success-queue",
    "high-five-order-payment-dead-letter-queue"
)

foreach ($queue in $queues) {
    $info = Invoke-RestMethod `
        -Method Get `
        -Uri "$RabbitManagementUrl/api/queues/%2F/$queue" `
        -Headers $headers

    [PSCustomObject]@{
        queue = $info.name
        state = $info.state
        consumers = $info.consumers
        messages = $info.messages
        ready = $info.messages_ready
        unacknowledged = $info.messages_unacknowledged
        ack = $info.message_stats.ack
        deliver = $info.message_stats.deliver
        publish = $info.message_stats.publish
    }
}
