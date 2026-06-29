package com.place_booking_service.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import feign.Response;

public class HotelServiceErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey,
                            Response response) {

        try {
            ErrorResponse error =
                objectMapper.readValue(
                    response.body().asInputStream(),
                    ErrorResponse.class);

            if ("HOTEL_NOT_FOUND".equals(error.code())) {
                return new HotelNotFoundException(error.message());
            }

            if ("USER_NOT_FOUND".equals(error.code())) {
                return new UserNotFoundException(error.message());
            }

            if (error.message() != null && !error.message().isBlank()) {
                return new ExternalServiceException(methodKey, error.message());
            }

        } catch (Exception ignored) {
        }

        Exception decoded = defaultDecoder.decode(methodKey, response);
        return new ExternalServiceException(methodKey, decoded.getMessage());
    }
}
