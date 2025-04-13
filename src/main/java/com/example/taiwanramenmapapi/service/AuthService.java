package com.example.taiwanramenmapapi.service;

import com.example.taiwanramenmapapi.dto.request.LoginRequest;
import com.example.taiwanramenmapapi.dto.request.ShopOwnerSignUpRequest;
import com.example.taiwanramenmapapi.dto.request.UserSignUpRequest;
import com.example.taiwanramenmapapi.dto.response.JwtAuthenticationResponse;
import com.example.taiwanramenmapapi.dto.response.UserDTO; // 引入 UserDTO

public interface AuthService {

    /**
     * 註冊普通食客用戶
     * @param userSignUpRequest 包含用戶名、密碼、Email 的請求體
     * @return 創建成功的用戶信息 (不含密碼)
     */
    UserDTO signUpUser(UserSignUpRequest userSignUpRequest);

    /**
     * 註冊店家用戶，並同時創建店家資料
     * @param shopOwnerSignUpRequest 包含用戶信息和店家信息的請求體
     * @return 創建成功的用戶信息 (不含密碼)
     */
    UserDTO signUpShopOwner(ShopOwnerSignUpRequest shopOwnerSignUpRequest);

    /**
     * 用戶登入
     * @param loginRequest 包含用戶名和密碼的請求體
     * @return 包含 JWT Token 的響應體
     */
    JwtAuthenticationResponse login(LoginRequest loginRequest);

}