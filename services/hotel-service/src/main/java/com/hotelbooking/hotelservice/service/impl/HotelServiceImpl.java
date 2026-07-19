package com.hotelbooking.hotelservice.service.impl;

import com.hotelbooking.hotelservice.client.BookingServiceClient;
import com.hotelbooking.hotelservice.constant.HotelStatus;
import com.hotelbooking.hotelservice.constant.RoomStatus;
import com.hotelbooking.hotelservice.entity.AmenityEntity;
import com.hotelbooking.hotelservice.dto.request.HotelSearchRequest;
import com.hotelbooking.hotelservice.dto.request.RoomCheckinRequest;
import com.hotelbooking.hotelservice.dto.response.*;
import com.hotelbooking.hotelservice.entity.HotelEntity;
import com.hotelbooking.hotelservice.entity.HotelImageEntity;
import com.hotelbooking.hotelservice.entity.PolicyEntity;
import com.hotelbooking.hotelservice.entity.RoomEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeEntity;
import com.hotelbooking.hotelservice.exception.AppException;
import com.hotelbooking.hotelservice.exception.HotelNotFoundException;
import com.hotelbooking.hotelservice.exception.InvalidDateRangeException;
import com.hotelbooking.hotelservice.mapper.HotelMapper;
import com.hotelbooking.hotelservice.mapper.RoomTypeMapper;
import com.hotelbooking.hotelservice.repository.HotelAmenityRepository;
import com.hotelbooking.hotelservice.repository.HotelImageRepository;
import com.hotelbooking.hotelservice.repository.HotelRepository;
import com.hotelbooking.hotelservice.repository.PolicyRepository;
import com.hotelbooking.hotelservice.repository.RoomRepository;
import com.hotelbooking.hotelservice.repository.RoomTypeRepository;
import com.hotelbooking.hotelservice.service.HotelService;
import jakarta.validation.ValidationException;
import org.springframework.transaction.annotation.Transactional;
import com.hotelbooking.hotelservice.dto.HotelSearchItemDTO;
import com.hotelbooking.hotelservice.dto.CheapestRoomTypeDTO;
import com.hotelbooking.hotelservice.dto.AddressDTO;

