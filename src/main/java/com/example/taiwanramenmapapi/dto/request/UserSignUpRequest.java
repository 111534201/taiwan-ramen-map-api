package com.example.taiwanramenmapapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserSignUpRequest {
    @NotBlank(message = "用戶名不能為空")
    @Size(min = 3, max = 20, message = "用戶名長度必須在 3 到 20 個字符之間")
    private String username;

    @NotBlank(message = "密碼不能為空")
    @Size(min = 6, max = 40, message = "密碼長度必須在 6 到 40 個字符之間")
    private String password;

    @NotBlank(message = "Email不能為空")
    @Email(message = "Email格式不正確")
    @Size(max = 50, message = "Email長度不能超過 50 個字符")
    private String email;
}