package com.example.taiwanramenmapapi.config;

import com.example.taiwanramenmapapi.entity.enums.Role; // 引入 Role
import com.example.taiwanramenmapapi.service.JwtService; // 引入 JwtService
import com.example.taiwanramenmapapi.service.UserService; // 引入 UserService (用於 UserDetailsService)
import jakarta.servlet.http.HttpServletResponse; // 引入 HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // 引入 HttpMethod
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // 啟用方法級別安全
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // 引入 AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy; // 引入 SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService; // 引入 UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // 引入 CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource; // 引入 CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // 引入 UrlBasedCorsConfigurationSource

import java.util.Arrays; // 引入 Arrays
import java.util.List; // 引入 List

@Configuration // 標記為配置類
@EnableWebSecurity // 啟用 Spring Security 的 Web 安全支持
@EnableMethodSecurity // 啟用方法級別的權限控制 (例如 @PreAuthorize)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // 注入我們稍後創建的 JWT 過濾器

    @Autowired
    private UserService userService; // 注入 UserService (實現 UserDetailsService)

    // 配置密碼編碼器 Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // 使用 BCrypt 算法加密密碼
    }

    // 配置 UserDetailsService Bean
    @Bean
    public UserDetailsService userDetailsService() {
        // 我們的 UserService 將負責根據用戶名加載用戶信息
        return userService::loadUserByUsername; // 使用方法引用
    }

    // 配置 AuthenticationProvider Bean (數據訪問對象認證提供者)
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService()); // 設置 UserDetailsService
        authProvider.setPasswordEncoder(passwordEncoder()); // 設置密碼編碼器
        return authProvider;
    }

    // 配置 AuthenticationManager Bean (身份驗證管理器)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 配置 CORS (跨域資源共享)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允許來自前端開發伺服器和部署後可能的來源
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173", "你的前端部署網域")); // !!! 修改為你的前端地址 !!!
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // 允許的 HTTP 方法
        configuration.setAllowedHeaders(Arrays.asList("*")); // 允許所有請求頭
        configuration.setAllowCredentials(true); // 允許發送 Cookie
        configuration.setMaxAge(3600L); // 預檢請求 (OPTIONS) 的緩存時間

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration); // 只對 /api/** 路徑應用此 CORS 配置
        return source;
    }


    // 配置安全過濾鏈 (核心配置)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 配置 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. 禁用 CSRF (因為我們使用 JWT，不需要 CSRF 保護)
                .csrf(AbstractHttpConfigurer::disable)

                // 3. 配置請求授權規則
                .authorizeHttpRequests(authorize -> authorize
                        // --- 公開訪問的端點 ---
                        .requestMatchers("/api/auth/**").permitAll() // 登入和註冊接口允許匿名訪問
                        .requestMatchers(HttpMethod.GET, "/api/shops", "/api/shops/{id}", "/api/shops/top").permitAll() // 公開的店家查詢接口
                        .requestMatchers(HttpMethod.GET, "/api/reviews/shop/{shopId}", "/api/reviews/{reviewId}/replies").permitAll() // 公開的評論查詢接口
                        .requestMatchers("/uploads/**").permitAll() // 允許公開訪問上傳的文件
                        .requestMatchers("/error").permitAll() // 允許訪問錯誤頁面
                        // --- 其他公開資源 (例如靜態文件，如果有的話) ---
                        // .requestMatchers("/public/**").permitAll()

                        // --- 需要特定角色的端點 (更細粒度的可以在 Controller 使用 @PreAuthorize) ---
                        // 評論相關 (至少需要是 USER)
                        .requestMatchers(HttpMethod.POST, "/api/reviews/shop/{shopId}").hasAnyRole("USER", "SHOP_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/{reviewId}").hasAnyRole("USER", "SHOP_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/{reviewId}").hasAnyRole("USER", "SHOP_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reviews/{reviewId}/media").hasAnyRole("USER", "SHOP_OWNER", "ADMIN") // 假設有此端點
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/{reviewId}/media/{mediaId}").hasAnyRole("USER", "SHOP_OWNER", "ADMIN") // 假設有此端點

                        // 店家管理 (需要 SHOP_OWNER 或 ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/shops/{id}").hasAnyRole("SHOP_OWNER", "ADMIN")
                        // 刪除店家和上傳/刪除媒體現在由 Service 層做更細的權限判斷，但基礎要求是 SHOP_OWNER 或 ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/shops/{shopId}/media").hasAnyRole("SHOP_OWNER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/{shopId}/media/{mediaId}").hasAnyRole("SHOP_OWNER", "ADMIN")
                        // 創建店家也移到 Controller 的 @PreAuthorize，這裡可以不用寫，或者寫 hasAnyRole('ADMIN', 'SHOP_OWNER')

                        // 管理員專屬端點 (示例)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // 假設有 /api/admin 路徑
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/{id}").hasRole("ADMIN") // 明確只有 Admin 能刪除店家

                        // --- 其他所有請求都需要身份驗證 ---
                        .anyRequest().authenticated()
                )

                // 4. 配置 Session 管理策略為無狀態 (STATELESS)，因為我們用 JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. 配置自定義的 AuthenticationProvider
                .authenticationProvider(authenticationProvider())

                // 6. 添加 JWT 認證過濾器，在 UsernamePasswordAuthenticationFilter 之前執行
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 7. 配置異常處理 (可選，用於自定義未授權/未認證的響應)
                .exceptionHandling(exceptions -> exceptions
                        // 未認證（未登入）時的處理
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未授權：請先登入")
                        )
                        // 已認證但權限不足時的處理
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "權限不足")
                        )
                );


        return http.build(); // 構建並返回 SecurityFilterChain
    }
}