import java.time.LocalDate;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
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
    RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hotel-detail-availability",
                key = "#hotelId + '::' + #checkinDate + '::' + #checkoutDate + '::' + #guestNum + '::' + #roomNum",
                unless = "#result == null"
            )
    public HotelDetailsResponse getHotelDetail(UUID hotelId, LocalDate checkinDate, LocalDate checkoutDate, Integer guestNum, Integer roomNum) {
        validateAvailabilityInput(checkinDate, checkoutDate);

        System.out.println("HOTEL-DETAIL - Lấy trong db");

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
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "room-types", key = "#hotelId", unless = "#result == null || #result.isEmpty()")
    public List<RoomTypeResponse> getListRoomTypeByHotelId(UUID hotelId){
        ensureHotelExists(hotelId);

        System.out.println(" LIST ROOMTYPES - Lấy data từ db");

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

    @Override
    @Transactional
    public void updateRoomStatus(RoomCheckinRequest request){
        List<UUID> roomIds = request.roomIds();
        if (roomIds == null || roomIds.isEmpty()) {
            throw new AppException("VALIDATION_ERROR", "Danh sách phòng check-in không được để trống", HttpStatus.BAD_REQUEST);
        }

        // check room tồn tại
        List<UUID> uniqueRoomId = roomIds.stream().distinct().toList();
        List<RoomEntity> roomEntities = roomRepository.findAllById(uniqueRoomId);
        if(roomEntities.size() != uniqueRoomId.size()){
            throw new AppException("ROOM_NOT_FOUND", "Một số roomId không tồn tại trong hệ thống", HttpStatus.BAD_REQUEST);
        }

        // check đúng số lượng đặt theo roomType
        Map<UUID, Long> countByRoomType = roomEntities.stream()
            .collect(Collectors.groupingBy(room -> room.getRoomType().getId(), Collectors.counting()));

        for (RoomCheckinRequest.RoomTypeQuantity expected : request.roomTypeQuantities()) {
            long count = countByRoomType.getOrDefault(expected.roomTypeId(), 0L);
            if (count != expected.quantity()) {
                throw new AppException(
                    "ROOM_TYPE_QUANTITY_MISMATCH",
                    "Số phòng chọn cho loại phòng " + expected.roomTypeId()
                        + " không khớp (cần " + expected.quantity() + ", nhận " + count + ")",
                    HttpStatus.BAD_REQUEST
                );
            }
        }

        int updateRows = roomRepository.updateStatusRooms(uniqueRoomId, RoomStatus.AVAILABLE, RoomStatus.OCCUPIED);

        if (updateRows != uniqueRoomId.size()) {
            throw new AppException(
                "ROOM_NOT_AVAILABLE", 
                "Check-in thất bại! Có phòng trong danh sách không ở trạng thái trống (AVAILABLE). Vui lòng tải lại danh sách phòng.", 
                HttpStatus.BAD_REQUEST
            );
        }
    }


    // đây là phần Long thêm và sửa

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hotel-search", 
    key = "#locationCode + '::' + #checkinDate + '::' + #checkoutDate + '::' + #guestNum + '::' + #roomNum", 
    unless = "#result == null || #result.data.isEmpty()")
    public PagedResponse<HotelSearchItemDTO> search(HotelSearchRequest request) {
        System.out.println("HOTEL-SEARCH - Lấy trong db");
        validateRequest(request);

        List<HotelEntity> hotels = findHotelByLocation(request);

        if (hotels.isEmpty()) {
            return new PagedResponse<>(List.of(), 0, request.getPage(), request.getSize());
        }

        List<UUID> hotelIds = extractHotelIds(hotels);

        List<RoomTypeEntity> roomTypes = findRoomTypes(hotelIds, request.getGuestNum());

        if (roomTypes.isEmpty()) {
            return new PagedResponse<>(
                    List.of(),
                    0,
                    request.getPage(),
                    request.getSize()
            );
        }

        List<UUID> roomTypeIds = extractRoomTypeIds(roomTypes);

        Map<UUID, List<RoomTypeEntity>> groupedRoomTypes = groupRoomTypeByHotel(roomTypes);

        Map<UUID, Integer> bookingCounts = countActiveBooking(roomTypeIds, request.getCheckinDate(), request.getCheckoutDate());

        return buildResponse(hotels,
                groupedRoomTypes,
                bookingCounts,
                request.getRoomNum(),
                request.getPage(),
                request.getSize());

    }

    private void validateRequest(HotelSearchRequest request)  {
        String locationCode = request.getLocationCode();

        if (locationCode == null ||
                !(locationCode.length() == 2
                        || locationCode.length() == 3
                        || locationCode.length() == 5)) {

                throw new ValidationException();

        }

        if (request.getCheckinDate() == null
                || request.getCheckoutDate() == null
                || !request.getCheckinDate().isBefore(request.getCheckoutDate())) {
            throw new ValidationException();
        }

        if (request.getGuestNum() <= 0) throw new ValidationException();

        if (request.getRoomNum() <= 0) throw new ValidationException();

    }

    //
    private List<HotelEntity> findHotelByLocation(HotelSearchRequest request) {

        String locationCode = request.getLocationCode();

        return switch (locationCode.length()) {
            case 2 -> hotelRepository.findByProvinceCode(locationCode);
            case 3 -> hotelRepository.findByDistrictCode(locationCode);
            case 5 -> hotelRepository.findByWardCode(locationCode);
            default -> throw new ValidationException();
        };

    }

    private List<UUID> extractHotelIds(List<HotelEntity> hotels) {
        return hotels.stream().map(HotelEntity::getId).toList();
    }

    private List<RoomTypeEntity> findRoomTypes(List<UUID> hotelIds, int guestNum) {
        return roomTypeRepository.findByHotelIdsAndGuestNum(hotelIds, guestNum);
    }

    private List<UUID> extractRoomTypeIds(List<RoomTypeEntity> roomTypes) {
        return roomTypes.stream().map(RoomTypeEntity::getId).toList();
    }

    private Map<UUID, List<RoomTypeEntity>> groupRoomTypeByHotel(List<RoomTypeEntity> roomTypes) {
        return roomTypes.stream()
                .collect(Collectors.groupingBy(
                        room -> room.getHotel().getId()
        ));
    }



    private Map<UUID, Integer> countActiveBooking(List<UUID> roomTypeIds, LocalDate checkin, LocalDate checkout) {
        return bookingServiceClient.countActiveBookingsByRoomType(
                null,
                roomTypeIds,
                checkin,
                checkout
        );
    } // Phương thức này chỉ là wrapper gọi sang BookingServiceClient (Đây là business logic của Booking service)

    private PagedResponse<HotelSearchItemDTO> buildResponse(
            List<HotelEntity> hotels,
            Map<UUID, List<RoomTypeEntity>> groupedRoomTypes,
            Map<UUID, Integer> bookingCounts,
            int roomNum,
            int page,
            int size) {

        List<HotelSearchItemDTO> items = new ArrayList<>();

        for (HotelEntity hotel : hotels) {

            List<RoomTypeEntity> roomTypes =
                    groupedRoomTypes.getOrDefault(hotel.getId(), List.of());

            if (roomTypes.isEmpty()) {
                continue;
            }

            List<RoomTypeEntity> availableRoomTypes = new ArrayList<>();

            for (RoomTypeEntity roomType : roomTypes) {

                int bookingCount =
                        bookingCounts.getOrDefault(roomType.getId(), 0);

                int availableRooms =
                        roomType.getQuantity() - bookingCount;

                if (availableRooms >= roomNum) {
                    availableRoomTypes.add(roomType);
                }
            }

            if (availableRoomTypes.isEmpty()) {
                continue;
            }

            RoomTypeEntity cheapestRoom =
                    availableRoomTypes.stream()
                            .min(Comparator.comparing(RoomTypeEntity::getBasePricePerNight))
                            .orElseThrow();

            int availableRooms =
                    cheapestRoom.getQuantity()
                            - bookingCounts.getOrDefault(cheapestRoom.getId(), 0);

            CheapestRoomTypeDTO cheapestRoomDto =
                    CheapestRoomTypeDTO.builder()
                            // đổi sang String nếu DTO sửa lại
                            .roomTypeId(cheapestRoom.getId().toString())
                            .name(cheapestRoom.getName())
                            .pricePerNight(cheapestRoom.getBasePricePerNight())
                            .availableRooms(availableRooms)
                            .build();

            AddressDTO address =
                    AddressDTO.builder()
                            .fullAddress(hotel.getAddress())
                            .province(null)
                            .district(null)
                            .ward(null)
                            .build();

            HotelSearchItemDTO item =
                    HotelSearchItemDTO.builder()
                            .hotelId(hotel.getId().toString())
                            .name(hotel.getName())
                            // .coverImageUrl(hotel.getImageUrl())
                            .coverImageUrl(hotel.getHotelImages().get(0).getUrl()) // đang 0 biết nên sửa kiểu gì vì entity đnag 0 có imageURL nên lấy tạm cái ảnh đầu
                            .address(address)
                            .policies(List.of())
                            .cheapestRoomType(cheapestRoomDto)
                            .build();

            items.add(item);
        }

        return new PagedResponse<>(
                items,
                items.size(),
                page,
                size
        );
    }
}






