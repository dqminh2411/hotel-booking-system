package com.hotelbooking.bookingservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.bookingservice.dto.BookingDetail;
import com.hotelbooking.bookingservice.dto.BookingResponse;
import com.hotelbooking.bookingservice.dto.CountBookingsResponse;
import com.hotelbooking.bookingservice.dto.ActiveBookingRoomType;
import com.hotelbooking.bookingservice.dto.kafka.BookingCancelled;
import com.hotelbooking.bookingservice.dto.kafka.BookingConfirmed;
import com.hotelbooking.bookingservice.dto.kafka.BookingCreated;
import com.hotelbooking.bookingservice.dto.kafka.BookingFailed;
import com.hotelbooking.bookingservice.dto.kafka.CreateBookingCommand;
import com.hotelbooking.bookingservice.entity.BookedRoomTypeEntity;
import com.hotelbooking.bookingservice.entity.BookingEntity;
import com.hotelbooking.bookingservice.entity.BookingInfoEntity;
import com.hotelbooking.bookingservice.entity.OutboxEventEntity;
import com.hotelbooking.bookingservice.entity.RoomTypeInventory;
import com.hotelbooking.bookingservice.enums.BookingStatus;
import com.hotelbooking.bookingservice.exception.AppException;
import com.hotelbooking.bookingservice.repository.BookedRoomTypeRepository;
import com.hotelbooking.bookingservice.repository.BookingInfoRepository;
import com.hotelbooking.bookingservice.repository.BookingRepository;
import com.hotelbooking.bookingservice.repository.OutboxEventRepository;
import com.hotelbooking.bookingservice.repository.RoomTypeInventoryRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingService {
    private static final String OUTBOX_TOPIC = "booking-events";
    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKEDIN);

    BookingRepository bookingRepository;
    BookedRoomTypeRepository bookedRoomTypeRepository;
    BookingInfoRepository bookingInfoRepository;
    OutboxEventRepository outboxEventRepository;
    ObjectMapper objectMapper;
    RoomTypeInventoryRepository roomTypeInventoryRepository;

    public BookingResponse getBookingById(UUID bookingId) {
        BookingEntity booking = bookingRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new AppException("BOOKING_NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND));

        JsonNode details = null;
        if (bookingInfoRepository.existsById(bookingId)) {
            BookingInfoEntity bookingInfo = bookingInfoRepository.findById(bookingId).orElse(null);
            if (bookingInfo != null) {
                try {
                    details = objectMapper.readTree(bookingInfo.getBookingDetail());
                } catch (Exception ignore) {
                    details = null;
                }
            }
        }

        return new BookingResponse(booking.getId(), booking.getStatus(), details);
    }

    public CountBookingsResponse countBookings(UUID hotelId, List<UUID> roomTypeList, LocalDate checkin, LocalDate checkout) {
        if (checkin == null || checkout == null) {
            throw new AppException("VALIDATION_ERROR", "checkin and checkout are required", HttpStatus.BAD_REQUEST);
        }
        if (!checkout.isAfter(checkin)) {
            throw new AppException("VALIDATION_ERROR", "checkout must be after checkin", HttpStatus.BAD_REQUEST);
        }
        if ((hotelId == null || hotelId.toString().isBlank()) && (roomTypeList == null || roomTypeList.isEmpty())) {
            throw new AppException("VALIDATION_ERROR", "hotelId or roomTypeList is required", HttpStatus.BAD_REQUEST);
        }

        List<UUID> safeRoomTypeList = roomTypeList == null ? List.of() : roomTypeList;
        List<ActiveBookingRoomType> activeBookingCount = bookingRepository.countActiveBookingsByRoomType(
            hotelId,
            safeRoomTypeList,
            safeRoomTypeList.isEmpty(),
            checkin,
            checkout,
            ACTIVE_STATUSES
        );

        return new CountBookingsResponse(hotelId, checkin, checkout, activeBookingCount);
    }

    @Transactional
    public BookingResponse updateBookingStatus(UUID bookingId, BookingStatus newStatus) {
        BookingEntity booking = bookingRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new AppException("BOOKING_NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND));

        BookingStatus currentStatus = booking.getStatus();
        if (currentStatus == newStatus) {
            return getBookingById(bookingId);
        }

        boolean validTransition =
            (currentStatus == BookingStatus.PENDING && (newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.FAILED))
                || (currentStatus == BookingStatus.CONFIRMED && newStatus == BookingStatus.CANCELLED);
        if (!validTransition) {
            throw new AppException(
                "INVALID_STATUS_TRANSITION",
                "Cannot transition from " + currentStatus + " to " + newStatus,
                HttpStatus.BAD_REQUEST
            );
        }

        booking.setStatus(newStatus);
        bookingRepository.save(booking);
        BookingDetail bookingDetail = getBookingDetailByBookingId(booking.getId());
        if (newStatus == BookingStatus.CONFIRMED) {
            saveOutboxEvent(new BookingConfirmed(null, "BookingConfirmed", bookingDetail));
        } else {
            saveOutboxEvent(new BookingCancelled(null, "BookingCancelled", bookingDetail, "Booking cancelled"));
        }
        return getBookingById(bookingId);
    }

    @Transactional
    public void handleCreateBooking(CreateBookingCommand command, List<UUID> sortedRoomTypeId){
        validateCreateCommand(command);
        if(bookingRepository.existsById(command.bookingId())){
            return;
        }

        Map<UUID, RoomTypeInventory> inventories = new LinkedHashMap<>();
        for(UUID roomTypeId : sortedRoomTypeId){
            RoomTypeInventory roomTypeInventory = roomTypeInventoryRepository.findByIdForTruth(roomTypeId)
                                .orElseThrow(() -> new AppException("ROOMTYPE_INVENTORY_NOT_FOUND", "Không tìm thấy roomTypeInventory có id: " + roomTypeId, HttpStatus.NOT_FOUND));

            inventories.put(roomTypeId, roomTypeInventory);
        }

        List<ActiveBookingRoomType> activeBookings = bookingRepository.countActiveBookingsByRoomType(
                    command.hotel().hotelId(),
                    sortedRoomTypeId,
                    sortedRoomTypeId.isEmpty(),
                    command.checkin(),
                    command.checkout(),
                    ACTIVE_STATUSES);
        
        Map<UUID, Long> activeByRoomType = activeBookings.stream()
                .collect(Collectors.toMap(ActiveBookingRoomType::roomTypeId, ActiveBookingRoomType::bookingCount));
        
        BookingDetail bookingDetail = toBookingDetail(command);

        BookingEntity bookingEntity = new BookingEntity();
        bookingEntity.setId(command.bookingId());
        bookingEntity.setCustomerId(command.user().userId());
        bookingEntity.setHotelId(command.hotel().hotelId());
        bookingEntity.setCreatedAt(Instant.now());
        bookingEntity.setCheckinDate(command.checkin());
        bookingEntity.setCheckoutDate(command.checkout());
        bookingEntity.setNumAdults(command.numAdults());
        bookingEntity.setTotalAmount(command.totalAmount());
        bookingEntity.setCurrency(command.currency());
        bookingEntity.setStatus(BookingStatus.PENDING);
        bookingEntity.setPaymentMethod(command.paymentMethod());
        bookingRepository.save(bookingEntity);

        boolean isRoomTypeNotEnough = false;
        long nights = ChronoUnit.DAYS.between(command.checkin(), command.checkout());
        List<UUID> unAvailable = new ArrayList<>();

        for(CreateBookingCommand.RoomTypeItem roomTypeItem : command.roomTypeList()){
            long totalBooking = activeByRoomType.getOrDefault(roomTypeItem.roomTypeId(), 0L);
            long expected = totalBooking + roomTypeItem.bookingQuantity();

            if(expected > ((RoomTypeInventory)inventories.get(roomTypeItem.roomTypeId())).getTotalQuantity()){
                isRoomTypeNotEnough = true;
                unAvailable.add(roomTypeItem.roomTypeId());
            }

            BookedRoomTypeEntity bookedRoomTypeEntity = new BookedRoomTypeEntity();
            bookedRoomTypeEntity.setId(UUID.randomUUID());
            bookedRoomTypeEntity.setBookingId(command.bookingId());
            bookedRoomTypeEntity.setRoomTypeId(roomTypeItem.roomTypeId());
            bookedRoomTypeEntity.setQuantity(roomTypeItem.bookingQuantity());
            bookedRoomTypeEntity.setPricePerNight(roomTypeItem.price());
            bookedRoomTypeEntity.setNights((int)nights);
            bookedRoomTypeEntity.setSubtotal(roomTypeItem.price().multiply(BigDecimal.valueOf(roomTypeItem.bookingQuantity())).multiply(BigDecimal.valueOf(nights)));
            bookedRoomTypeRepository.save(bookedRoomTypeEntity);
        }

        try {
            BookingInfoEntity info = new BookingInfoEntity();

            info.setBookingId(command.bookingId());
            info.setBookingDetail(objectMapper.writeValueAsString(bookingDetail));
            bookingInfoRepository.save(info);
        } catch (Exception ex) {
            throw new AppException("INTERNAL_SERVER_ERROR", "Failed to persist booking detail", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if(isRoomTypeNotEnough){
            bookingEntity.setStatus(BookingStatus.FAILED);
            bookingRepository.save(bookingEntity);
            saveOutboxEvent(
                    new BookingFailed(
                        command.sagaId(),
                        "BookingFailed",
                        toBookingDetail(command),
                        "Room " + String.join(", ", unAvailable.toString()) + " not available"
                    )
                );
                return;
        }

        saveOutboxEvent(
            new BookingCreated(
                command.sagaId(),
                "BookingCreated",
                command.bookingId(),
                command.user().userId(),
                command.totalAmount(),
                command.currency(),
                command.paymentMethod(),
                command.paymentToken()
            )
        );

        
    }

    @Transactional
    public void handleConfirmBooking(UUID sagaId, UUID bookingId) {
        updateStatusFromCommand(sagaId, bookingId, BookingStatus.CONFIRMED, "BookingConfirmed","");
    }

    @Transactional
    public void handleCancelBooking(UUID sagaId, UUID bookingId, String reason) {
        updateStatusFromCommand(sagaId, bookingId, BookingStatus.CANCELLED, "BookingCancelled", reason);
    }

    private void updateStatusFromCommand(UUID sagaId, UUID bookingId, BookingStatus targetStatus, String eventType, String reason) {
        BookingEntity booking = bookingRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new AppException("BOOKING_NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND));
        booking.setStatus(targetStatus);
        bookingRepository.save(booking);

        BookingDetail bookingDetail = getBookingDetailByBookingId(bookingId);
        if (targetStatus == BookingStatus.CONFIRMED) {
            saveOutboxEvent(new BookingConfirmed(sagaId, eventType, bookingDetail));
            return;
        }

        saveOutboxEvent(new BookingCancelled(sagaId, eventType, bookingDetail, reason));
    }

    public void saveOutboxEvent(Object event) {
        try {
            OutboxEventEntity outbox = new OutboxEventEntity();
            outbox.setId(UUID.randomUUID());
            outbox.setTopic(OUTBOX_TOPIC);
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setPublished(Boolean.FALSE);
            outbox.setCreatedAt(Instant.now());
            outboxEventRepository.save(outbox);
        } catch (Exception ex) {
            throw new AppException("INTERNAL_SERVER_ERROR", "Failed to write outbox event", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private BookingDetail getBookingDetailByBookingId(UUID bookingId) {
        BookingInfoEntity bookingInfo = bookingInfoRepository.findById(bookingId)
            .orElseThrow(() -> new AppException("BOOKING_DETAIL_NOT_FOUND", "Booking detail does not exist", HttpStatus.NOT_FOUND));

        try {
            return objectMapper.readValue(bookingInfo.getBookingDetail(), BookingDetail.class);
        } catch (Exception ex) {
            throw new AppException("INTERNAL_SERVER_ERROR", "Failed to parse booking detail", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public BookingDetail toBookingDetail(CreateBookingCommand command) {
        BookingDetail bookingDetail = new BookingDetail();
        bookingDetail.setBookingId(command.bookingId());
        bookingDetail.setCheckin(command.checkin().toString());
        bookingDetail.setCheckout(command.checkout().toString());
        bookingDetail.setNumAdults(command.numAdults());
        bookingDetail.setTotalAmount(command.totalAmount());
        bookingDetail.setHotel(new BookingDetail.Hotel(
            command.hotel().hotelId(),
            command.hotel().name(),
            command.hotel().address()
        ));
        bookingDetail.setCustomer(new BookingDetail.Customer(
            command.user().userId(),
            command.user().name(),
            command.user().email()
        ));
        bookingDetail.setRoomTypeList(
            command.roomTypeList().stream()
                .map(item -> new BookingDetail.RoomType(
                    item.roomTypeId(),
                    item.name(),
                    item.bedCount(),
                    item.bookingQuantity(),
                    item.totalQuantity(),
                    item.price()
                ))
                .toList()
        );
        return bookingDetail;
    }

    private void validateCreateCommand(CreateBookingCommand command) {
        if (command == null
            || command.user() == null
            || command.hotel() == null
            || command.roomTypeList() == null
            || command.roomTypeList().isEmpty()
            || command.checkin() == null
            || command.checkout() == null
            || command.numAdults() == null
            || command.totalAmount() == null
            || command.currency() == null
            || command.paymentMethod() == null) {
            throw new AppException("VALIDATION_ERROR", "CreateBooking payload is invalid", HttpStatus.BAD_REQUEST);
        }
        if (!command.checkout().isAfter(command.checkin())) {
            throw new AppException("VALIDATION_ERROR", "checkout must be after checkin", HttpStatus.BAD_REQUEST);
        }
        if (command.numAdults() <= 0) {
            throw new AppException("VALIDATION_ERROR", "numAdults must be positive", HttpStatus.BAD_REQUEST);
        }

        for (CreateBookingCommand.RoomTypeItem item : command.roomTypeList()) {
            if (item.roomTypeId() == null || item.bookingQuantity() == null || item.totalQuantity() == null || item.price() == null) {
                throw new AppException("VALIDATION_ERROR", "Invalid roomTypeList item", HttpStatus.BAD_REQUEST);
            }
            if (item.bookingQuantity() <= 0 || item.totalQuantity() <= 0) {
                throw new AppException("VALIDATION_ERROR", "room quantities must be positive", HttpStatus.BAD_REQUEST);
            }
        }
    }
}
