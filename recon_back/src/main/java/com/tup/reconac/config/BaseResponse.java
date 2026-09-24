package com.tup.reconac.config;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
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
                .timestamp(Instant.now().toString())
                .build();
    }

    public static <T> BaseResponse<T> error(String message, List<String> errors) {
        return BaseResponse.<T>builder()
                .message(message)
                .errors(errors)
                .timestamp(Instant.now().toString())
                .build();
    }
}
