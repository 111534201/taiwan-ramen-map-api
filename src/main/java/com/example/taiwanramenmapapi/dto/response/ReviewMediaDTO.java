package com.example.taiwanramenmapapi.dto.response;

import lombok.Data;

@Data
public class ReviewMediaDTO {
    private Long id;
    private String type; // "image"
    private String url; // 可訪問的 URL
}