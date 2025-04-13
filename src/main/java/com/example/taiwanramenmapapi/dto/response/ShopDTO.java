package com.example.taiwanramenmapapi.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ShopDTO {
    private Long id;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private String openingHours;
    private String description;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private BigDecimal weightedRating; // 用於排行
    private ReviewerDTO owner; // 店家擁有者信息 (只含 ID 和用戶名)
    private List<ShopMediaDTO> media; // 店家媒體列表
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}