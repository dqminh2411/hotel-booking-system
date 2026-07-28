-- xóa key của bookingId kho release trong trường hợp kafka retry
redis.call('DEL', KEYS[1])

-- hoàn lại khi booking failed
for i=2, #KEYS do
    redis.call('INCRBY', KEYS[i], tonumber(ARGV[i]))
end
return 1