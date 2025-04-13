package com.example.taiwanramenmapapi.service;

import com.example.taiwanramenmapapi.dto.response.PageResponse;
import com.example.taiwanramenmapapi.dto.request.CreateReviewRequest;
import com.example.taiwanramenmapapi.dto.request.UpdateReviewRequest;
import com.example.taiwanramenmapapi.dto.response.ReviewDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile; // 引入

import java.util.List;

public interface ReviewService {

    /**
     * 創建新的頂級評論或回覆
     * @param shopId 評論所屬的店家 ID
     * @param createReviewRequest 評論數據 (包含評分、內容、可選的父評論 ID)
     * @param files 可選的上傳照片列表
     * @return 創建成功的評論 DTO
     */
    ReviewDTO createReview(Long shopId, CreateReviewRequest createReviewRequest, List<MultipartFile> files);

    /**
     * 更新評論或回覆
     * @param reviewId 要更新的評論 ID
     * @param updateReviewRequest 更新的數據
     * @return 更新後的評論 DTO
     */
    ReviewDTO updateReview(Long reviewId, UpdateReviewRequest updateReviewRequest);

    /**
     * 刪除評論或回覆 (以及其下的所有回覆)
     * @param reviewId 要刪除的評論 ID
     */
    void deleteReview(Long reviewId);

    /**
     * 獲取特定店家的頂級評論列表 (分頁)
     * @param shopId 店家 ID
     * @param pageable 分頁和排序信息
     * @return 評論的分頁響應
     */
    PageResponse<ReviewDTO> getShopReviews(Long shopId, Pageable pageable);

    /**
     * 獲取特定評論的所有回覆列表 (不分頁)
     * @param parentReviewId 父評論 ID
     * @return 回覆列表
     */
    List<ReviewDTO> getReviewReplies(Long parentReviewId);

    /**
     * 為指定評論上傳照片
     * @param reviewId 評論 ID
     * @param files 上傳的文件列表
     * @return 更新後的評論 DTO
     */
    // ReviewDTO uploadReviewMedia(Long reviewId, List<MultipartFile> files); // 考慮合併到 createReview

    /**
     * 刪除指定的評論媒體
     * @param reviewId 評論 ID
     * @param mediaId 要刪除的媒體 ID
     */
    void deleteReviewMedia(Long reviewId, Long mediaId);
}