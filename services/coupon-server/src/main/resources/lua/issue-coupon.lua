-- KEYS[1]: coupon:count:{id} (잔여 수량 키)
-- KEYS[2]: coupon:issued:{id}:users (발급받은 유저 목록 Set 키)
-- ARGV[1]: userId (유저 ID)

-- 1. 중복 발급 검사
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1 -- 이미 발급됨 code
end

-- 2. 재고 확인 및 차감
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil or stock <= 0 then
    return 0 -- 재고 없음 code
end

-- 3. 재고 차감 및 유저 등록 (Atomic)
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])

return 1 -- 발급 성공 code