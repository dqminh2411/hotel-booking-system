package com.hotelbooking.bookingservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.bookingservice.dto.kafka.BookingFailed;
import com.hotelbooking.bookingservice.dto.kafka.CancelBooking;
import com.hotelbooking.bookingservice.dto.kafka.ConfirmBooking;
import com.hotelbooking.bookingservice.dto.kafka.CreateBookingCommand;
import com.hotelbooking.bookingservice.service.BookingService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingCommandConsumer {
    ObjectMapper objectMapper;
    BookingService bookingService;
    RedissonClient redissonClient;

    @KafkaListener(topics = "booking-commands")
    public void consume(String message) throws JsonProcessingException{
        JsonNode node = objectMapper.readTree(message);
        String eventType = node.path("eventType").asText();
        switch (eventType) {
            case "CreateBooking" -> handleLockCreateBooking(objectMapper.treeToValue(node, CreateBookingCommand.class));
            case "ConfirmBooking" -> {
                ConfirmBooking command = objectMapper.treeToValue(node, ConfirmBooking.class);
                bookingService.handleConfirmBooking(command.sagaId(), command.bookingId());
            }
            case "CancelBooking" -> {
                CancelBooking command = objectMapper.treeToValue(node, CancelBooking.class);
                bookingService.handleCancelBooking(command.sagaId(), command.bookingId(), command.reason());
            }
            default -> log.warn("Ignore unsupported booking command eventType={}", eventType);
        }
    }

    private void handleLockCreateBooking(CreateBookingCommand command){
        List<UUID> listRoomTypeId = command.roomTypeList().stream()
                    .map(rt -> rt.roomTypeId())
                    .sorted()
                    .toList();

        List<RLock> listRLock = listRoomTypeId.stream()
                        // key = "lock:booking:d0001...."
                        .map(rt -> redissonClient.getLock("lock:booking:" + rt.toString()))
                        .toList();
        
        RLock multiLock = redissonClient.getMultiLock(listRLock.toArray(new RLock[0]));
        boolean isLocked = false;
        try {
            // giải thích tham số:
            // 5: wait time - đợi xin khóa tối đa 5s
            // -1: least time - thời gian nhả khóa (-1 là khi khóa hết hạn mà chưa xong thì tự động gia hạn)
            // có thể cho wait time lên 10s
            isLocked = multiLock.tryLock(5, -1, TimeUnit.SECONDS);
            if (!isLocked) {
                // khóa lỗi thì trả về việc tạo booking false đi (tại 5-10s là cx đủ lâu để biết lỗi rồi)
                bookingService.saveOutboxEvent(
                    new BookingFailed(
                        command.sagaId(),
                        "BookingFailed",
                        bookingService.toBookingDetail(command),
                        "Hệ thống đang bận/quá tải, vui lòng thử lại"));
                return;
            }
            log.info("Lấy khóa thành công {}", listRoomTypeId);
            bookingService.handleCreateBooking(command);

        } catch (InterruptedException ex) {
            // cho trường hợp bị lỗi khi đang đứng chờ khóa
            Thread.currentThread().interrupt();
            log.error("Tiến trình chờ khóa bị ngắt", ex);
            throw new RuntimeException("Lỗi máy chủ khi đang xử lý yêu cầu");
        } catch(Exception e){
            log.info("Lỗi khi xử lý đặt phòng: " + e.getMessage());
            throw e;
        } finally{
            if(isLocked){
                try {
                    multiLock.unlock();
                } catch (Exception e) {
                    log.error("Lỗi khi trả khóa cho các phòng {}. (Khóa sẽ tự động hết hạn)", listRoomTypeId, e);
                }
            }

        }
    }
}
