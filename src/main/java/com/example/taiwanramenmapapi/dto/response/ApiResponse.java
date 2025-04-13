package com.example.taiwanramenmapapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude; // 用於忽略 null 值
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 序列化時忽略 null 值的字段
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data; // 泛型數據字段
    // private String errorCode; // 可選：錯誤代碼

    // --- 靜態工廠方法，方便創建響應 ---
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }
    // 可以添加帶錯誤代碼的 failure 方法
}