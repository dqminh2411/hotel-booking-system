package com.hotelbooking.hotelservice.service.impl;

import com.hotelbooking.hotelservice.client.BookingServiceClient;
import com.hotelbooking.hotelservice.constant.HotelStatus;
import com.hotelbooking.hotelservice.constant.ScopeType;
import com.hotelbooking.hotelservice.dto.request.*;
import com.hotelbooking.hotelservice.entity.*;
import com.hotelbooking.hotelservice.dto.response.*;
import com.hotelbooking.hotelservice.event.HotelCreatedEvent;
import com.hotelbooking.hotelservice.exception.AppException;
import com.hotelbooking.hotelservice.exception.HotelNotFoundException;
import com.hotelbooking.hotelservice.exception.InvalidDateRangeException;
import com.hotelbooking.hotelservice.mapper.HotelMapper;
import com.hotelbooking.hotelservice.mapper.RoomTypeMapper;
import com.hotelbooking.hotelservice.repository.*;
import com.hotelbooking.hotelservice.security.SecurityUtils;
import com.hotelbooking.hotelservice.service.HotelService;
import jakarta.validation.ValidationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import com.hotelbooking.hotelservice.dto.HotelSearchItemDTO;
import com.hotelbooking.hotelservice.dto.CheapestRoomTypeDTO;
import com.hotelbooking.hotelservice.dto.AddressDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import java.util.*;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
    AmenityRepository amenityRepository;
    RoomTypeAmenityRepository roomTypeAmenityRepository;
    ApplicationEventPublisher applicationEventPublisher;
    ProvinceRepository provinceRepository;
    DistrictRepository districtRepository;
    WardRepository wardRepository;
    SecurityUtils securityUtils;


    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hotel-detail-availability", key = "#hotelId + '::' + #checkinDate + '::' + #checkoutDate + '::' + #guestNum + '::' + #roomNum", unless = "#result == null")
    public HotelDetailsResponse getHotelDetail(UUID hotelId, LocalDate checkinDate, LocalDate checkoutDate,
            Integer guestNum, Integer roomNum) {
        validateAvailabilityInput(checkinDate, checkoutDate);

        System.out.println("HOTEL-DETAIL - Lấy trong db");

        HotelEntity hotel = hotelRepository.findByIdAndIsDeletedFalseAndStatus(hotelId, HotelStatus.APPROVED)
                .orElseThrow(() -> new HotelNotFoundException(hotelId.toString()));

        List<HotelImageEntity> images = hotelImageRepository.findByHotel_IdAndIsDeletedFalseOrderByIsCoverDescCreatedAtAsc(hotelId);
        List<PolicyEntity> policies = policyRepository.findByHotel_IdAndIsDeletedFalse(hotelId);
        List<AmenityEntity> hotelAmenities = hotelAmenityRepository.findActiveAmenitiesByHotelId(hotelId);

        List<RoomTypeEntity> roomTypes = roomTypeRepository
                .findActiveByHotelIdWithCoverImage(hotelId);

        Map<UUID, Integer> bookedCountByRoomType = getBookedCountByRoomType(hotelId, checkinDate, checkoutDate,
                roomTypes);

        List<RoomTypeEntity> filteredRoomTypes = filterAvailableRoomTypes(
                roomTypes, bookedCountByRoomType, guestNum, roomNum, checkinDate);

        return hotelMapper.toHotelDetailsResponse(
                hotel, images, policies, hotelAmenities, filteredRoomTypes, bookedCountByRoomType);
    }

    @Override
    @Transactional(readOnly = true)
    public HotelDetailsResponse getHotelDetailForAdmin(UUID hotelId){
        HotelEntity hotel = hotelRepository.findByIdAndIsDeletedFalse(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId.toString()));

        List<HotelImageEntity> images = hotelImageRepository.findByHotel_IdAndIsDeletedFalseOrderByIsCoverDescCreatedAtAsc(hotelId);
        List<PolicyEntity> policies = policyRepository.findByHotel_IdAndIsDeletedFalse(hotelId);
        List<AmenityEntity> hotelAmenities = hotelAmenityRepository.findActiveAmenitiesByHotelId(hotelId);
        List<RoomTypeEntity> roomTypes = roomTypeRepository.findActiveByHotelIdWithCoverImage(hotelId);

        return hotelMapper.toHotelDetailsResponse(
                hotel, images, policies, hotelAmenities, roomTypes, Map.of());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "room-types", key = "#hotelId", unless = "#result == null || #result.isEmpty()")
    public List<RoomTypeResponse> getListRoomTypeByHotelId(UUID hotelId) {
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
            LocalDate checkin) {
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
                            || rt.getMaxGuests() >= Math.ceil(1.0 * requiredGuests / requiredRooms); // maxGuest >=
                                                                                                     // ceiling(guestNum/roomNum)
                    return hasEnoughRooms && hasEnoughCapacity;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HotelAndRoomTypesResponse getRequestedRoomTypesByHotel(UUID hotelId, List<UUID> roomTypeList) {
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
    public void updateRoomStatus(RoomCheckinRequest request) {
        List<UUID> roomIds = request.roomIds();
        if (roomIds == null || roomIds.isEmpty()) {
            throw new AppException("VALIDATION_ERROR", "Danh sách phòng không được để trống", HttpStatus.BAD_REQUEST);
        }
        if (request.oldStatus() == null || request.newStatus() == null) {
            throw new AppException("VALIDATION_ERROR", "oldStatus/newStatus không được để trống",
                    HttpStatus.BAD_REQUEST);
        }

        // check room tồn tại
        List<UUID> uniqueRoomId = roomIds.stream().distinct().toList();
        List<RoomEntity> roomEntities = roomRepository.findAllById(uniqueRoomId);
        if (roomEntities.size() != uniqueRoomId.size()) {
            throw new AppException("ROOM_NOT_FOUND", "Một số roomId không tồn tại trong hệ thống",
                    HttpStatus.BAD_REQUEST);
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
                        HttpStatus.BAD_REQUEST);
            }
        }

        // check: đảm bảo mọi phòng trong roomIds đều thuộc roomType đã đặt
        Set<UUID> roomTypeIds = request.roomTypeQuantities().stream()
                .map(RoomCheckinRequest.RoomTypeQuantity::roomTypeId)
                .collect(Collectors.toSet());

        if (!roomTypeIds.containsAll(countByRoomType.keySet())) {
            throw new AppException(
                    "ROOM_TYPE_QUANTITY_MISMATCH",
                    "Danh sách phòng chứa loại phòng không được đặt trong roomTypeQuantities",
                    HttpStatus.BAD_REQUEST);
        }

        int updateRows = roomRepository.updateStatusRooms(uniqueRoomId, request.oldStatus(), request.newStatus());

        if (updateRows != uniqueRoomId.size()) {
            throw new AppException(
                    "ROOM_NOT_AVAILABLE",
                    "Cập nhật trạng thái phòng thất bại! Có phòng không ở trạng thái " + request.oldStatus()
                            + " như mong đợi. Vui lòng tải lại danh sách phòng.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @PreAuthorize("hasRole('HOTEL_OWNER')")
    @Transactional
    public CreateHotelResponse createHotel(CreateHotelRequest request) {
        UUID tenantId = securityUtils.getCurrentTenantId();

        ProvinceEntity province = provinceRepository.findById(request.getProvinceCode())
                .orElseThrow(() -> new AppException(
                        "PROVINCE_NOT_FOUND",
                        "Không tìm thấy provinceCode: " + request.getProvinceCode(),
                        HttpStatus.BAD_REQUEST));

        DistrictEntity district = districtRepository.findById(request.getDistrictCode())
                .orElseThrow(() -> new AppException(
                        "DISTRICT_NOT_FOUND",
                        "Không tìm thấy districtCode: " + request.getDistrictCode(),
                        HttpStatus.BAD_REQUEST));

        WardEntity ward = wardRepository.findById(request.getWardCode())
                .orElseThrow(() -> new AppException(
                        "WARD_NOT_FOUND",
                        "Không tìm thấy wardCode: " + request.getWardCode(),
                        HttpStatus.BAD_REQUEST));

        // Validate district thuộc đúng province, ward thuộc đúng district
        // (tránh trường hợp client gửi 3 code hợp lệ riêng lẻ nhưng không cùng 1 cây địa giới)
        if (!district.getProvince().getCode().equals(province.getCode())) {
            throw new AppException(
                    "INVALID_LOCATION_HIERARCHY",
                    "districtCode không thuộc provinceCode đã cho",
                    HttpStatus.BAD_REQUEST);
        }
        if (!ward.getDistrict().getCode().equals(district.getCode())) {
            throw new AppException(
                    "INVALID_LOCATION_HIERARCHY",
                    "wardCode không thuộc districtCode đã cho",
                    HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        HotelEntity hotel = new HotelEntity();
        hotel.setTenantId(tenantId);
        hotel.setName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setAddress(request.getAddress());
        hotel.setProvince(province);
        hotel.setDistrict(district);
        hotel.setWard(ward);
        hotel.setStatus(HotelStatus.PENDING);
        hotel.setIsDeleted(false);
        hotel.setCreatedAt(now);
        hotel.setUpdatedAt(now);

        HotelEntity saved = hotelRepository.save(hotel);

        List<AmenityResponse> amenityResponses = processAmenities(request.getAmenities(), saved);
        List<PolicyResponse> policyResponses = processPolicies(request.getPolicies(), saved);
        List<RoomTypeCreatedResponse> roomTypeResponses = processRoomTypes(request.getRoomTypes(), saved, now);

        log.info("Tạo hotel mới id={} tenantId={} status=PENDING amenities={} policies={} roomTypes={}",
                saved.getId(), tenantId, amenityResponses.size(), policyResponses.size(), roomTypeResponses.size());

        // Publish event NỘI BỘ ngay trong transaction. Kafka message thật sự
        // chỉ được gửi đi ở HotelKafkaEventPublisher, SAU KHI transaction này commit.
        applicationEventPublisher.publishEvent(new HotelCreatedEvent(
                saved.getId(),
                saved.getTenantId(),
                saved.getName(),
                saved.getCreatedAt()));

        return CreateHotelResponse.builder()
                .id(saved.getId())
                .tenantId(saved.getTenantId())
                .name(saved.getName())
                .description(saved.getDescription())
                .address(saved.getAddress())
                .provinceCode(saved.getProvince().getCode())
                .districtCode(saved.getDistrict().getCode())
                .wardCode(saved.getWard().getCode())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .amenities(amenityResponses)
                .policies(policyResponses)
                .roomTypes(roomTypeResponses)
                .build();
    }

    /**
     * Với mỗi amenity trong request: nếu có id -> dùng amenity có sẵn (phải
     * tồn tại, chưa bị xoá). Nếu không có id -> tìm theo tên (không phân biệt
     * hoa/thường); nếu đã tồn tại thì dùng lại, chưa có thì tạo mới với
     * scope = HOTEL. Sau đó tạo liên kết HotelAmenityEntity.
     */
    private List<AmenityResponse> processAmenities(List<AmenityRequest> requests, HotelEntity hotel) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<AmenityResponse> result = new ArrayList<>();
        for (AmenityRequest req : requests) {
            AmenityEntity amenity = resolveAmenity(req);

            HotelAmenityEntity link = new HotelAmenityEntity();
            link.setId(new HotelAmenityId(hotel.getId(), amenity.getId()));
            link.setHotel(hotel);
            link.setAmenity(amenity);
            link.setIsDeleted(false);
            hotelAmenityRepository.save(link);

            result.add(AmenityResponse.builder()
                    .id(amenity.getId())
                    .name(amenity.getName())
                    .build());
        }
        return result;
    }

    private AmenityEntity resolveAmenity(AmenityRequest req) {
        if (req.getId() != null) {
            return amenityRepository.findById(req.getId())
                    .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                    .orElseThrow(() -> new AppException(
                            "AMENITY_NOT_FOUND",
                            "Không tìm thấy amenity id: " + req.getId(),
                            HttpStatus.BAD_REQUEST));
        }

        if (req.getName() == null || req.getName().isBlank()) {
            throw new AppException(
                    "AMENITY_NAME_REQUIRED",
                    "Mỗi amenity phải có id (dùng cái có sẵn) hoặc name (tạo mới)",
                    HttpStatus.BAD_REQUEST);
        }

        String trimmedName = req.getName().trim();
        return amenityRepository.findByNameIgnoreCaseAndIsDeletedFalse(trimmedName)
                .orElseGet(() -> {
                    AmenityEntity newAmenity = new AmenityEntity();
                    newAmenity.setName(trimmedName);
                    newAmenity.setScope(ScopeType.HOTEL);
                    newAmenity.setIsDeleted(false);
                    AmenityEntity createdAmenity = amenityRepository.save(newAmenity);
                    log.info("Tạo amenity mới '{}' (id={})", trimmedName, createdAmenity.getId());
                    return createdAmenity;
                });
    }

    private List<PolicyResponse> processPolicies(List<PolicyRequest> requests, HotelEntity hotel) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<PolicyResponse> result = new ArrayList<>();
        for (PolicyRequest req : requests) {
            PolicyEntity policy = new PolicyEntity();
            policy.setHotel(hotel);
            policy.setType(req.getType());
            policy.setDescription(req.getDescription());
            policy.setIsDeleted(false);

            PolicyEntity saved = policyRepository.save(policy);
            result.add(PolicyResponse.builder()
                    .id(saved.getId())
                    .type(saved.getType())
                    .description(saved.getDescription())
                    .build());
        }
        return result;
    }

    /** roomTypes bắt buộc có ít nhất 1 phần tử (đã validate bằng @NotEmpty ở DTO). */
    private List<RoomTypeCreatedResponse> processRoomTypes(List<RoomTypeRequest> requests, HotelEntity hotel, Instant now) {
        List<RoomTypeCreatedResponse> result = new ArrayList<>();
        for (RoomTypeRequest req : requests) {
            RoomTypeEntity roomType = new RoomTypeEntity();
            roomType.setHotel(hotel);
            roomType.setName(req.getName());
            roomType.setDescription(req.getDescription());
            roomType.setMaxGuests(req.getMaxGuests());
            roomType.setBedCounts(req.getBedCounts());
            roomType.setBasePricePerNight(req.getBasePricePerNight());
            roomType.setQuantity(req.getQuantity());
            roomType.setArea(req.getArea());
            roomType.setIsDeleted(false);
            roomType.setCreatedAt(now);
            roomType.setUpdatedAt(now);

            RoomTypeEntity saved = roomTypeRepository.save(roomType);
            result.add(RoomTypeCreatedResponse.builder()
                    .id(saved.getId())
                    .name(saved.getName())
                    .description(saved.getDescription())
                    .maxGuests(saved.getMaxGuests())
                    .bedCounts(saved.getBedCounts())
                    .basePricePerNight(saved.getBasePricePerNight())
                    .quantity(saved.getQuantity())
                    .area(saved.getArea())
                    .build());
        }
        return result;
    }



    // đây là phần Long thêm và sửa

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "hotel-search", key = "#request.locationCode + '::' + #request.checkinDate + '::' + #request.checkoutDate + '::' + #request.guestNum + '::' + #request.roomNum", unless = "#result == null || #result.data.isEmpty()")
    public PagedResponse<HotelSearchItemDTO> search(HotelSearchRequest request) {
        System.out.println(request.getLocationCode());
        validateRequest(request);

        List<HotelEntity> hotels = findHotelByLocation(request);

        if (hotels.isEmpty()) {
            return new PagedResponse<>(List.of(), 0, request.getPage(), request.getSize());
        }

        List<UUID> hotelIdsByAmenities = filterAmenities(hotels, request.getAmenities());

        Set<UUID> hotelIdSet = new HashSet<>(hotelIdsByAmenities);

        hotels = hotels.stream()
                .filter(h -> hotelIdSet.contains(h.getId()))
                .toList();

        if (hotelIdsByAmenities.isEmpty()) {
            return new PagedResponse<>(
                    List.of(),
                    0,
                    request.getPage(),
                    request.getSize());
        } // nếu không có hotel thoả mãn thì trả ve page rỗng

        List<RoomTypeEntity> roomTypes = searchRoomTypes(
                hotelIdsByAmenities,
                request.getGuestNum(),
                request.getMinPrice(),
                request.getMaxPrice());

        if (roomTypes.isEmpty()) {
            return new PagedResponse<>(
                    List.of(),
                    0,
                    request.getPage(),
                    request.getSize());
        }

        List<UUID> roomTypeIds = extractRoomTypeIds(roomTypes);

        Map<UUID, List<RoomTypeEntity>> groupedRoomTypes = groupRoomTypeByHotel(roomTypes);

        Map<UUID, Integer> bookingCounts = countActiveBooking(roomTypeIds, request.getCheckinDate(),
                request.getCheckoutDate());

        List<HotelSearchItemDTO> items = buildResponse(
                hotels,
                groupedRoomTypes,
                bookingCounts,
                request.getRoomNum());

        sortItems(items, request.getSortBy()); // sắp xếp theo sortBy

        return paginate(
                items,
                request.getPage(),
                request.getSize()); // phân trang

    }

    private void validateRequest(HotelSearchRequest request) {
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

        if (request.getMinPrice() != null && request.getMaxPrice() != null &&
                request.getMinPrice().compareTo(request.getMaxPrice()) > 0)
            throw new ValidationException();

        if (request.getGuestNum() <= 0)
            throw new ValidationException();

        if (request.getRoomNum() <= 0)
            throw new ValidationException();

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

    private List<RoomTypeEntity> searchRoomTypes(List<UUID> hotelIds, int guestNum, BigDecimal minPrice,
            BigDecimal maxPrice) {
        return roomTypeRepository.searchRoomTypes(hotelIds, guestNum, minPrice, maxPrice);
    }

    private List<UUID> extractRoomTypeIds(List<RoomTypeEntity> roomTypes) {
        return roomTypes.stream().map(RoomTypeEntity::getId).toList();
    }

    private Map<UUID, List<RoomTypeEntity>> groupRoomTypeByHotel(List<RoomTypeEntity> roomTypes) {
        return roomTypes.stream()
                .collect(Collectors.groupingBy(
                        room -> room.getHotel().getId()));
    }

    private Map<UUID, Integer> countActiveBooking(List<UUID> roomTypeIds, LocalDate checkin, LocalDate checkout) {
        return bookingServiceClient.countActiveBookingsByRoomType(
                null,
                roomTypeIds,
                checkin,
                checkout);
    } // Phương thức này chỉ là wrapper gọi sang BookingServiceClient (Đây là business
      // logic của Booking service)

    private List<HotelSearchItemDTO> buildResponse(
            List<HotelEntity> hotels,
            Map<UUID, List<RoomTypeEntity>> groupedRoomTypes,
            Map<UUID, Integer> bookingCounts,
            int roomNum) {

        List<HotelSearchItemDTO> items = new ArrayList<>();

        for (HotelEntity hotel : hotels) {

            List<RoomTypeEntity> roomTypes = groupedRoomTypes.getOrDefault(hotel.getId(), List.of());

            if (roomTypes.isEmpty()) {
                continue;
            }

            List<RoomTypeEntity> availableRoomTypes = new ArrayList<>();

            for (RoomTypeEntity roomType : roomTypes) {

                int bookingCount = bookingCounts.getOrDefault(roomType.getId(), 0);

                int availableRooms = roomType.getQuantity() - bookingCount;

                if (availableRooms >= roomNum) {
                    availableRoomTypes.add(roomType);
                }
            }

            if (availableRoomTypes.isEmpty()) {
                continue;
            }

            RoomTypeEntity cheapestRoom = availableRoomTypes.stream()
                    .min(Comparator.comparing(RoomTypeEntity::getBasePricePerNight))
                    .orElseThrow();

            int availableRooms = cheapestRoom.getQuantity()
                    - bookingCounts.getOrDefault(cheapestRoom.getId(), 0);

            CheapestRoomTypeDTO cheapestRoomDto = CheapestRoomTypeDTO.builder()
                    // đổi sang String nếu DTO sửa lại
                    .roomTypeId(cheapestRoom.getId())
                    .name(cheapestRoom.getName())
                    .pricePerNight(cheapestRoom.getBasePricePerNight())
                    .availableRooms(availableRooms)
                    .build();

            AddressDTO address = AddressDTO.builder()
                    .fullAddress(hotel.getAddress())
                    .province(null)
                    .district(null)
                    .ward(null)
                    .build();

            HotelSearchItemDTO item = HotelSearchItemDTO.builder()
                    .hotelId(hotel.getId())
                    .name(hotel.getName())
                    // .coverImageUrl(hotel.getImageUrl())
                    .coverImageUrl(hotel.getHotelImages().getFirst().getUrl())
                    .address(address)
                    .policies(List.of())
                    .cheapestRoomType(cheapestRoomDto)
                    .build();

            items.add(item);
        }

        return items;
    }

    // trả về list hotelIds hợp lệ
    private List<UUID> filterAmenities(
            List<HotelEntity> hotels, // danh sách hotels đã lọc theo location
            List<UUID> amenityIds // danh sách các amenity theo id từ request
    ) {
        List<UUID> hotelIds = extractHotelIds(hotels);

        if (amenityIds == null || amenityIds.isEmpty()) {
            return extractHotelIds(hotels);
        }
        // lấy danh sách amenity theo danh sách id
        List<AmenityEntity> amenities = amenityRepository.findAllById(amenityIds);

        if (amenities.size() != amenityIds.size()) {
            throw new ValidationException();
        }

        List<UUID> currentHotelIds = new ArrayList<>();

        List<UUID> resultHotelIds = null;

        // chia amenities theo scope
        for (AmenityEntity amenity : amenities) {

            switch (amenity.getScope()) {
                case HOTEL -> currentHotelIds = hotelAmenityRepository.findHotelIdsByAmenityIds(hotelIds,
                        List.of(amenity.getId()), 1);
                case ROOM_TYPE -> {
                    List<UUID> roomTypeIds = roomTypeAmenityRepository.findRoomTypeIdsByAmenityIds(hotelIds,
                            List.of(amenity.getId()), 1);
                    currentHotelIds = roomTypeRepository.findHotelIdsByRoomTypeIds(roomTypeIds);
                }
                case BOTH -> {
                    List<UUID> hotelIdsFromHotel = hotelAmenityRepository.findHotelIdsByAmenityIds(hotelIds,
                            List.of(amenity.getId()), 1);

                    List<UUID> roomTypeIds = roomTypeAmenityRepository.findRoomTypeIdsByAmenityIds(hotelIds,
                            List.of(amenity.getId()), 1);

                    List<UUID> hotelIdsFromRoomType = roomTypeRepository.findHotelIdsByRoomTypeIds(roomTypeIds);

                    Set<UUID> union = new HashSet<>(hotelIdsFromHotel);
                    union.addAll(hotelIdsFromRoomType);

                    currentHotelIds = new ArrayList<>(union);

                }
            }

            if (resultHotelIds == null) {
                resultHotelIds = new ArrayList<>(currentHotelIds);
            } else {
                resultHotelIds.retainAll(currentHotelIds);
            }

        }
        return resultHotelIds;
    }

    // sắp xếp kết quả tìm kiếm

    private void sortItems(List<HotelSearchItemDTO> items, String sortBy) {
        if (sortBy == null) {
            return;
        }

        switch (sortBy) {

            case "priceAsc" -> items.sort(
                    Comparator.comparing(
                            item -> item.getCheapestRoomType()
                                    .getPricePerNight()));

            case "priceDesc" -> items.sort(
                    Comparator.comparing((HotelSearchItemDTO item) -> item.getCheapestRoomType().getPricePerNight())
                            .reversed());

            default -> {
            }

        }
    }

    private PagedResponse<HotelSearchItemDTO> paginate(
            List<HotelSearchItemDTO> items,
            int page,
            int size) {

        int totalElements = items.size();

        int fromIndex = page * size;

        if (fromIndex >= totalElements) {
            return new PagedResponse<>(
                    List.of(),
                    totalElements,
                    page,
                    size);
        }

        int toIndex = Math.min(fromIndex + size, totalElements);

        List<HotelSearchItemDTO> pageItems = new ArrayList<>(items.subList(fromIndex, toIndex));

        return new PagedResponse<>(
                pageItems,
                totalElements,
                page,
                size);
    }
}