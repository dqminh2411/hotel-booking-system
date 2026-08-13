package com.hotelbooking.bookingservice.scheduler;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.bookingservice.entity.RoomTypeInventory;
import com.hotelbooking.bookingservice.repository.RoomTypeInventoryRepository;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryWarnUpSchedule {

    private static final int WARM_UP_DAYS = 180;

    RoomTypeInventoryRepository roomTypeInventoryRepository;
    StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void setWarmUp(){
        warmUpWindow(LocalDate.now(), LocalDate.now().plusDays(WARM_UP_DAYS));
        log.info("Set inventory vào Redis cho {} ngày kể từ hôm nay", WARM_UP_DAYS);
    }

    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void renewAvailableValue(){
        warmUpWindow(LocalDate.now(), LocalDate.now().plusDays(60));
        log.info("Set lại giá trị vào Redis cho 60 ngày kể từ hôm nay");
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    public void setNewDay(){
        int inserted = roomTypeInventoryRepository.extendInventoryWindow();
        int deleted = roomTypeInventoryRepository.deleteExpiredInventory();
        log.info("Rolling window roomtype_inventory: thêm {} dòng mới, xoá {} dòng quá hạn", inserted, deleted);

        clearKeys(LocalDate.now().minusDays(1));
        warmUpWindow(LocalDate.now(), LocalDate.now().plusDays(WARM_UP_DAYS));
    }

    private void warmUpWindow(LocalDate from, LocalDate to){
        List<RoomTypeInventory> inventories = roomTypeInventoryRepository
                .findAllByIdInventoryDateBetween(from, to.minusDays(1));

        Map<String, String> redisData = new HashMap<>();
        for (RoomTypeInventory rti : inventories) {
            String key = "avail:" + rti.getId().getRoomTypeId() + ":" + rti.getId().getInventoryDate();
            redisData.put(key, String.valueOf(rti.getAvailableQuantity()));
        }

        if(!redisData.isEmpty()){
            stringRedisTemplate.opsForValue().multiSet(redisData);
            log.info("Đã đồng bộ {} avail lên Redis thành công", redisData.size());
        }
    }

    private void clearKeys(LocalDate date){
        String oldKeys = "avail:*:" + date.toString();
        Set<String> keysToDel = stringRedisTemplate.keys(oldKeys);

        if(keysToDel != null && !keysToDel.isEmpty()){
            stringRedisTemplate.delete(keysToDel);
            log.info("Đã xóa các key của ngày hôm qua");
        }
    }
}