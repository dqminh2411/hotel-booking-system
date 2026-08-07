package com.hotelbooking.hotelservice.util;

import com.hotelbooking.hotelservice.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public final class ImageValidationUtils {

    private ImageValidationUtils() {
    }

    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp");

    /**
     * Validate 1 file. Ném AppException(400) ngay khi phát hiện sai,
     * KHÔNG upload gì lên MinIO trước khi toàn bộ batch pass validate.
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(
                    "IMAGE_EMPTY",
                    "File ảnh rỗng hoặc không hợp lệ",
                    HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(
                    "IMAGE_TOO_LARGE",
                    "File '%s' vượt quá 5MB".formatted(file.getOriginalFilename()),
                    HttpStatus.BAD_REQUEST);
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AppException(
                    "IMAGE_TYPE_NOT_ALLOWED",
                    "File '%s' có định dạng không được hỗ trợ (chỉ nhận jpg/jpeg/png/webp)"
                            .formatted(file.getOriginalFilename()),
                    HttpStatus.BAD_REQUEST);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new AppException(
                    "IMAGE_TYPE_NOT_ALLOWED",
                    "File '%s' có đuôi file không được hỗ trợ (chỉ nhận jpg/jpeg/png/webp)"
                            .formatted(file.getOriginalFilename()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    public static String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
    }
}
