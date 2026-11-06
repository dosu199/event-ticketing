-- Atomically holds a set of seats for one user, all-or-nothing.
-- Redis runs a Lua script as ONE operation: no other command can run in between,
-- so two users can never both see a seat as free and both take it.
--
-- KEYS[1]  hold:event:<eventId>              hash  seatId -> holdId (each field has its own TTL)
-- KEYS[2]  hold:<holdId>                     hash  userId, eventId, seatIds, expiresAt
-- KEYS[3]  hold:user:<eventId>:<userId>      string holdId (one active hold per user per event)
-- ARGV[1]  ttl in milliseconds
-- ARGV[2]  holdId
-- ARGV[3]  userId
-- ARGV[4]  eventId
-- ARGV[5]  expiresAt (epoch millis)
-- ARGV[6..] seat ids
--
-- Returns {'OK'} | {'ALREADY_HOLDING'} | {'TAKEN', seatId, seatId, ...}

if redis.call('EXISTS', KEYS[3]) == 1 then
  return {'ALREADY_HOLDING'}
end

local taken = {}
for i = 6, #ARGV do
  if redis.call('HEXISTS', KEYS[1], ARGV[i]) == 1 then
    table.insert(taken, ARGV[i])
  end
end
if #taken > 0 then
  table.insert(taken, 1, 'TAKEN')
  return taken
end

local ttl = tonumber(ARGV[1])
local seats = {}
for i = 6, #ARGV do
  redis.call('HSET', KEYS[1], ARGV[i], ARGV[2])
  -- Per-field expiry (Redis 7.4+): each seat frees itself when the hold ends.
  redis.call('HPEXPIRE', KEYS[1], ttl, 'FIELDS', 1, ARGV[i])
  table.insert(seats, ARGV[i])
end

redis.call('HSET', KEYS[2], 'userId', ARGV[3], 'eventId', ARGV[4],
           'expiresAt', ARGV[5], 'seatIds', table.concat(seats, ','))
redis.call('PEXPIRE', KEYS[2], ttl)
redis.call('SET', KEYS[3], ARGV[2], 'PX', ttl)
return {'OK'}
