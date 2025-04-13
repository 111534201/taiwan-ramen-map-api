package com.example.taiwanramenmapapi.service;

import org.springframework.security.core.userdetails.UserDetails; // 引入 UserDetails
import java.util.Map; // 引入 Map

public interface JwtService {

    /**
     * 從 Token 中提取用戶名
     * @param token JWT Token
     * @return 用戶名
     */
    String extractUsername(String token);

    /**
     * 根據 UserDetails 生成 JWT Token (包含默認 Claims)
     * @param userDetails 用戶詳情
     * @return 生成的 JWT Token
     */
    String generateToken(UserDetails userDetails);

    /**
     * 根據 UserDetails 生成帶有額外 Claims 的 JWT Token
     * @param extraClaims 額外的 Claims (例如角色、用戶 ID 等)
     * @param userDetails 用戶詳情
     * @return 生成的 JWT Token
     */
    String generateToken(Map<String, Object> extraClaims, UserDetails userDetails);

    /**
     * 驗證 Token 是否對指定用戶有效 (用戶名匹配且未過期)
     * @param token JWT Token
     * @param userDetails 用戶詳情
     * @return 如果有效返回 true，否則返回 false
     */
    boolean isTokenValid(String token, UserDetails userDetails);
}