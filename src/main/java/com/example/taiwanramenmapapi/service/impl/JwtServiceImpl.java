package com.example.taiwanramenmapapi.service.impl;

import com.example.taiwanramenmapapi.entity.User; // 引入 User 實體
import com.example.taiwanramenmapapi.entity.enums.Role; // 引入 Role 枚舉
import com.example.taiwanramenmapapi.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority; // 引入 GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.slf4j.Logger; // 引入 Logger
import org.slf4j.LoggerFactory; // 引入 LoggerFactory


import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors; // 引入 Collectors

@Service
public class JwtServiceImpl implements JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

    @Value("${app.jwt.secret}")
    private String jwtSigningKey; // 從配置讀取簽名密鑰

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs; // 從配置讀取過期時間

    /**
     * 從 Token 中提取用戶名 (Subject)。
     */
    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 根據 UserDetails 生成 JWT Token，自動添加額外的 Claims。
     */
    @Override
    public String generateToken(UserDetails userDetails) {
        // 創建一個 Map 來存放額外的 Claims
        Map<String, Object> extraClaims = new HashMap<>();

        // 檢查傳入的 userDetails 是否是我們自定義的 User 實例
        if (userDetails instanceof User user) {
            logger.debug("為用戶 '{}' 生成 Token，添加額外 Claims...", user.getUsername());
            // 添加 userId
            extraClaims.put("userId", user.getId());

            // 添加角色列表 (轉換為字符串列表)
            extraClaims.put("roles", user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority) // 獲取角色名稱字符串 (例如 "ROLE_USER")
                    .collect(Collectors.toList())); // 收集為列表

            // 如果是店家角色，並且有關聯的店家，添加店家 ID
            if (user.getRole() == Role.ROLE_SHOP_OWNER && user.getOwnedShops() != null && !user.getOwnedShops().isEmpty()) {
                // 假設一個店家用戶只管理一家店，取第一家店的 ID
                // 如果你的邏輯允許一個用戶擁有多家店，這裡可能需要傳遞 ID 列表
                Long ownedShopId = user.getOwnedShops().get(0).getId();
                if (ownedShopId != null) {
                    extraClaims.put("ownedShopId", ownedShopId);
                    logger.debug("-> 添加 ownedShopId: {}", ownedShopId);
                }
            }
            // 可選：添加 Email 等其他信息
            // extraClaims.put("email", user.getEmail());

        } else {
            logger.warn("傳入 generateToken 的 UserDetails 不是 User 實例，無法添加額外 Claims。只包含用戶名。");
        }

        // 調用包含 extraClaims 的 generateToken 方法
        return generateToken(extraClaims, userDetails);
    }

    /**
     * 根據 UserDetails 和額外的 Claims 生成 JWT Token。
     */
    @Override
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // --- 如果 extraClaims 中缺少基礎信息，嘗試再次添加 (作為備用) ---
        if (userDetails instanceof User user) {
            extraClaims.putIfAbsent("userId", user.getId()); // 如果 userId 不存在則添加
            extraClaims.putIfAbsent("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()); // 如果 roles 不存在則添加
            if (user.getRole() == Role.ROLE_SHOP_OWNER && user.getOwnedShops() != null && !user.getOwnedShops().isEmpty() && !extraClaims.containsKey("ownedShopId")) {
                Long ownedShopId = user.getOwnedShops().get(0).getId();
                if (ownedShopId != null) { extraClaims.put("ownedShopId", ownedShopId); }
            }
        }
        // --- ---

        logger.debug("使用 Claims: {} 為用戶 '{}' 生成 Token", extraClaims.keySet(), userDetails.getUsername());

        return Jwts.builder()
                .claims(extraClaims) // 設置所有 Claims
                .subject(userDetails.getUsername()) // 設置主題為用戶名
                .issuedAt(new Date(System.currentTimeMillis())) // 設置簽發時間
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // 設置過期時間
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 使用 HS256 算法和密鑰簽名
                .compact(); // 構建 Token 字符串
    }

    /**
     * 驗證 Token 對指定用戶是否有效。
     */
    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (token == null || userDetails == null) {
            return false;
        }
        try {
            final String username = extractUsername(token);
            // 檢查用戶名是否匹配且 Token 是否未過期
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            // 解析或驗證過程中出錯，視為無效
            logger.warn("驗證 Token 時出錯 (可能已過期、簽名無效等): {}", e.getMessage());
            return false;
        }
    }

    // --- 私有輔助方法 ---

    /**
     * 從 Token 中提取指定的 Claim。
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    /**
     * 檢查 Token 是否已過期。
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * 從 Token 中提取過期時間。
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 解析 Token 中的所有 Claims。如果解析失敗 (例如簽名無效、格式錯誤)，會拋出異常。
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // 使用密鑰驗證簽名
                .build()
                .parseSignedClaims(token) // 解析 JWS (帶簽名的 JWT)
                .getPayload(); // 獲取 Payload (Claims)
    }

    /**
     * 從配置的 Base64 密鑰字符串生成簽名所需的 SecretKey。
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey); // 解碼 Base64
        return Keys.hmacShaKeyFor(keyBytes); // 生成 HMAC-SHA 密鑰
    }
}