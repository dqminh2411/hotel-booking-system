package com.hotelbooking.chassis.logging.util;

import com.hotelbooking.chassis.logging.LoggingProperties;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class SensitiveDataMasker {

    private final LoggingProperties properties;

    // Pattern cứng cho các loại dữ liệu nhạy cảm biết trước
    private static final List<Pattern> HARDCODED_PATTERNS = List.of(
        // Số thẻ tín dụng (13-16 chữ số, có thể có dấu cách/gạch ngang)
        Pattern.compile("\\b(?:\\d[ -]?){13,16}\\b"),
        // Bearer token
        Pattern.compile("(?i)bearer\\s+[A-Za-z0-9\\-._~+/]+=*"),
        // VNPay secure hash
        Pattern.compile("(?i)vnp_SecureHash=[A-Fa-f0-9]+")
    );

    /**
     * Mask các giá trị nhạy cảm trong một chuỗi string.
     * Dùng cho log message tự do.
     */
    public String maskString(String input) {
        if (input == null) return null;

        String result = input;

        // Mask theo pattern cứng
        for (Pattern pattern : HARDCODED_PATTERNS) {
            result = pattern.matcher(result).replaceAll(m -> maskValue(m.group()));
        }

        // Mask theo field name được cấu hình trong properties
        if (properties.getSensitiveFields() != null) {
            for (String sensitiveField : properties.getSensitiveFields()) {
                Pattern fieldPattern = Pattern.compile(
                    "(?i)(\"?" + Pattern.quote(sensitiveField) + "\"?\\s*[:=]\\s*\"?)([^\"&\\s]+)(\"?)",
                    Pattern.CASE_INSENSITIVE
                );
                result = fieldPattern.matcher(result)
                    .replaceAll(m -> m.group(1) + "***" + m.group(3));
            }
        }

        return result;
    }

    /**
     * Mask object (dùng khi logReturnValue = true trong @Loggable).
     * Convert toString() rồi mask.
     */
    public String maskObject(Object obj) {
        if (obj == null) return "null";
        return maskString(obj.toString());
    }

    private String maskValue(String value) {
        if (value == null) return "";
        if (value.length() <= 4) return "***";
        // Giữ 2 ký tự đầu và 2 ký tự cuối, mask phần giữa
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}
