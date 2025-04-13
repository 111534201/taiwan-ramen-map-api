package com.example.taiwanramenmapapi.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateShopRequest {
    // 所有字段都是可選的，只更新傳入的字段
    @Size(max = 100, message = "店家名稱不能超過 100 個字符")
    private String name;

    @Size(max = 512, message = "店家地址不能超過 512 個字符")
    private String address; // 地址更新會觸發重新 Geocoding

    @Size(max = 20, message = "電話號碼不能超過 20 個字符")
    private String phone;

    private String openingHours; // 長文本

    private String description; // 長文本

    // 注意：圖片/影片的更新通常是通過單獨的上傳/刪除端點完成，
    // 而不是直接包含在更新請求中。
}