-- lặp kiểm tra key và kiểm tra available so với request
for i=1, #KEYS do
    local avail = redis.call('GET', KEYS[i])
    if avail == false or tonumber(avail) < tonumber(ARGV[i]) then
        return 0 
    end
end

-- trừ khi toàn bộ room-type thỏa mãn 
for i=1, #KEYS do
    redis.call('DECRBY', KEYS[i], tonumber(ARGV[i]))
end

-- thành công
return 1