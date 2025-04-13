package com.example.taiwanramenmapapi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
// import jakarta.validation.constraints.NotBlank; // 內容可以為空，但評分頂級評論必填
import lombok.Data;

@Data
public class CreateReviewRequest {
    // 評分 (只有頂級評論需要，回覆不需要)
    // 在 Service 層進行邏輯判斷是否必填
    @Min(value = 1, message = "評分不能低於 1")
    @Max(value = 5, message = "評分不能高於 5")
    private Integer rating;

    // 評論內容 (可以為空，如果只給評分或只上傳圖片)
    private String content;

    // 父評論 ID (如果是回覆，則提供此 ID)
    private Long parentReviewId;

    // 圖片 URL 列表 (前端先上傳，後端接收 URL) - 或者處理文件上傳
    // private List<String> photoUrls; // 方案一：接收 URL
    // 方案二：接收文件，但如前所述，DTO 不適合直接放 MultipartFile
}