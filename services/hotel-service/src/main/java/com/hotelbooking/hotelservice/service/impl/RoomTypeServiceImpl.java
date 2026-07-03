package com.hotelbooking.hotelservice.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.hotelbooking.hotelservice.dto.response.RoomTypeDetailResponse;
import com.hotelbooking.hotelservice.entity.AmenityEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeEntity;
import com.hotelbooking.hotelservice.entity.RoomTypeImageEntity;
import com.hotelbooking.hotelservice.exception.RoomTypeNotFoundException;
import com.hotelbooking.hotelservice.mapper.RoomTypeMapper;
import com.hotelbooking.hotelservice.repository.RoomTypeAmenityRepository;
import com.hotelbooking.hotelservice.repository.RoomTypeImageRepository;
import com.hotelbooking.hotelservice.repository.RoomTypeRepository;
import com.hotelbooking.hotelservice.service.RoomTypeService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTypeServiceImpl implements RoomTypeService{
    RoomTypeRepository roomTypeRepository;
    RoomTypeAmenityRepository roomTypeAmenityRepository;
    RoomTypeImageRepository roomTypeImageRepository;
    RoomTypeMapper roomTypeMapper;

    @Override
    public RoomTypeDetailResponse getRoomTypeDetail(UUID roomTypeId){
        
        RoomTypeEntity roomType = roomTypeRepository.findByIdAndIsDeletedFalse(roomTypeId)
                            .orElseThrow(() -> new RoomTypeNotFoundException(roomTypeId.toString()));
        
        List<RoomTypeImageEntity> images = roomTypeImageRepository.findByRoomType_IdOrderByIsCoverDescCreatedAtAsc(roomTypeId);
        List<AmenityEntity> amenities = roomTypeAmenityRepository.findActiveAmenitiesByRoomTypeId(roomTypeId);

        return roomTypeMapper.toRoomTypeDetailResponse(roomType, images, amenities);
    }
}
