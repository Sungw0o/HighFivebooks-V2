-- KEYS[1] : member cart key
-- KEYS[2] : guest cart key
-- ARGV[1] : maxQty
-- ARGV[2] : ttlSeconds

local memberKey = KEYS[1]
local guestKey = KEYS[2]
local maxQty = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

-- guest 장바구니 조회
local guestFields = redis.call('HGETALL', guestKey)

-- 비어있거나 없으면 redis 지우고 리턴
if guestFields == nil or #guestFields == 0 then
	redis.call('DEL', guestKey)
	return 1
end

-- 멤버에 아이템이 있으면 합산 없으면 생성
-- 2는 증가값 왜냐 id qty id qty 이런식으로 저장되어있기때문에
-- 뒤에 i+1 나오는 이유도 qty가 다음에 저장되어있기 때문에
for i = 1, #guestFields, 2 do
	local field = guestFields[i]
	local gQty = tonumber(guestFields[i+1]) or 0
	if gQty > 0 then
		local cur = redis.call('HGET', memberKey, field)
		local curQty = tonumber(cur) or 0
		local newQty = curQty + gQty
		-- MAX 초과 방지
		if newQty > maxQty then newQty = maxQty end
		-- 계산한 수량으로 덮어씀
		redis.call('HSET', memberKey, field, newQty)
	end
end

-- 병합 완료 redis 제거
redis.call('DEL', guestKey)
-- ttl 증가
redis.call('EXPIRE', memberKey, ttl)

return 1
