package com.hotelbooking.bookingservice.scheduler;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hotelbooking.bookingservice.dto.ActiveBookingRoomType;
import com.hotelbooking.bookingservice.entity.RoomTypeInventory;
import com.hotelbooking.bookingservice.enums.BookingStatus;
import com.hotelbooking.bookingservice.repository.BookingRepository;
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
    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKEDIN);

    RoomTypeInventoryRepository roomTypeInventoryRepository;
    BookingRepository bookingRepository;
    StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void setWarmUp(){
        warnUpWindow(LocalDate.now(), LocalDate.now().plusDays(WARM_UP_DAYS));
        log.info("Set inventory vào Redis cho {} ngày kể từ hôm nay", WARM_UP_DAYS);
    }

    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void renewAvailableValue(){
        warnUpWindow(LocalDate.now(), LocalDate.now().plusDays(60));
        log.info("Set lại giá trị vào Redis cho 60 ngày kể từ hôm nay");
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void setNewDay(){
        clearKeys(LocalDate.now().minusDays(1));

        warnUpWindow(LocalDate.now(), LocalDate.now().plusDays(WARM_UP_DAYS));
    }

    private void warnUpWindow(LocalDate from, LocalDate to){
        List<RoomTypeInventory> allRoomTypes = roomTypeInventoryRepository.findAll();
        List<UUID> roomTypeIds = allRoomTypes.stream()
                                        .map(RoomTypeInventory::getRoomTypeId)
                                        .toList();

        Map<String, String> redisData = new HashMap<>();
        LocalDate date = from;
        while (date.isBefore(to)) {
            LocalDate nextDay = date.plusDays(1);
            List<ActiveBookingRoomType> activeBookings = bookingRepository.countActiveBookingsByRoomType(
                null, // đếm tất cả cho các loại phòng cho tất cả khách sạn
                roomTypeIds,
                roomTypeIds.isEmpty(),
                date,
                nextDay,
                ACTIVE_STATUSES
            );

            Map<UUID, Long> countBooking = activeBookings.stream()
                                    .collect(Collectors.toMap(
                                        ActiveBookingRoomType::roomTypeId, 
                                        ActiveBookingRoomType::bookingCount,
                                        (exist, replace) -> exist));
            
            for(RoomTypeInventory rti : allRoomTypes){
                long booking = countBooking.getOrDefault(rti.getRoomTypeId(), 0L);
                int avail = (int)(rti.getTotalQuantity() - booking);

                String keys = "avail:" + rti.getRoomTypeId().toString() + ":" + date.toString();
                redisData.put(keys, String.valueOf(avail));
            }

            date = nextDay;
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
