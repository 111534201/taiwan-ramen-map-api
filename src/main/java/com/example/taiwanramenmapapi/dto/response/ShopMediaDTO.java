package com.example.taiwanramenmapapi.dto.response;

import lombok.Data;

@Data
public class ShopMediaDTO {
    private Long id;
    private String type; // "image" or "video"
    private String url; // 可訪問的 URL
    private Integer displayOrder;
}