package com.hotelbooking.hotelservice.service.impl;

import com.hotelbooking.hotelservice.client.BookingServiceClient;
import com.hotelbooking.hotelservice.dto.request.HotelSearchRequest;
import com.hotelbooking.hotelservice.dto.response.*;
import com.hotelbooking.hotelservice.entity.HotelEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeEntity;
import com.hotelbooking.hotelservice.exception.HotelNotFoundException;
import com.hotelbooking.hotelservice.exception.InvalidDateRangeException;
import com.hotelbooking.hotelservice.exception.RoomTypeNotFoundException;
import com.hotelbooking.hotelservice.repository.HotelRepository;
import com.hotelbooking.hotelservice.repository.RoomTypeRepository;
import com.hotelbooking.hotelservice.service.HotelService;
import jakarta.validation.ValidationException;
import org.springframework.transaction.annotation.Transactional;
import com.hotelbooking.hotelservice.dto.HotelSearchItemDTO;
import com.hotelbooking.hotelservice.dto.CheapestRoomTypeDTO;
import com.hotelbooking.hotelservice.dto.AddressDTO;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.hotelbooking.hotelservice.dto.Hotel;

@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingServiceClient bookingServiceClient;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<HotelSummaryResponse> searchHotels(String name, String address, int page, int size) {
        String normalizedName = name == null ? "" : name.trim();
        String normalizedAddress = address == null ? "" : address.trim();

        Page<HotelEntity> hotels = hotelRepository.findByNameContainingIgnoreCaseAndAddressContainingIgnoreCase(
                normalizedName,
                normalizedAddress,
                PageRequest.of(page, size)
        );

        List<HotelSummaryResponse> data = hotels.getContent().stream()
                .map(this::toHotelSummary)
                .toList();

        return new PagedResponse<>(data, hotels.getTotalElements(), hotels.getNumber(), hotels.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDetailsResponse getHotelById(String hotelId) {
        HotelEntity hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        return toHotelDetails(hotel);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getRoomTypesByHotel(String hotelId, LocalDate checkin, LocalDate checkout) {
        validateAvailabilityInput(checkin, checkout);
        ensureHotelExists(hotelId);

        List<RoomTypeEntity> roomTypes = roomTypeRepository.findByHotel_Id(hotelId);
        if (roomTypes.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> bookedCountByRoomType = getBookedCountByRoomType(hotelId, checkin, checkout, roomTypes);

        return roomTypes.stream()
                .map(roomType -> toRoomTypeResponse(roomType, bookedCountByRoomType.get(roomType.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTypeResponse getRoomTypeById(String hotelId, String roomTypeId, LocalDate checkin, LocalDate checkout) {
        validateAvailabilityInput(checkin, checkout);
        ensureHotelExists(hotelId);

        RoomTypeEntity roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RoomTypeNotFoundException(hotelId, roomTypeId));

        Integer bookedCount = getBookedCountByRoomType(
                hotelId,
                checkin,
                checkout,
                List.of(roomType)
        ).get(roomType.getId());

        return toRoomTypeResponse(roomType, bookedCount);
    }

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
            throw new HotelNotFoundException(hotelId);
        }
    }

    private void validateAvailabilityInput(LocalDate checkin, LocalDate checkout) {
        if (checkin == null && checkout == null) {
            return;
        }

        if (checkin == null || checkout == null) {
            throw new InvalidDateRangeException("checkin and checkout must be provided together");
        }

        if (checkin.isBefore(LocalDate.now())) {
            throw new InvalidDateRangeException("checkin cannot be in the past");
        }

        if (!checkout.isAfter(checkin)) {
            throw new InvalidDateRangeException("checkout must be after checkin");
        }
    }

    private Map<String, Integer> getBookedCountByRoomType(String hotelId, LocalDate checkin, LocalDate checkout,
            List<RoomTypeEntity> roomTypes) {
        if (checkin == null || checkout == null) {
            return Collections.emptyMap();
        }

        List<String> roomTypeIds = roomTypes.stream().map(RoomTypeEntity::getId).toList();
        return bookingServiceClient.countActiveBookingsByRoomType(hotelId, roomTypeIds, checkin, checkout);
    }

    

    private HotelSummaryResponse toHotelSummary(HotelEntity entity) {
        return new HotelSummaryResponse(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                null,
                entity.getImageUrl()
        );
    }

    private HotelDetailsResponse toHotelDetails(HotelEntity entity) {
        List<String> imageUrls = entity.getImageUrl() == null ? List.of() : List.of(entity.getImageUrl());

        return new HotelDetailsResponse(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                null,
                entity.getImageUrl(),
                entity.getDescription(),
                null,
                null,
                imageUrls
        );
    }

    private RoomTypeResponse toRoomTypeResponse(RoomTypeEntity entity, Integer bookedCount) {
        Integer availableRooms = bookedCount == null ? null : Math.max(entity.getQuantity() - bookedCount, 0);
        List<String> imageUrls = entity.getImageUrl() == null ? List.of() : List.of(entity.getImageUrl());

        return new RoomTypeResponse(
                entity.getId(),
                entity.getHotel().getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getQuantity(),
                entity.getMaxGuests(),
                entity.getBasePricePerNight(),
                imageUrls,
                availableRooms
        );
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






