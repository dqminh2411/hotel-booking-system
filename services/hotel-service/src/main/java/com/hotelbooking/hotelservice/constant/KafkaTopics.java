package com.hotelbooking.hotelservice.constant;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    /**
     * Topic chứa lệnh yêu cầu admin duyệt hotel mới tạo.
     * Consumer: admin-service (hoặc notification-service) sẽ lắng nghe topic này.
     */
    public static final String REVIEW_HOTEL_COMMAND = "review-hotel-command";
}
