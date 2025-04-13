package com.example.taiwanramenmapapi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateReviewRequest {
    // 評分 (只有頂級評論可更新)
    @Min(value = 1, message = "評分不能低於 1")
    @Max(value = 5, message = "評分不能高於 5")
    private Integer rating;

    // 評論內容
    private String content;
}