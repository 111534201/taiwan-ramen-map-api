package com.example.taiwanramenmapapi.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewDTO {
    private Long id;
    private Integer rating; // 頂級評論才有
    private String content;
    private ReviewerDTO user; // 評論者信息
    private Long shopId; // 所屬店家 ID
    private Long parentReviewId; // 父評論 ID (如果是回覆)
    private Integer replyCount; // 回覆數
    private List<ReviewMediaDTO> media; // 評論附帶的媒體 (照片)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // 可選：是否為當前用戶所寫 (方便前端判斷是否顯示編輯/刪除按鈕)
    // private boolean isOwnReview;
}