package com.tup.reconac.config;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Builder
public class BaseResponse<T> {

    private final T data;
    private final String message;
    private final List<String> errors;
    private final String timestamp;

    public static <T> BaseResponse<T> ok(T data, String message) {
        return BaseResponse.<T>builder()
                .data(data)
                .message(message)
                .errors(null)
                .timestamp(getCurrentTimestamp())
                .build();
    }

    // ver utilidad de esto
    public static <T> BaseResponse<T> error(String message, List<String> errors) {
        return BaseResponse.<T>builder()
                .data(null)
                .message(message)
                .errors(errors)
                .timestamp(getCurrentTimestamp())
                .build();
    }

    private static String getCurrentTimestamp() {
        return DateTimeFormatter.ISO_INSTANT
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
    }
}
