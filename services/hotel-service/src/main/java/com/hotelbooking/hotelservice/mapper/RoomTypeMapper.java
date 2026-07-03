package com.hotelbooking.hotelservice.mapper;

import java.util.Comparator;
import java.util.List;

import com.hotelbooking.hotelservice.dto.other.HotelAmenity;
import com.hotelbooking.hotelservice.dto.other.Image;
import com.hotelbooking.hotelservice.dto.response.RoomTypeDetailResponse;
import com.hotelbooking.hotelservice.entity.AmenityEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeImageEntity;

public class RoomTypeMapper {

    public RoomTypeDetailResponse toRoomTypeResponse(
        RoomTypeEntity roomType,
        List<RoomTypeImageEntity> images,
        List<AmenityEntity> amenities
    ){
        return new RoomTypeDetailResponse(
            roomType.getId(),
            roomType.getHotel().getId(),
            roomType.getName(),
            roomType.getDescription(),
            roomType.getBasePricePerNight(),
            roomType.getMaxGuests(),
            roomType.getBedCounts(),
            roomType.getArea(),
            roomType.getQuantity(),
            buildAmenities(amenities),
            buildImages(images));
    }


    private List<Image> buildImages(List<RoomTypeImageEntity> images){
        return images.stream()
            .sorted(Comparator.comparing(img -> !img.getIsCover()))
            .limit(5)
            .map(img -> new Image(img.getId(), img.getUrl(), img.getIsCover()))
            .toList();
    }

    private List<HotelAmenity> buildAmenities(List<AmenityEntity> amenities){
        return amenities.stream()
                .map(a -> new HotelAmenity(a.getId(), a.getName(), a.getScope()))
                .toList();
    }
}
