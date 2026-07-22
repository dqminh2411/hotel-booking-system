package com.hotelbooking.bookingservice.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hotelbooking.bookingservice.dto.CheckinRequest;
import com.hotelbooking.bookingservice.dto.RoomCheckinRequest;
import com.hotelbooking.bookingservice.entity.BookedRoomTypeEntity;
import com.hotelbooking.bookingservice.entity.BookingEntity;
import com.hotelbooking.bookingservice.enums.BookingStatus;
import com.hotelbooking.bookingservice.enums.RoomStatus;
import com.hotelbooking.bookingservice.exception.AppException;
import com.hotelbooking.bookingservice.repository.BookedRoomTypeRepository;
import com.hotelbooking.bookingservice.repository.BookingRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckinCheckoutService {
    BookingRepository bookingRepository;
    BookedRoomTypeRepository bookedRoomTypeRepository;

    @Transactional(readOnly = true)
    public RoomCheckinRequest validateAndGetCheckinData(CheckinRequest checkinRequest) {
        BookingEntity bookingEntity = bookingRepository.findById(checkinRequest.bookingId())
                .orElseThrow(() -> new AppException("BOOKING_NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND));

        if (bookingEntity.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException("BOOKING_STATUS_INVALID",
                    "Chỉ có thể check-in cho booking đã CONFIRMED (hiện tại: " + bookingEntity.getStatus() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        List<BookedRoomTypeEntity> bookedRoomTypes = bookedRoomTypeRepository.findByBookingId(checkinRequest.bookingId());
        List<RoomCheckinRequest.RoomTypeQuantity> roomTypeQuantities = bookedRoomTypes.stream()
                .map(brt -> new RoomCheckinRequest.RoomTypeQuantity(brt.getRoomTypeId(), brt.getQuantity()))
                .toList();

        // check số lượng phòng đặt với tổng số lượng phòng trong booking
        int totalRoomsBooking = roomTypeQuantities.stream()
                                    .mapToInt(RoomCheckinRequest.RoomTypeQuantity::quantity)
                                    .sum();

        List<UUID> roomIds = checkinRequest.listRoomId().values().stream()
                                    .flatMap(List::stream)
                                    .distinct()
                                    .toList();

        if (roomIds.size() != totalRoomsBooking) {
            throw new AppException("ROOM_COUNT_MISMATCH",
                    "Số phòng đã chọn (" + roomIds.size() + ") không khớp tổng số phòng đã đặt (" + totalRoomsBooking + ")",
                    HttpStatus.BAD_REQUEST);
        }

        // kiểm tra có đủ các loại roomTypeId đã đặt không
        Set<UUID> expectedRoomTypeIds = bookedRoomTypes.stream()
                .map(BookedRoomTypeEntity::getRoomTypeId).collect(Collectors.toSet());
        if (!checkinRequest.listRoomId().keySet().equals(expectedRoomTypeIds)) {
            throw new AppException("BOOKING_DATA_WRONG",
                    "Danh sách loại phòng gửi lên không khớp chính xác với các loại phòng đã đặt",
                    HttpStatus.BAD_REQUEST);
        }

        return new RoomCheckinRequest(roomIds, roomTypeQuantities, RoomStatus.AVAILABLE, RoomStatus.OCCUPIED);
    }

    @Transactional
    public void updateBookingStatusCheckin(CheckinRequest checkinRequest) {
        BookingEntity bookingEntity = bookingRepository.findById(checkinRequest.bookingId())
                .orElseThrow(() -> new AppException("BOOKING_NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND));
        List<BookedRoomTypeEntity> bookedRoomTypes = bookedRoomTypeRepository
                .findByBookingId(checkinRequest.bookingId());

        if (bookedRoomTypes == null || bookedRoomTypes.isEmpty()) {
            throw new AppException("BOOKING_DATA_WRONG", "Đơn đặt phòng này không có bất kỳ loại phòng nào được ghi",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        for(BookedRoomTypeEntity bookedRoomTypeEntity : bookedRoomTypes){
            bookedRoomTypeEntity.setRoomIds(checkinRequest.listRoomId().get(bookedRoomTypeEntity.getRoomTypeId()));
        }
        bookedRoomTypeRepository.saveAll(bookedRoomTypes);

        bookingEntity.setStatus(BookingStatus.CHECKEDIN);
        bookingRepository.save(bookingEntity);
    }

    @Transactional(readOnly = true)
    public RoomCheckinRequest validateCheckoutAndGetData(UUID bookingId){
        BookingEntity bookingEntity = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException("BOOKING_NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND));

        if (bookingEntity.getStatus() != BookingStatus.CHECKEDIN) {
            throw new AppException("BOOKING_STATUS_INVALID",
                    "Chỉ có thể check-out cho booking đã CHECKIN (hiện tại: " + bookingEntity.getStatus() + ")",
                    HttpStatus.BAD_REQUEST);
        }

        List<BookedRoomTypeEntity> bookedRoomTypes = bookedRoomTypeRepository.findByBookingId(bookingId);
        List<RoomCheckinRequest.RoomTypeQuantity> roomTypeQuantities = bookedRoomTypes.stream()
                .map(brt -> new RoomCheckinRequest.RoomTypeQuantity(brt.getRoomTypeId(), brt.getQuantity()))
                .toList();
        List<UUID> listRoomId = bookedRoomTypes.stream()
                                            .flatMap(brt -> brt.getRoomIds().stream())
                                            .distinct()
                                            .toList();
        return new RoomCheckinRequest(listRoomId, roomTypeQuantities, RoomStatus.OCCUPIED, RoomStatus.CLEANING);  
    }

    @Transactional
    public void updateBookingStatusCheckout(UUID bookingId){
        BookingEntity bookingEntity = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException("BOOKING_NOT_FOUND", "Booking does not exist", HttpStatus.NOT_FOUND));

        bookingEntity.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(bookingEntity);
    }
}
