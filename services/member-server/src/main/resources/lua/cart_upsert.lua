-- KEYS[1] : cart key
-- ARGV[1] : field(bookId)
-- ARGV[2] : qty (ADD면 addQty, SET이면 newQty)
-- ARGV[3] : maxQty
-- ARGV[4] : ttlSeconds
-- ARGV[5] : mode ("ADD" or "SET")

-- keys는 redis용 ARGV는 로직용

redis.call('HDEL', KEYS[1], 'CART_STATUS')

local key = KEYS[1]
local field = ARGV[1]
local qtyArg = tonumber(ARGV[2])
local maxQty = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])
local mode = ARGV[5]

-- 숫자 검증
if qtyArg == nil or qtyArg <= 0 then
	return -1
end

-- 장바구니에 없으면 nil 있으면 값 들어옴
local current = redis.call('HGET', key, field)
local currentQty = tonumber(current) or 0

-- 증가 연산
if mode == 'ADD' then
	if currentQty + qtyArg > maxQty then
		return -1
	end
	-- HINCRBY 가장 빠른 증가 연산
	redis.call('HINCRBY', key, field, qtyArg)
	-- 변화 있으니 TTL도 수정
	redis.call('EXPIRE', key, ttl)
	return currentQty + qtyArg
end

-- 업데이트 연산
if mode == 'SET' then
	if qtyArg > maxQty then
		return -1
	end
	redis.call('HSET', key, field, qtyArg)
	redis.call('EXPIRE', key, ttl)
	return qtyArg
end

return -1
