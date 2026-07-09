package com.hotelbooking.hotelservice.service.impl;

import com.hotelbooking.hotelservice.client.BookingServiceClient;
import com.hotelbooking.hotelservice.constant.HotelStatus;
import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeResponse;
import com.hotelbooking.hotelservice.entity.AmenityEntity;
import com.hotelbooking.hotelservice.dto.request.HotelSearchRequest;
import com.hotelbooking.hotelservice.dto.response.*;
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
import jakarta.validation.ValidationException;
import org.springframework.transaction.annotation.Transactional;
import com.hotelbooking.hotelservice.dto.HotelSearchItemDTO;
import com.hotelbooking.hotelservice.dto.CheapestRoomTypeDTO;
import com.hotelbooking.hotelservice.dto.AddressDTO;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.hotelbooking.hotelservice.dto.Hotel;

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
    @Override
    @Transactional(readOnly = true)
    public HotelAndRoomTypesResponse getRequestedRoomTypesByHotel(String hotelId, List<String> roomTypeList) {
        HotelDetailsResponse hotelDetails = getHotelById(hotelId);
        Hotel h = new Hotel(hotelDetails.hotelId(), hotelDetails.name(), hotelDetails.address());
        List<RoomTypeEntity> roomTypes = roomTypeRepository.findByHotel_IdAndIdIn(hotelId, roomTypeList);
        if (roomTypes.isEmpty()) {
            return new HotelAndRoomTypesResponse(h, List.of());
        }

        List<RoomTypeQuantityResponse> roomTypeQuantities = roomTypes.stream()
                .map(roomType -> new RoomTypeQuantityResponse(roomType.getId(), roomType.getQuantity()))
                .toList();

        return new HotelAndRoomTypesResponse(h, roomTypeQuantities);
    }


    private void ensureHotelExists(String hotelId) {
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

    // đây là phần Long thêm và sửa

    @Override
    public PagedResponse<HotelSearchItemDTO> search(HotelSearchRequest request) {

        validateRequest(request);

        List<HotelEntity> hotels = findHotelByLocation(request);

        if (hotels.isEmpty()) {
            return new PagedResponse<>(List.of(), 0, request.getPage(), request.getSize());
        }

        List<String> hotelIds = extractHotelIds(hotels);

        List<RoomTypeEntity> roomTypes = findRoomTypes(hotelIds, request.getGuestNum());

        if (roomTypes.isEmpty()) {
            return new PagedResponse<>(
                    List.of(),
                    0,
                    request.getPage(),
                    request.getSize()
            );
        }

        List<String> roomTypeIds = extractRoomTypeIds(roomTypes);

        Map<String, List<RoomTypeEntity>> groupedRoomTypes = groupRoomTypeByHotel(roomTypes);

        Map<String, Integer> bookingCounts = countActiveBooking(roomTypeIds, request.getCheckinDate(), request.getCheckoutDate());

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

    private List<String> extractHotelIds(List<HotelEntity> hotels) {
        return hotels.stream().map(HotelEntity::getId).toList();
    }

    private List<RoomTypeEntity> findRoomTypes(List<String> hotelIds, int guestNum) {
        return roomTypeRepository.findByHotelIdsAndGuestNum(hotelIds, guestNum);
    }

    private List<String> extractRoomTypeIds(List<RoomTypeEntity> roomTypes) {
        return roomTypes.stream().map(RoomTypeEntity::getId).toList();
    }

    private Map<String, List<RoomTypeEntity>> groupRoomTypeByHotel(List<RoomTypeEntity> roomTypes) {
        return roomTypes.stream()
                .collect(Collectors.groupingBy(
                        room -> room.getHotel().getId()
        ));
    }



    private Map<String, Integer> countActiveBooking(List<String> roomTypeIds, LocalDate checkin, LocalDate checkout) {
        return bookingServiceClient.countActiveBookingsByRoomType(
                null,
                roomTypeIds,
                checkin,
                checkout
        );
    } // Phương thức này chỉ là wrapper gọi sang BookingServiceClient (Đây là business logic của Booking service)

    private PagedResponse<HotelSearchItemDTO> buildResponse(
            List<HotelEntity> hotels,
            Map<String, List<RoomTypeEntity>> groupedRoomTypes,
            Map<String, Integer> bookingCounts,
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
                            .roomTypeId(cheapestRoom.getId())
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
                            .hotelId(hotel.getId())
                            .name(hotel.getName())
                            .coverImageUrl(hotel.getImageUrl())
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






