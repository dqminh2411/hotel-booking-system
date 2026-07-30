package com.hotelbooking.bookingservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.bookingservice.dto.kafka.BookingFailed;
import com.hotelbooking.bookingservice.dto.kafka.CancelBooking;
import com.hotelbooking.bookingservice.dto.kafka.ConfirmBooking;
import com.hotelbooking.bookingservice.dto.kafka.CreateBookingCommand;
import com.hotelbooking.bookingservice.enums.ReserveResult;
import com.hotelbooking.bookingservice.exception.AppException;
import com.hotelbooking.bookingservice.service.BookingService;
import com.hotelbooking.bookingservice.service.RoomInventoryRedisService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class BookingCommandConsumer {
    ObjectMapper objectMapper;
    BookingService bookingService;
    RoomInventoryRedisService roomInventoryRedisService;

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

        Map<UUID, Integer> roomTypeQuantities = command.roomTypeList().stream()
                                                .collect(Collectors.toMap(
                                                    CreateBookingCommand.RoomTypeItem::roomTypeId,
                                                    CreateBookingCommand.RoomTypeItem::bookingQuantity,
                                                    Integer::sum
                                                ));
        
        ReserveResult result;
        try {
            result = roomInventoryRedisService.tryReserve(command.bookingId(), roomTypeQuantities, command.checkin(), command.checkout());

        } catch (Exception e) {
            log.warn("Redis lỗi, không chọn chặn sớm --> truy vấn CSDL như bình thường");
            // vì bị lỗi redis không còn làm chốt chặn trả lời sớm --> gọi db nhưng sẽ bị chậm đi
            result = ReserveResult.UNKNOWN;
        }

        if(result == ReserveResult.REJECTED){
            log.warn("Từ chối booking {} do hết phòng", command.bookingId());
                bookingService.saveOutboxEvent(
                    new BookingFailed(
                        command.sagaId(),
                        "BookingFailed",
                        bookingService.toBookingDetail(command),
                        "Phòng bạn chọn đã hết chỗ trong những ngày này. Vui lòng chọn lại!"));
            return;
        }

        boolean isRedisDown = (result == ReserveResult.UNKNOWN);
        try {
            List<UUID> listSortedRoomTypeId = roomTypeQuantities.keySet().stream()
                                                                .distinct()
                                                                .sorted()
                                                                .toList();
            bookingService.handleCreateBooking(command, listSortedRoomTypeId, !isRedisDown);
        } catch (Exception e) {
            log.error("Lỗi khi tạo booking");
            
            if (!isRedisDown) { // tránh lỗi gọi redis, chỉ release lại số lượng khi redis còn sống và mình đã trừ
                roomInventoryRedisService.release(command.bookingId() ,roomTypeQuantities, command.checkin(), command.checkout());
            }
            throw new AppException("BOOKING_FAILED", "Tạo booking thất bại", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
