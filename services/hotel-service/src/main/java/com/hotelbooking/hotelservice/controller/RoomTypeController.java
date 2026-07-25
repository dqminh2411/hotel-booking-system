package com.hotelbooking.hotelservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelbooking.hotelservice.dto.response.ApiResponse;
import com.hotelbooking.hotelservice.dto.response.RoomTypeDetailResponse;
import com.hotelbooking.hotelservice.service.RoomTypeService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTypeController {

    RoomTypeService roomTypeService;
    
    @GetMapping("/{id}")
    public ApiResponse<RoomTypeDetailResponse> getRoomTypeDetail(
            @PathVariable("id") UUID id){
        
                
        return ApiResponse.<RoomTypeDetailResponse>builder()
                            .code(200)
                            .message("Thông tin chi tiết loại phòng: " + id.toString())
                            .data(roomTypeService.getRoomTypeDetail(id))
                            .build();
    }

    //cái này thì chắc 0 cần page vì thường 0 nhiều phòng đến vậy để phân trang
    @GetMapping("/rooms")
    public ApiResponse<?> getListRoomAvailable(@RequestParam("roomTypeIds") List<UUID> roomTypeIds){

        return ApiResponse.builder()
                        .code(200)
                        .message("Lấy thành công danh sách phòng sẵn sàng để checkin của booking")
                        .data(roomTypeService.getListRoomAvailable(roomTypeIds))
                        .build();
    }
}
