package com.example.taiwanramenmapapi.dto.response;

import com.example.taiwanramenmapapi.entity.enums.Role;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List; // 引入 List

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 可選：是否返回擁有的店家 ID 列表 (如果需要)
    private List<Long> ownedShopIds; // 只返回 ID 列表
    // private List<ShopSummaryDTO> ownedShops; // 或者返回簡要店家信息
}