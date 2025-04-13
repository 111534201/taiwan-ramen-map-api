package com.example.taiwanramenmapapi.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JwtAuthenticationResponse {
    private String token;
    // 可以添加 token 過期時間、用戶基本信息等
    // private String tokenType = "Bearer";
    // private UserDTO user;
}