package com.hotelbooking.hotelservice.exception;

import com.hotelbooking.hotelservice.dto.response.ApiResponse;
import com.hotelbooking.hotelservice.dto.response.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HotelNotFoundException.class)
    public ApiResponse<?> handleHotelNotFound(HotelNotFoundException ex) {
        return ApiResponse.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("HOTEL_NOT_FOUND: " + ex.getMessage())
                    .build();
    }

    @ExceptionHandler(RoomTypeNotFoundException.class)
    public ApiResponse<?> handleRoomTypeNotFound(RoomTypeNotFoundException ex) {
        return ApiResponse.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("ROOM_TYPE_NOT_FOUND: " + ex.getMessage())
                    .build();
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ApiResponse<?> handleInvalidDateRange(InvalidDateRangeException ex) {
        return ApiResponse.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .message("INVALID_DATE_RANGE: " + ex.getMessage())
                    .build();
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentNotValidException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Unexpected server error"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", "Unexpected something error"));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }
}


