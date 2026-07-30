package com.hotelbooking.hotelservice.mapper;

import com.hotelbooking.hotelservice.dto.other.District;
import com.hotelbooking.hotelservice.dto.other.HotelAmenity;
import com.hotelbooking.hotelservice.dto.other.HotelPolicy;
import com.hotelbooking.hotelservice.dto.other.Image;
import com.hotelbooking.hotelservice.dto.other.Province;
import com.hotelbooking.hotelservice.dto.other.Ward;
import com.hotelbooking.hotelservice.dto.response.Address;
import com.hotelbooking.hotelservice.dto.response.HotelDetailsResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeResponse;
import com.hotelbooking.hotelservice.entity.AmenityEntity;
import com.hotelbooking.hotelservice.entity.HotelEntity;
import com.hotelbooking.hotelservice.entity.HotelImageEntity;
import com.hotelbooking.hotelservice.entity.PolicyEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class HotelMapper {

    public HotelDetailsResponse toHotelDetailsResponse(
            HotelEntity hotel,
            List<HotelImageEntity> images,
            List<PolicyEntity> policies,
            List<AmenityEntity> hotelAmenities,
            List<RoomTypeEntity> roomTypes,
            Map<UUID, Integer> bookedCountByRoomType) {
        return new HotelDetailsResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getDescription(),
                toAddress(hotel),
                buildImages(images),
                buildPolicies(policies),
                buildAmenities(hotelAmenities),
                buildRoomTypeResponses(roomTypes, bookedCountByRoomType),
                hotel.getStatus().name());
    }

    public Address toAddress(HotelEntity hotel) {
        return new Address(
                hotel.getAddress(),
                new Province(hotel.getProvince().getCode(), hotel.getProvince().getName()),
                new District(hotel.getDistrict().getCode(), hotel.getDistrict().getName()),
                new Ward(hotel.getWard().getCode(), hotel.getWard().getName()));
    }

    public List<Image> buildImages(List<HotelImageEntity> images) {
        return images.stream()
                .sorted(Comparator.comparing(img -> !img.getIsCover()))
                .limit(5)
                .map(img -> new Image(img.getId(), img.getUrl(), img.getIsCover()))
                .toList();
    }

    public List<HotelPolicy> buildPolicies(List<PolicyEntity> policies) {
        return policies.stream()
                .map(p -> new HotelPolicy(p.getId(), p.getType(), p.getDescription()))
                .toList();
    }

    public List<HotelAmenity> buildAmenities(List<AmenityEntity> hotelAmenities) {
        return hotelAmenities.stream()
                .map(ha -> new HotelAmenity(
                        ha.getId(),
                        ha.getName(),
                        ha.getScope()))
                .toList();
    }

    private List<RoomTypeResponse> buildRoomTypeResponses(
            List<RoomTypeEntity> roomTypes,
            Map<UUID, Integer> bookedCountByRoomType) {
        return roomTypes.stream()
                .map(rt -> toRoomTypeResponse(rt, bookedCountByRoomType))
                .toList();
    }

    private RoomTypeResponse toRoomTypeResponse(
            RoomTypeEntity rt,
            Map<UUID, Integer> bookedCountByRoomType) {
        String coverImageUrl = rt.getRoomTypeImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsCover()))
                .findFirst()
                .or(() -> rt.getRoomTypeImages().stream().findFirst())
                .map(img -> img.getUrl())
                .orElse(null);

        // availableRooms = null nếu không có checkin/checkout (bookedCountByRoomType rỗng)
        Integer availableRooms = null;
        if (!bookedCountByRoomType.isEmpty()) {
            int booked = bookedCountByRoomType.getOrDefault(rt.getId(), 0);
            availableRooms = Math.max(0, rt.getQuantity() - booked);
        }

        return new RoomTypeResponse(
                rt.getId(),
                rt.getName(),
                rt.getBasePricePerNight(),
                rt.getMaxGuests(),
                rt.getBedCounts(),
                rt.getArea(),
                coverImageUrl,
                rt.getQuantity(),
                availableRooms);
    }
}
