package com.example.taiwanramenmapapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // 通常由錯誤的地址引起，返回 400
public class GeocodingException extends RuntimeException {
    public GeocodingException(String message) {
        super(message);
    }
    public GeocodingException(String message, Throwable cause) {
        super(message, cause);
    }
}