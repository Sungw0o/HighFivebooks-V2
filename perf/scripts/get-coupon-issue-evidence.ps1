param(
    [long]$CouponId = 1,
    [string]$MySqlContainer = "highfive-mysql",
    [string]$RedisContainer = "highfive-redis",
    [string]$Database = "highfive_coupon",
    [string]$User = "coupon_user",
    [string]$Password = "coupon_pass",
    [string]$RedisPassword = "highfive_redis"
)

$ErrorActionPreference = "Stop"

Write-Host "Coupon issue evidence"
Write-Host "couponId=$CouponId"

$issuedCountSql = "SELECT COUNT(*) FROM member_coupon WHERE coupon_id=$CouponId;"
$duplicateCountSql = "SELECT COUNT(*) FROM (SELECT user_id, COUNT(*) c FROM member_coupon WHERE coupon_id=$CouponId GROUP BY user_id HAVING c > 1) dup;"
$statusCountSql = "SELECT status, COUNT(*) FROM member_coupon WHERE coupon_id=$CouponId GROUP BY status;"

Write-Host ""
Write-Host "[MySQL] issued count"
docker exec $MySqlContainer mysql -u$User -p$Password $Database -N -e $issuedCountSql

Write-Host ""
Write-Host "[MySQL] duplicate user count"
docker exec $MySqlContainer mysql -u$User -p$Password $Database -N -e $duplicateCountSql

Write-Host ""
Write-Host "[MySQL] status count"
docker exec $MySqlContainer mysql -u$User -p$Password $Database -N -e $statusCountSql

Write-Host ""
Write-Host "[Redis] remaining stock"
docker exec $RedisContainer redis-cli -a $RedisPassword GET "coupon:count:$CouponId"

Write-Host ""
Write-Host "[Redis] issued user set size"
docker exec $RedisContainer redis-cli -a $RedisPassword SCARD "coupon:issued:${CouponId}:users"
