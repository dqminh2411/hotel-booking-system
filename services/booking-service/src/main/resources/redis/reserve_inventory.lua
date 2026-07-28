if redis.call('GET', KEYS[1]) == '1' then
    return 1
end

-- lặp kiểm tra key và kiểm tra available so với request
for i=2, #KEYS do
    local avail = redis.call('GET', KEYS[i])
    if avail == false then
        return -1  
    end
    if tonumber(avail) < tonumber(ARGV[i]) then
        return 0  
    end
end

-- trừ khi toàn bộ room-type thỏa mãn 
for i=2, #KEYS do
    redis.call('DECRBY', KEYS[i], tonumber(ARGV[i]))
end

redis.call('SET', KEYS[1], '1', 'EX', 600) -- cho kiểm tra key trong 10p

-- thành công
return 1