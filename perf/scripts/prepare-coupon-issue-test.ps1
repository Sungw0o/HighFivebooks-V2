param(
    [string]$BaseUrl = "http://localhost:9004",
    [string]$PolicyName = "K6 first-come coupon policy",
    [string]$CouponName = "K6 first-come coupon",
    [int]$IssueCount = 100,
    [int]$DiscountValue = 5000,
    [int]$MinOrderValue = 10000,
    [int]$MaxDiscountValue = 5000,
    [int]$ValidPeriodDate = 30
)

$ErrorActionPreference = "Stop"

$now = Get-Date
$issueStartAt = $now.AddMinutes(-5).ToString("yyyy-MM-ddTHH:mm:ss")
$issueEndAt = $now.AddDays(1).ToString("yyyy-MM-ddTHH:mm:ss")

$policyBody = @{
    name = $PolicyName
    comment = "EVENT"
    discountType = "FIXED"
    discountValue = $DiscountValue
    minOrderValue = $MinOrderValue
    maxDiscountValue = $MaxDiscountValue
} | ConvertTo-Json

Write-Host "Creating coupon policy..."
$policy = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/coupons/admin/coupon-policies" -ContentType "application/json" -Body $policyBody

$policyId = $policy.id
if (-not $policyId -and $policy.data) {
    $policyId = $policy.data.id
}
if (-not $policyId) {
    throw "Could not read policy id from response: $($policy | ConvertTo-Json -Depth 10)"
}

$couponBody = @{
    id = $policyId
    couponName = $CouponName
    description = "Coupon issue concurrency test seed"
    issueCount = $IssueCount
    issueStartAt = $issueStartAt
    issueEndAt = $issueEndAt
    validPeriodDate = $ValidPeriodDate
    validEndAt = $null
    couponType = "NORMAL"
} | ConvertTo-Json

Write-Host "Creating coupon template..."
$coupon = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/coupons/admin/coupons" -ContentType "application/json" -Body $couponBody

$couponId = $coupon.id
if (-not $couponId -and $coupon.data) {
    $couponId = $coupon.data.id
}
if (-not $couponId) {
    throw "Could not read coupon id from response: $($coupon | ConvertTo-Json -Depth 10)"
}

Write-Host "Coupon issue test seed ready"
Write-Host "policyId=$policyId"
Write-Host "couponId=$couponId"
Write-Host "issueCount=$IssueCount"
Write-Host "issueStartAt=$issueStartAt"
Write-Host "issueEndAt=$issueEndAt"
