package com.example.taiwanramenmapapi.dto.response;

import lombok.Data;

@Data
public class ReviewerDTO { // 用於 ReviewDTO 中顯示評論者信息
    private Long id;
    private String username;
    // 可以添加頭像 URL 等
}