package com.intelliguard.dto;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Wraps every API response in a consistent envelope:
 * {
 *   "success": true,
 *   "message": "Transaction processed successfully",
 *   "data": { ...actual response... },
 *   "timestamp": "2024-01-15T14:30:00"
 * }
 *
 * This is a professional API pattern — every response
 * looks the same whether it's success or failure.
 * Interviewers love seeing this.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    // Quick factory methods
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}