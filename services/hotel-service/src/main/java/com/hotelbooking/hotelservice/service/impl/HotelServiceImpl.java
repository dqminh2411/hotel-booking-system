package com.hotelbooking.hotelservice.service.impl;

import com.hotelbooking.hotelservice.client.BookingServiceClient;
import com.hotelbooking.hotelservice.constant.HotelStatus;
import com.hotelbooking.hotelservice.dto.response.HotelAndRoomTypesResponse;
import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;
import com.hotelbooking.hotelservice.dto.response.HotelSummaryResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeQuantityResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeResponse;
import com.hotelbooking.hotelservice.entity.AmenityEntity;
import com.hotelbooking.hotelservice.entity.HotelEntity;
import com.hotelbooking.hotelservice.entity.HotelImageEntity;
import com.hotelbooking.hotelservice.entity.PolicyEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeEntity;
import com.hotelbooking.hotelservice.exception.HotelNotFoundException;
import com.hotelbooking.hotelservice.exception.InvalidDateRangeException;
import com.hotelbooking.hotelservice.mapper.HotelMapper;
import com.hotelbooking.hotelservice.mapper.RoomTypeMapper;
import com.hotelbooking.hotelservice.repository.HotelAmenityRepository;
import com.hotelbooking.hotelservice.repository.HotelImageRepository;
import com.hotelbooking.hotelservice.repository.HotelRepository;
import com.hotelbooking.hotelservice.repository.PolicyRepository;
import com.hotelbooking.hotelservice.repository.RoomTypeRepository;
import com.hotelbooking.hotelservice.service.HotelService;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HotelServiceImpl implements HotelService {

    HotelRepository hotelRepository;
    RoomTypeRepository roomTypeRepository;
    BookingServiceClient bookingServiceClient;
    HotelMapper hotelMapper;
    HotelImageRepository hotelImageRepository;
    PolicyRepository policyRepository;
    HotelAmenityRepository hotelAmenityRepository;
    RoomTypeMapper roomTypeMapper;

    @Override
    @Transactional(readOnly = true)
    public HotelDetailsResponse getHotelDetail(UUID hotelId, LocalDate checkinDate, LocalDate checkoutDate, Integer guestNum, Integer roomNum) {
        validateAvailabilityInput(checkinDate, checkoutDate);

        HotelEntity hotel = hotelRepository.findByIdAndIsDeletedFalseAndStatus(hotelId, HotelStatus.APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(hotelId.toString()));

        List<HotelImageEntity> images = hotelImageRepository.findByHotel_IdOrderByIsCoverDescCreatedAtAsc(hotelId);
        List<PolicyEntity> policies = policyRepository.findByHotel_IdAndIsDeletedFalse(hotelId);
        List<AmenityEntity> hotelAmenities = hotelAmenityRepository.findActiveAmenitiesByHotelId(hotelId);
        
        List<RoomTypeEntity> roomTypes = roomTypeRepository
                .findActiveByHotelIdWithCoverImage(hotelId);

        Map<UUID, Integer> bookedCountByRoomType =
                getBookedCountByRoomType(hotelId, checkinDate, checkoutDate, roomTypes);


        List<RoomTypeEntity> filteredRoomTypes = filterAvailableRoomTypes(
                roomTypes, bookedCountByRoomType, guestNum, roomNum, checkinDate
        );    
        
        return hotelMapper.toHotelDetailsResponse(
                hotel, images, policies, hotelAmenities, filteredRoomTypes, bookedCountByRoomType
        );
    }

    @Override
    public List<RoomTypeResponse> getListRoomTypeByHotelId(UUID hotelId){
        ensureHotelExists(hotelId);

        List<RoomTypeEntity> roomTypes = roomTypeRepository
                .findActiveByHotelIdWithCoverImage(hotelId);

        return roomTypeMapper.toListRoomtypeResponse(roomTypes);
    }

    private void ensureHotelExists(UUID hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new HotelNotFoundException(hotelId.toString());
        }
    }

    private void validateAvailabilityInput(LocalDate checkin, LocalDate checkout) {
        if (checkin == null && checkout == null) {
            return;
        }

        if (checkin == null || checkout == null) {
            throw new InvalidDateRangeException("Checkin và Checkout phải được cung cấp cùng nhau");
        }

        if (checkin.isBefore(LocalDate.now())) {
            throw new InvalidDateRangeException("Checkin không thể là ngày trong quá khứ");
        }

        if (!checkout.isAfter(checkin)) {
            throw new InvalidDateRangeException("Ngày checkoutDate phải sau ngày checkinDate");
        }
    }

    private Map<UUID, Integer> getBookedCountByRoomType(UUID hotelId, LocalDate checkin, LocalDate checkout,
            List<RoomTypeEntity> roomTypes) {
        if (checkin == null || checkout == null) {
            return Collections.emptyMap();
        }

        List<UUID> roomTypeIds = roomTypes.stream()
                                        .map(RoomTypeEntity::getId)
                                        .toList();
                                        
        return bookingServiceClient.countActiveBookingsByRoomType(hotelId, roomTypeIds, checkin, checkout);
    }

    private List<RoomTypeEntity> filterAvailableRoomTypes(
            List<RoomTypeEntity> roomTypes,
            Map<UUID, Integer> bookedCountByRoomType,
            Integer guestNum,
            Integer roomNum,
            LocalDate checkin
    ) {
        if (checkin == null) {
            return roomTypes;
        }
        

        int requiredRooms = (roomNum != null && roomNum > 0) ? roomNum : 1;
        int requiredGuests = (guestNum != null && guestNum > 0) ? guestNum : 0;

        return roomTypes.stream()
                .filter(rt -> {
                    int booked = bookedCountByRoomType.getOrDefault(rt.getId(), 0);
                    int available = rt.getQuantity() - booked;
                    boolean hasEnoughRooms = available >= requiredRooms; // availableRoom >= roomNum
                    boolean hasEnoughCapacity = requiredGuests == 0
                            || rt.getMaxGuests() >= Math.ceil(1.0 * requiredGuests/requiredRooms); //maxGuest >= ceiling(guestNum/roomNum) 
                    return hasEnoughRooms && hasEnoughCapacity;
                })
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public HotelAndRoomTypesResponse getRequestedRoomTypesByHotel(UUID hotelId, List<UUID> roomTypeList){
        HotelEntity hotel = hotelRepository.findByIdAndIsDeletedFalseAndStatus(hotelId, HotelStatus.APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(hotelId.toString()));
        HotelSummaryResponse h = new HotelSummaryResponse(hotel.getId(), hotel.getName(), hotel.getAddress());
        List<RoomTypeEntity> roomTypes = roomTypeRepository.findByHotel_IdAndIdIn(hotelId, roomTypeList);
        if (roomTypes.isEmpty()) {
            return new HotelAndRoomTypesResponse(h, List.of());
        }

        List<RoomTypeQuantityResponse> roomTypeQuantities = roomTypes.stream()
                .map(roomType -> new RoomTypeQuantityResponse(roomType.getId(), roomType.getQuantity()))
                .toList();

        return new HotelAndRoomTypesResponse(h, roomTypeQuantities);
    };

}



