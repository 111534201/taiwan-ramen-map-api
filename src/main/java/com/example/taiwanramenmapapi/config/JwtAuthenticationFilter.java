package com.example.taiwanramenmapapi.config; // 或者 com.example.taiwanramenmapapi.filter;

import com.example.taiwanramenmapapi.service.JwtService;
import com.example.taiwanramenmapapi.service.UserService; // 引入 UserService
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull; // Lombok @NonNull
import lombok.RequiredArgsConstructor; // Lombok 自動生成構造函數
import org.apache.commons.lang3.StringUtils; // 使用 Apache Commons Lang 判斷空字符串
import org.slf4j.Logger; // 引入 Logger
import org.slf4j.LoggerFactory; // 引入 LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter; // 確保每個請求只過濾一次

import java.io.IOException;

@Component // 標記為 Spring 組件
@RequiredArgsConstructor // Lombok: 自動為 final 或 @NonNull 字段生成構造函數 (方便注入)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger filterLogger = LoggerFactory.getLogger(JwtAuthenticationFilter.class); // 單獨的 Logger

    private final JwtService jwtService; // 注入 JwtService
    private final UserService userService; // 注入 UserService (用於加載 UserDetails)

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        filterLogger.trace("JWT Filter: Processing request for {}", request.getRequestURI()); // TRACE 級別日誌

        final String authHeader = request.getHeader("Authorization"); // 從請求頭獲取 Authorization
        final String jwt;
        final String username;

        // 1. 檢查 Authorization 標頭是否存在且格式正確 (Bearer Token)
        if (StringUtils.isBlank(authHeader) || !StringUtils.startsWith(authHeader, "Bearer ")) {
            filterLogger.trace("JWT Filter: No JWT token found in Authorization header for {}", request.getRequestURI());
            filterChain.doFilter(request, response); // 沒有 Token 或格式不對，直接放行給下一個過濾器
            return;
        }

        // 2. 提取 JWT Token (去掉 "Bearer " 前綴)
        jwt = authHeader.substring(7);
        filterLogger.trace("JWT Filter: Extracted JWT token: {}", jwt); // 注意：生產環境不應記錄完整 Token

        try {
            // 3. 從 Token 中提取用戶名
            username = jwtService.extractUsername(jwt);
            filterLogger.trace("JWT Filter: Extracted username from token: {}", username);

            // 4. 檢查用戶名是否存在，且當前 SecurityContext 中是否還沒有認證信息
            if (StringUtils.isNotEmpty(username) && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 5. 根據用戶名從 UserService 加載 UserDetails
                UserDetails userDetails = this.userService.loadUserByUsername(username); // 注意 UserDetailsService 的實現

                // 6. 驗證 Token 是否有效 (用戶名匹配且未過期)
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    filterLogger.debug("JWT Filter: Token is valid for user {}", username); // DEBUG 級別記錄驗證成功
                    // 7. 創建 AuthenticationToken
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, // Principal (可以是 UserDetails 或 User 實體)
                            null, // Credentials (JWT 模式下不需要密碼)
                            userDetails.getAuthorities() // 用戶權限
                    );
                    // 8. 設置請求詳情
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 9. 更新 SecurityContextHolder 中的認證信息
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authToken);
                    SecurityContextHolder.setContext(context);
                    filterLogger.debug("JWT Filter: Security context updated for user {}", username);
                } else {
                    filterLogger.warn("JWT Filter: Invalid token received for user {}", username);
                }
            } else {
                filterLogger.trace("JWT Filter: Username empty or context already has authentication for {}", request.getRequestURI());
            }
        } catch (Exception e) {
            // 處理 Token 解析或驗證過程中可能出現的異常
            filterLogger.warn("JWT Filter: Error processing JWT token for {}: {}", request.getRequestURI(), e.getMessage());
            // 可以選擇清除 SecurityContext
            // SecurityContextHolder.clearContext();
            // 不建議在這裡直接返回錯誤響應，讓後續的異常處理機制 (如 ExceptionHandling) 來處理
        }


        // 10. 繼續執行過濾鏈中的下一個過濾器
        filterChain.doFilter(request, response);
    }
}