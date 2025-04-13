package com.example.taiwanramenmapapi.controller;

import com.example.taiwanramenmapapi.dto.response.PageResponse;
import com.example.taiwanramenmapapi.dto.request.CreateReviewRequest;
import com.example.taiwanramenmapapi.dto.request.UpdateReviewRequest;
import com.example.taiwanramenmapapi.dto.response.ApiResponse;
import com.example.taiwanramenmapapi.dto.response.ReviewDTO;
import com.example.taiwanramenmapapi.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // 引入 MediaType
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews") // Controller 的基礎路徑
@Validated // 啟用方法級別的參數驗證 (例如 @Min, @Max)
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewService reviewService; // 注入 ReviewService

    /**
     * POST /api/reviews/shop/{shopId} : 為指定店家創建評論或回覆 (包含照片)。
     * 需要 multipart/form-data 格式的請求。
     * 請求體需要包含名為 "reviewData" 的 JSON 部分 (CreateReviewRequest) 和名為 "photos" 的文件部分。
     * @param shopId 店家 ID
     * @param createReviewRequest 評論的 JSON 數據
     * @param photos 上傳的照片文件列表 (可選)
     * @return 創建成功的評論 DTO
     */
    @PostMapping(value = "/shop/{shopId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}) // 指定 consumes
    @PreAuthorize("isAuthenticated()") // 必須登入才能評論/回覆
    public ResponseEntity<ApiResponse<ReviewDTO>> createReview(
            @PathVariable Long shopId,
            @Valid @RequestPart("reviewData") CreateReviewRequest createReviewRequest, // 驗證 JSON 部分
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) { // 文件部分非必須

        logger.info("Controller: 收到為店家 ID {} 創建評論/回覆的請求", shopId);
        ReviewDTO createdReview = reviewService.createReview(shopId, createReviewRequest, photos);
        return ResponseEntity.status(HttpStatus.CREATED) // 返回 201 Created
                .body(ApiResponse.success(createdReview, "評論/回覆創建成功"));
    }

    /**
     * PUT /api/reviews/{reviewId} : 更新評論或回覆的文字內容/評分。
     * 只處理 JSON 請求體。圖片的增刪需要調用其他接口。
     * @param reviewId 要更新的評論 ID
     * @param updateReviewRequest 包含 content 和/或 rating 的請求體
     * @return 更新後的評論 DTO
     */
    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()") // 必須登入，Service 層會檢查是否為作者
    public ResponseEntity<ApiResponse<ReviewDTO>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest updateReviewRequest) { // 接收 JSON 請求體

        logger.info("Controller: 收到更新評論/回覆 ID {} 的請求", reviewId);
        ReviewDTO updatedReview = reviewService.updateReview(reviewId, updateReviewRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedReview, "評論/回覆更新成功")); // 返回 200 OK
    }

    /**
     * DELETE /api/reviews/{reviewId} : 刪除評論或回覆。
     * 會級聯刪除回覆及其媒體文件。
     * @param reviewId 要刪除的評論 ID
     * @return 成功響應 (無數據)
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()") // 必須登入，Service 層會檢查權限 (作者或管理員)
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        logger.warn("Controller: 收到刪除評論/回覆 ID {} 的請求", reviewId); // 使用 warn 級別
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("評論/回覆刪除成功")); // 返回 200 OK
    }

    /**
     * GET /api/reviews/shop/{shopId} : 獲取指定店家的頂級評論列表 (分頁)。
     * @param shopId 店家 ID
     * @param page 頁碼 (從 0 開始)
     * @param size 每頁數量
     * @param sortBy 排序字段 (預設 createdAt)
     * @param sortDir 排序方向 (預設 DESC)
     * @return 包含評論分頁數據的響應
     */
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewDTO>>> getShopReviews(
            @PathVariable Long shopId,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "5") @Min(1) @Max(50) int size, // 限制每頁最大數量
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "DESC") String sortDir) {

        logger.debug("Controller: 獲取店家 ID {} 評論: page={}, size={}, sort={}", shopId, page, size, sortBy + "," + sortDir);
        // 驗證排序字段
        List<String> allowedSortBy = List.of("createdAt", "rating");
        if (!allowedSortBy.contains(sortBy)) {
            sortBy = "createdAt"; // 如果不合法，使用預設值
        }
        Sort.Direction direction = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponse<ReviewDTO> reviews = reviewService.getShopReviews(shopId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * GET /api/reviews/{parentReviewId}/replies : 獲取指定評論的所有回覆列表。
     * @param parentReviewId 父評論 ID
     * @return 包含回覆列表的響應
     */
    @GetMapping("/{parentReviewId}/replies")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> getReviewReplies(@PathVariable Long parentReviewId) {
        logger.debug("Controller: 獲取評論 ID {} 的回覆", parentReviewId);
        List<ReviewDTO> replies = reviewService.getReviewReplies(parentReviewId);
        return ResponseEntity.ok(ApiResponse.success(replies));
    }


    /**
     * POST /api/reviews/{reviewId}/media : 為指定評論添加照片。
     * 需要 multipart/form-data 格式的請求。
     * @param reviewId 評論 ID
     * @param photos 上傳的照片文件列表 (請求參數名為 "photos")
     * @return 更新後的評論 DTO (包含新的媒體列表)
     */
    @PostMapping(value = "/{reviewId}/media", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}) // 指定 consumes
    @PreAuthorize("isAuthenticated()") // 至少需要登入，Service 層會做所有權檢查
    public ResponseEntity<ApiResponse<ReviewDTO>> addReviewMedia(
            @PathVariable Long reviewId,
            @RequestParam("photos") List<MultipartFile> photos) { // 使用 @RequestParam 接收文件列表

        logger.info("Controller: 收到為評論 ID {} 添加照片的請求，文件數量: {}", reviewId, photos != null ? photos.size() : 0);
        // Service 層會處理文件為空的檢查，Controller 層也可以加一道保險
        if (photos == null || photos.isEmpty() || photos.stream().allMatch(MultipartFile::isEmpty)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("未選擇任何有效文件進行上傳"));
        }

        ReviewDTO updatedReview = reviewService.addMediaToReview(reviewId, photos);
        return ResponseEntity.ok(ApiResponse.success(updatedReview, "照片添加成功"));
    }


    /**
     * DELETE /api/reviews/{reviewId}/media/{mediaId} : 刪除評論的指定媒體文件。
     * @param reviewId 評論 ID
     * @param mediaId 要刪除的媒體 ID
     * @return 成功響應 (無數據)
     */
    @DeleteMapping("/{reviewId}/media/{mediaId}")
    @PreAuthorize("isAuthenticated()") // 必須登入，Service 層會檢查權限 (作者或管理員)
    public ResponseEntity<ApiResponse<Void>> deleteReviewMedia(
            @PathVariable Long reviewId,
            @PathVariable Long mediaId) {
        logger.warn("Controller: 收到刪除評論 ID {} 的媒體 ID {} 的請求", reviewId, mediaId); // 使用 warn 級別
        reviewService.deleteReviewMedia(reviewId, mediaId);
        return ResponseEntity.ok(ApiResponse.success("評論媒體刪除成功")); // 返回 200 OK
    }
}