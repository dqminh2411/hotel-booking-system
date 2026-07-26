package com.hotelbooking.bookingservice.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoomInventoryRedisService {
    StringRedisTemplate stringRedisTemplate;

    public boolean tryReserve(Map<UUID, Integer> roomTypeQuantities, LocalDate checkin, LocalDate checkout){
        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();

        buildKeysAndArgs(roomTypeQuantities, checkin, checkout, keys, args);

        DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setLocation(new ClassPathResource("redis/reserve_inventory.lua"));
        defaultRedisScript.setResultType(Long.class);

        Long result = stringRedisTemplate.execute(defaultRedisScript, keys, args.toArray());

        return result != null && result == 1L;
    }

    public void release(Map<UUID, Integer> roomTypeQuantities, LocalDate checkin, LocalDate checkout){
        try {
            List<String> keys = new ArrayList<>();
            List<String> args = new ArrayList<>();

            buildKeysAndArgs(roomTypeQuantities, checkin, checkout, keys, args);

            
            DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
            defaultRedisScript.setLocation(new ClassPathResource("redis/release_inventory.lua"));
            defaultRedisScript.setResultType(Long.class);

            stringRedisTemplate.execute(defaultRedisScript, keys, args.toArray());
            
            log.info("Đã trả phòng thành công");
        } catch (Exception e) {
            log.error("Lỗi khi release phòng trên Redis"); // lỗi này đợi định kỳ lấy ở db sửa cũng ok, vì db mình coi là src of truth
        }
        
    }

    private void buildKeysAndArgs(Map<UUID, Integer> roomTypeQuantities,
                                LocalDate checkin,
                                LocalDate checkout,
                                List<String> keys,
                                List<String> args){
        
        for(UUID roomTypeId : roomTypeQuantities.keySet()){
            Integer quantity = roomTypeQuantities.get(roomTypeId);

            LocalDate date = checkin;
            while (date.isBefore(checkout)) {
                keys.add("avail:" + roomTypeId + ":" + date.toString());
                args.add(String.valueOf(quantity));
                date.plusDays(1);
            }
        }
    }
}
