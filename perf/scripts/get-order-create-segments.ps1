param(
    [string]$BaseUrl = "http://localhost:9006"
)

$lines = (curl.exe -fsS "$BaseUrl/actuator/prometheus") -split "`n"
$segments = @{}

foreach ($line in $lines) {
    if ($line -match '^order_create_segment_seconds_count\{segment="([^"]+)"\} ([0-9.Ee+-]+)') {
        $segment = $Matches[1]
        if (-not $segments.ContainsKey($segment)) { $segments[$segment] = @{} }
        $segments[$segment].count = [double]$Matches[2]
    }
    elseif ($line -match '^order_create_segment_seconds_sum\{segment="([^"]+)"\} ([0-9.Ee+-]+)') {
        $segment = $Matches[1]
        if (-not $segments.ContainsKey($segment)) { $segments[$segment] = @{} }
        $segments[$segment].sum = [double]$Matches[2]
    }
    elseif ($line -match '^order_create_segment_seconds_bucket\{segment="([^"]+)",le="([^"]+)"\} ([0-9.Ee+-]+)') {
        $segment = $Matches[1]
        $upperBound = $Matches[2]
        if ($upperBound -eq '+Inf') { continue }
        if (-not $segments.ContainsKey($segment)) { $segments[$segment] = @{} }
        if (-not $segments[$segment].ContainsKey('buckets')) { $segments[$segment].buckets = @() }
        $segments[$segment].buckets += [pscustomobject]@{
            upperBound = [double]$upperBound
            count = [double]$Matches[3]
        }
    }
}

function Get-UpperBoundPercentile($data, [double]$percentile) {
    $target = [math]::Ceiling($data.count * $percentile)
    $bucket = $data.buckets |
        Sort-Object upperBound |
        Where-Object { $_.count -ge $target } |
        Select-Object -First 1
    if ($null -eq $bucket) { return $null }
    return [math]::Round($bucket.upperBound * 1000, 2)
}

$rows = foreach ($segment in ($segments.Keys | Sort-Object)) {
    $data = $segments[$segment]
    [pscustomobject]@{
        segment = $segment
        count = [int]$data.count
        avg_ms = [math]::Round(($data.sum / $data.count) * 1000, 2)
        p50_upper_ms = Get-UpperBoundPercentile $data 0.50
        p95_upper_ms = Get-UpperBoundPercentile $data 0.95
        p99_upper_ms = Get-UpperBoundPercentile $data 0.99
    }
}

$rows | Sort-Object p95_upper_ms -Descending | Format-Table -AutoSize
