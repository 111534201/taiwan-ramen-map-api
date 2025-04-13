package com.example.taiwanramenmapapi.controller;

import com.example.taiwanramenmapapi.dto.request.LoginRequest;
import com.example.taiwanramenmapapi.dto.request.ShopOwnerSignUpRequest;
import com.example.taiwanramenmapapi.dto.request.UserSignUpRequest;
import com.example.taiwanramenmapapi.dto.response.ApiResponse;
import com.example.taiwanramenmapapi.dto.response.JwtAuthenticationResponse;
import com.example.taiwanramenmapapi.dto.response.UserDTO;
import com.example.taiwanramenmapapi.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/auth") // 基礎路徑 /api/auth
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * POST /api/auth/signup/user : 註冊食客帳號
     */
    @PostMapping("/signup/user")
    public ResponseEntity<ApiResponse<UserDTO>> signUpUser(@Valid @RequestBody UserSignUpRequest userSignUpRequest) {
        logger.info("收到食客註冊請求: {}", userSignUpRequest.getUsername());
        UserDTO createdUser = authService.signUpUser(userSignUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUser, "食客帳號註冊成功"));
    }

    /**
     * POST /api/auth/signup/shop : 註冊店家帳號 (含店家資訊和照片)
     */
    @PostMapping(value = "/signup/shop", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<UserDTO>> signUpShopOwner(
            @Valid @RequestPart("shopData") ShopOwnerSignUpRequest shopOwnerSignUpRequest,
            @RequestPart(value = "initialPhotos", required = false) List<MultipartFile> initialPhotos) {

        logger.info("收到店家註冊請求: {}", shopOwnerSignUpRequest.getUsername());
        // 將文件列表設置到 DTO 中 (假設 DTO 有此字段和 setter)
        shopOwnerSignUpRequest.setInitialPhotos(initialPhotos);

        UserDTO createdUser = authService.signUpShopOwner(shopOwnerSignUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUser, "店家帳號註冊成功，店家已創建"));
    }

    /**
     * POST /api/auth/login : 用戶登入
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("收到登入請求: {}", loginRequest.getUsername());
        JwtAuthenticationResponse jwtResponse = authService.login(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(jwtResponse, "登入成功"));
    }
}