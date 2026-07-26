-- hoàn lại khi booking failed
for i=1, #KEYS do
    redis.call('INCRBY', KEYS[i], tonumber(ARGV[i]))
end
return 1