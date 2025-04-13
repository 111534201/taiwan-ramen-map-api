package com.example.taiwanramenmapapi.controller; // 或 exception/advice 包

import com.example.taiwanramenmapapi.dto.response.ApiResponse;
import com.example.taiwanramenmapapi.exception.*; // 引入所有自定義異常
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors; // 引入 Collectors

@RestControllerAdvice // 標記為全局異常處理
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- 處理 ResourceNotFoundException ---
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.warn("資源未找到: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.failure(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND); // 404
    }

    // --- 處理 UnauthorizedActionException ---
    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedActionException(UnauthorizedActionException ex, WebRequest request) {
        logger.warn("未授權的操作: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.failure(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN); // 403
    }

    // --- 處理 BadRequestException ---
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(BadRequestException ex, WebRequest request) {
        logger.warn("錯誤的請求: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.failure(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400
    }

    // --- 處理 AccountExistsException ---
    @ExceptionHandler(AccountExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountExistsException(AccountExistsException ex, WebRequest request) {
        logger.warn("帳號已存在衝突: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.failure(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409
    }

    // --- 處理 GeocodingException ---
    @ExceptionHandler(GeocodingException.class)
    public ResponseEntity<ApiResponse<Object>> handleGeocodingException(GeocodingException ex, WebRequest request) {
        logger.error("地理編碼失敗: {}", ex.getMessage(), ex.getCause());
        ApiResponse<Object> response = ApiResponse.failure("地址處理失敗: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400
    }

    // --- 處理 FileStorageException ---
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiResponse<Object>> handleFileStorageException(FileStorageException ex, WebRequest request) {
        logger.error("文件儲存錯誤: {}", ex.getMessage(), ex.getCause());
        ApiResponse<Object> response = ApiResponse.failure("文件處理失敗: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR); // 500
    }

    // --- 處理 Spring Security 異常 ---
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        logger.warn("登入失敗: 用戶名或密碼錯誤。");
        ApiResponse<Object> response = ApiResponse.failure("用戶名或密碼錯誤");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED); // 401
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.warn("權限不足: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.failure("權限不足，無法訪問此資源");
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN); // 403
    }

    // --- 處理 Bean Validation 異常 (@Valid) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        logger.warn("請求體驗證失敗: {}", ex.getMessage());
        // 收集所有字段的錯誤信息
        String errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    String fieldName = (error instanceof FieldError) ? ((FieldError) error).getField() : error.getObjectName();
                    String errorMessage = error.getDefaultMessage();
                    return fieldName + ": " + errorMessage;
                })
                .collect(Collectors.joining(", "));

        ApiResponse<Object> response = ApiResponse.failure("輸入驗證失敗: " + errors);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400
    }

    // --- 處理文件上傳大小超限異常 ---
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        logger.warn("文件上傳失敗：文件大小超過限制。 Max: {}", exc.getMaxUploadSize());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE) // 413
                .body(ApiResponse.failure("文件大小超過限制")); // 可以返回更詳細的大小限制信息
    }

    // --- 處理所有其他未捕獲的異常 (兜底) ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("發生未處理的內部伺服器錯誤: {}", ex.getMessage(), ex); // 記錄完整堆疊
        ApiResponse<Object> response = ApiResponse.failure("伺服器發生內部錯誤，請稍後再試或聯繫管理員。");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR); // 500
    }
}