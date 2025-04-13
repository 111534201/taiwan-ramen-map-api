package com.example.taiwanramenmapapi.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile; // 引入

import java.util.List; // 引入

@Data
// 如果有共同字段，可以考慮繼承 UserSignUpRequest，但這裡為了清晰分開寫
// @EqualsAndHashCode(callSuper = true)
public class ShopOwnerSignUpRequest { // extends UserSignUpRequest { // 取消繼承，單獨定義所有字段

    // --- 用戶信息 ---
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

    // --- 店家信息 ---
    @NotBlank(message = "店家名稱不能為空")
    @Size(max = 100, message = "店家名稱不能超過 100 個字符")
    private String shopName;

    @NotBlank(message = "店家地址不能為空")
    @Size(max = 512, message = "店家地址不能超過 512 個字符")
    private String address;

    @Size(max = 20, message = "電話號碼不能超過 20 個字符")
    private String phone; // 可選

    private String openingHours; // 可選，長文本

    private String description; // 可選，長文本

    // --- 初始上傳的照片 ---
    // 注意：DTO 中直接接收 MultipartFile 可能不適用於純 JSON API。
    // 通常文件上傳是單獨的請求或 multipart/form-data 請求。
    // 這裡我們先定義，但實際 Controller 處理可能需要調整。
    // 或者，前端先上傳圖片獲取 URL，再將 URL 包含在註冊請求中。
    // 為了匹配你的需求，我們先保留 MultipartFile，假設 Controller 能處理。
    private List<MultipartFile> initialPhotos; // 可選
}