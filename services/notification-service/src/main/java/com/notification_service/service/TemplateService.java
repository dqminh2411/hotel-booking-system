package com.notification_service.service;

import com.notification_service.dto.EmailHotelStatusRequest;
import com.notification_service.dto.EmailHotelStatusTemplate;
import com.notification_service.dto.EmailRequest;
import com.notification_service.dto.EmailTemplate;
import org.springframework.stereotype.Service;


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

    public String buildHotelEmailContent(EmailHotelStatusTemplate template, EmailHotelStatusRequest data){
        
        return switch (template){
            case HOTEL_APPROVED -> """
                    Xin chào %s,

                    Yêu cầu đăng tin về khách sạn của bạn đã được phê duyệt!
                    
                    - Khách sạn: %s
                    - Địa chỉ: %s

                    - Chủ khách sạn: %s
                    - Email: %s

                    Từ giờ bạn có thể nhận các booking từ mọi khách hàng có yêu cầu trên hệ thống!
                    Chúc bạn có trải nghiệm sử dụng hệ thống thật hài lòng! 
                    """.formatted(
                        data.tenant().name(),
                        data.hotel().name(),
                        data.hotel().address(),
                        data.tenant().name(),
                        data.tenant().email()
            );

            case HOTEL_SUSPENDED -> """
                    Xin chào %s,

                    Yêu cầu đăng tin về khách sạn của bạn đã bị từ chối!
                    
                    - Khách sạn: %s
                    - Địa chỉ: %s

                    - Chủ khách sạn: %s
                    - Email: %s

                    =========Lý do=========
                    %s
                    Bạn hãy sửa/cập nhật lại thông tin đã sai rồi gửi lại yêu cầu nhé.
                    
                    Chúc bạn có trải nghiệm sử dụng hệ thống thật hài lòng! 
                    """.formatted(
                        data.tenant().name(),
                        data.hotel().name(),
                        data.hotel().address(),
                        data.tenant().name(),
                        data.tenant().email(),
                        data.reason()
            );
        };
    }
}
