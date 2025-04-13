package com.example.taiwanramenmapapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 資源衝突，返回 409
public class AccountExistsException extends RuntimeException {
    public AccountExistsException(String message) {
        super(message);
    }
}