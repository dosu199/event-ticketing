-- Atomically releases a hold. Only removes seats that still belong to THIS hold,
-- so it can never free a seat that expired and was meanwhile taken by someone else.
--
-- KEYS[1]  hold:event:<eventId>
-- KEYS[2]  hold:<holdId>
-- KEYS[3]  hold:user:<eventId>:<userId>
-- ARGV[1]  holdId
-- ARGV[2..] seat ids

for i = 2, #ARGV do
  if redis.call('HGET', KEYS[1], ARGV[i]) == ARGV[1] then
    redis.call('HDEL', KEYS[1], ARGV[i])
  end
end
redis.call('DEL', KEYS[2])
if redis.call('GET', KEYS[3]) == ARGV[1] then
  redis.call('DEL', KEYS[3])
end
return 1
