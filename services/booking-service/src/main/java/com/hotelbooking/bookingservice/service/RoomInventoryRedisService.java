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

import com.hotelbooking.bookingservice.enums.ReserveResult;

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

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = loadScript("redis/reserve_inventory.lua");
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = loadScript("redis/release_inventory.lua");
 
    private static DefaultRedisScript<Long> loadScript(String classpathLocation) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(classpathLocation));
        script.setResultType(Long.class);
        return script;
    }

    public ReserveResult tryReserve(UUID bookingId, Map<UUID, Integer> roomTypeQuantities, LocalDate checkin, LocalDate checkout){
        List<String> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();

        keys.add("reserved:booking:" + bookingId.toString());
        args.add(String.valueOf(-1));

        buildKeysAndArgs(roomTypeQuantities, checkin, checkout, keys, args);

        Long result = stringRedisTemplate.execute(RESERVE_SCRIPT, keys, args.toArray());

        if (result == null || result == -1L) return ReserveResult.UNKNOWN;
        return result == 1L ? ReserveResult.RESERVED : ReserveResult.REJECTED;
    }

    public void release(UUID bookingId, Map<UUID, Integer> roomTypeQuantities, LocalDate checkin, LocalDate checkout){
        try {
            List<String> keys = new ArrayList<>();
            List<String> args = new ArrayList<>();

            keys.add("reserved:booking:" + bookingId.toString());
            args.add(String.valueOf(-1));

            buildKeysAndArgs(roomTypeQuantities, checkin, checkout, keys, args);

            stringRedisTemplate.execute(RELEASE_SCRIPT, keys, args.toArray());
            
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
                keys.add("avail:" + roomTypeId.toString() + ":" + date.toString());
                args.add(String.valueOf(quantity));
                date = date.plusDays(1);
            }
        }
    }
}
