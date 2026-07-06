package com.notification_service.service;

import com.notification_service.dto.EmailRequest;
import com.notification_service.dto.EmailTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TemplateService {

    public String buildContent(EmailTemplate template, EmailRequest data) {

        return switch (template) {
            case BOOKING_CONFIRMED, SendBookingConfirmed -> """
                Xin chào %s,

                Đặt phòng của bạn đã được xác nhận!
                Booking ID: %s
                Khách sạn: %s
                Phòng: %s
                Checkin: %s
                Checkout: %s

                Tổng tiền: %s VND
                """.formatted(
                data.booking().customer().name(),
                data.booking().bookingId(),
                data.booking().hotel().name(),
                data.booking().roomTypeList(),
                data.booking().checkin(),
                data.booking().checkout(),
                data.booking().totalAmount()
            );

            case BOOKING_CANCELLED, SendBookingFailed -> """
                Xin chào %s,

                Đặt phòng của bạn đã bị huỷ.
                Booking ID: %s
                Khách sạn: %s
                Lý do: %s
                """.formatted(
                data.booking().customer().name(),
                data.booking().bookingId(),
                data.booking().hotel().name(),
                data.reason()
            );
        };
    }
}
