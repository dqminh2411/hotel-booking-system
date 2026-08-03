package com.hotelbooking.chassis.logging.aop;

import java.lang.annotation.*;

/**
 * Đánh dấu parameter của method được annotate @Loggable sẽ được đưa vào log.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogParam {

    /** Tên field trong log output */
    String value();

    /** Mask giá trị (thay bằng ***) — dùng cho field nhạy cảm */
    boolean sensitive() default false;
}
