package com.example.taiwanramenmapapi.service.impl;

import com.example.taiwanramenmapapi.dto.request.LoginRequest;
import com.example.taiwanramenmapapi.dto.request.ShopOwnerSignUpRequest;
import com.example.taiwanramenmapapi.dto.request.UserSignUpRequest;
import com.example.taiwanramenmapapi.dto.response.JwtAuthenticationResponse;
import com.example.taiwanramenmapapi.dto.response.UserDTO;
import com.example.taiwanramenmapapi.entity.User;
import com.example.taiwanramenmapapi.entity.enums.Role;
import com.example.taiwanramenmapapi.exception.AccountExistsException;
import com.example.taiwanramenmapapi.mapper.UserMapper; // 確保引入
import com.example.taiwanramenmapapi.repository.UserRepository;
import com.example.taiwanramenmapapi.service.AuthService;
import com.example.taiwanramenmapapi.service.JwtService;
import com.example.taiwanramenmapapi.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException; // 引入用於捕獲登入失敗
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private ShopService shopService;
    @Autowired
    private UserMapper userMapper; // 注入 UserMapper


    @Override
    @Transactional
    public UserDTO signUpUser(UserSignUpRequest userSignUpRequest) {
        logger.info("嘗試註冊食客用戶: {}", userSignUpRequest.getUsername());
        if (userRepository.existsByUsername(userSignUpRequest.getUsername())) {
            throw new AccountExistsException("用戶名已被註冊: " + userSignUpRequest.getUsername());
        }
        if (userRepository.existsByEmail(userSignUpRequest.getEmail())) {
            throw new AccountExistsException("Email已被註冊: " + userSignUpRequest.getEmail());
        }

        User user = User.builder()
                .username(userSignUpRequest.getUsername())
                .email(userSignUpRequest.getEmail())
                .password(passwordEncoder.encode(userSignUpRequest.getPassword()))
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        logger.info("食客用戶 '{}' 註冊成功 (ID: {})", savedUser.getUsername(), savedUser.getId());
        return userMapper.toUserDTO(savedUser); // 使用 Mapper 轉換
    }

    @Override
    @Transactional // 確保用戶和店家創建在同一個事務中
    public UserDTO signUpShopOwner(ShopOwnerSignUpRequest shopOwnerSignUpRequest) {
        logger.info("嘗試註冊店家用戶: {}, 店家名稱: {}", shopOwnerSignUpRequest.getUsername(), shopOwnerSignUpRequest.getShopName());
        if (userRepository.existsByUsername(shopOwnerSignUpRequest.getUsername())) {
            throw new AccountExistsException("用戶名已被註冊: " + shopOwnerSignUpRequest.getUsername());
        }
        if (userRepository.existsByEmail(shopOwnerSignUpRequest.getEmail())) {
            throw new AccountExistsException("Email已被註冊: " + shopOwnerSignUpRequest.getEmail());
        }

        // 創建 User
        User user = User.builder()
                .username(shopOwnerSignUpRequest.getUsername())
                .email(shopOwnerSignUpRequest.getEmail())
                .password(passwordEncoder.encode(shopOwnerSignUpRequest.getPassword()))
                .role(Role.ROLE_SHOP_OWNER)
                .enabled(true) // 根據需求，可以設為 false 等待管理員審核啟用
                .build();

        // 先保存 User
        User savedUser = userRepository.save(user);
        logger.info("店家用戶 '{}' 基礎信息保存成功 (ID: {})", savedUser.getUsername(), savedUser.getId());

        // 調用 ShopService 創建店家並關聯 owner
        try {
            // 這裡 shopService.createShopWithOwner 會處理 Geocoding 和文件上傳
            shopService.createShopWithOwner(shopOwnerSignUpRequest, savedUser);
            logger.info("店家 '{}' 已成功創建並關聯到用戶 '{}'", shopOwnerSignUpRequest.getShopName(), savedUser.getUsername());
        } catch (Exception e) {
            logger.error("為用戶 '{}' 創建店家 '{}' 失敗: {}", savedUser.getUsername(), shopOwnerSignUpRequest.getShopName(), e.getMessage(), e);
            // 拋出運行時異常以觸發事務回滾
            throw new RuntimeException("創建店家資料時發生錯誤，註冊流程終止。", e);
        }

        return userMapper.toUserDTO(savedUser); // 返回用戶信息
    }

    @Override
    public JwtAuthenticationResponse login(LoginRequest loginRequest) {
        logger.info("嘗試登入用戶: {}", loginRequest.getUsername());
        try {
            // 使用 AuthenticationManager 驗證用戶名和密碼
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            // 驗證成功後，SecurityContext 會被更新 (雖然我們這裡是無狀態)

            // 從驗證結果獲取 UserDetails (我們的 User 實例)
            User user = (User) authentication.getPrincipal();

            // 檢查帳號是否被禁用
            if (!user.isEnabled()) {
                logger.warn("登入失敗：用戶帳號 '{}' 已被禁用。", loginRequest.getUsername());
                throw new BadCredentialsException("用戶帳號已被禁用"); // 或者使用更具體的異常
            }


            // 生成 JWT Token
            String jwt = jwtService.generateToken(user);
            logger.info("為用戶 '{}' 生成 JWT Token 成功", user.getUsername());

            return JwtAuthenticationResponse.builder().token(jwt).build();

        } catch (BadCredentialsException e) {
            logger.warn("登入失敗：用戶名或密碼錯誤 for user '{}'", loginRequest.getUsername());
            // 可以考慮不打印詳細錯誤到日誌，防止信息洩露
            throw new BadCredentialsException("用戶名或密碼錯誤"); // 重新拋出標準異常
        } catch (Exception e) {
            logger.error("登入過程中發生未知錯誤 for user '{}': {}", loginRequest.getUsername(), e.getMessage(), e);
            throw new RuntimeException("登入過程中發生錯誤", e);
        }
    }
}