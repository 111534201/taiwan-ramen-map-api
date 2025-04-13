package com.example.taiwanramenmapapi.service;

import com.example.taiwanramenmapapi.dto.response.PageResponse;
import com.example.taiwanramenmapapi.dto.request.CreateReviewRequest;
import com.example.taiwanramenmapapi.dto.request.UpdateReviewRequest;
import com.example.taiwanramenmapapi.dto.response.ReviewDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile; // 引入

import java.util.List;

/**
 * 評論服務接口，定義了與評論和回覆相關的業務邏輯。
 */
public interface ReviewService {

    /**
     * 創建新的頂級評論或回覆。
     * @param shopId 評論所屬的店家 ID。
     * @param createReviewRequest 評論數據 (包含評分、內容、可選的父評論 ID)。
     * @param files 可選的上傳照片列表。
     * @return 創建成功的評論 DTO。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果店家或父評論不存在。
     * @throws com.example.taiwanramenmapapi.exception.UnauthorizedActionException 如果店家嘗試評論自己。
     * @throws com.example.taiwanramenmapapi.exception.BadRequestException 如果輸入驗證失敗 (例如評分無效、嘗試回覆回覆等)。
     */
    ReviewDTO createReview(Long shopId, CreateReviewRequest createReviewRequest, List<MultipartFile> files);

    /**
     * 更新評論或回覆的文字內容和/或評分 (僅限頂級評論)。
     * @param reviewId 要更新的評論 ID。
     * @param updateReviewRequest 更新的數據 (只包含 content 和 rating)。
     * @return 更新後的評論 DTO。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果評論不存在。
     * @throws com.example.taiwanramenmapapi.exception.UnauthorizedActionException 如果用戶無權修改此評論。
     * @throws com.example.taiwanramenmapapi.exception.BadRequestException 如果評分值無效。
     */
    ReviewDTO updateReview(Long reviewId, UpdateReviewRequest updateReviewRequest);

    /**
     * 刪除評論或回覆 (以及其下的所有回覆和關聯媒體)。
     * @param reviewId 要刪除的評論 ID。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果評論不存在。
     * @throws com.example.taiwanramenmapapi.exception.UnauthorizedActionException 如果用戶無權刪除此評論。
     */
    void deleteReview(Long reviewId);

    /**
     * 獲取特定店家的頂級評論列表 (分頁)。
     * @param shopId 店家 ID。
     * @param pageable 分頁和排序信息。
     * @return 評論的分頁響應。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果店家不存在。
     */
    PageResponse<ReviewDTO> getShopReviews(Long shopId, Pageable pageable);

    /**
     * 獲取特定評論的所有回覆列表 (不分頁，通常按時間排序)。
     * @param parentReviewId 父評論 ID。
     * @return 回覆列表 DTO。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果父評論不存在。
     */
    List<ReviewDTO> getReviewReplies(Long parentReviewId);

    /**
     * 為現有評論添加新的媒體文件（照片）。
     * @param reviewId 評論 ID。
     * @param files 上傳的文件列表。
     * @return 更新後的評論 DTO (包含新的媒體列表)。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果評論不存在。
     * @throws com.example.taiwanramenmapapi.exception.UnauthorizedActionException 如果用戶無權修改此評論。
     * @throws com.example.taiwanramenmapapi.exception.FileStorageException 如果文件儲存失敗。
     * @throws com.example.taiwanramenmapapi.exception.BadRequestException 如果上傳數量超出限制或未提供文件。
     */
    ReviewDTO addMediaToReview(Long reviewId, List<MultipartFile> files); // <-- 新增的方法簽名

    /**
     * 刪除指定的評論媒體文件和記錄。
     * @param reviewId 評論 ID。
     * @param mediaId 要刪除的媒體 ID。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果評論或媒體記錄不存在。
     * @throws com.example.taiwanramenmapapi.exception.UnauthorizedActionException 如果用戶無權刪除此媒體。
     */
    void deleteReviewMedia(Long reviewId, Long mediaId);
}