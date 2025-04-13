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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Validated
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    private ReviewService reviewService;

    /**
     * POST /api/reviews/shop/{shopId} : 創建評論或回覆 (含照片)
     */
    @PostMapping(value = "/shop/{shopId}", consumes = {"multipart/form-data"})
    @PreAuthorize("isAuthenticated()") // 必須登入
    public ResponseEntity<ApiResponse<ReviewDTO>> createReview(
            @PathVariable Long shopId,
            @Valid @RequestPart("reviewData") CreateReviewRequest createReviewRequest,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        logger.info("為店家 ID {} 創建評論/回覆", shopId);
        ReviewDTO createdReview = reviewService.createReview(shopId, createReviewRequest, photos);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdReview, "評論/回覆創建成功"));
    }

    /**
     * PUT /api/reviews/{reviewId} : 更新評論或回覆
     */
    @PutMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()") // 必須登入 (Service 檢查作者)
    public ResponseEntity<ApiResponse<ReviewDTO>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest updateReviewRequest) {
        logger.info("更新評論/回覆 ID {}", reviewId);
        ReviewDTO updatedReview = reviewService.updateReview(reviewId, updateReviewRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedReview, "評論/回覆更新成功"));
    }

    /**
     * DELETE /api/reviews/{reviewId} : 刪除評論或回覆
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()") // 必須登入 (Service 檢查作者或 Admin)
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        logger.warn("刪除評論/回覆 ID {}", reviewId);
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("評論/回覆刪除成功"));
    }

    /**
     * GET /api/reviews/shop/{shopId} : 獲取店家頂級評論 (分頁)
     */
    @GetMapping("/shop/{shopId}")
    public ResponseEntity<ApiResponse<PageResponse<ReviewDTO>>> getShopReviews(
            @PathVariable Long shopId,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "5") @Min(1) @Max(50) int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "DESC") String sortDir) {
        logger.debug("獲取店家 ID {} 評論: page={}, size={}, sort={}", shopId, page, size, sortBy + "," + sortDir);
        List<String> allowedSortBy = List.of("createdAt", "rating");
        if (!allowedSortBy.contains(sortBy)) { sortBy = "createdAt"; }
        Sort.Direction direction = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<ReviewDTO> reviews = reviewService.getShopReviews(shopId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    /**
     * GET /api/reviews/{parentReviewId}/replies : 獲取評論的回覆列表
     */
    @GetMapping("/{parentReviewId}/replies")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> getReviewReplies(@PathVariable Long parentReviewId) {
        logger.debug("獲取評論 ID {} 的回覆", parentReviewId);
        List<ReviewDTO> replies = reviewService.getReviewReplies(parentReviewId);
        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    /**
     * DELETE /api/reviews/{reviewId}/media/{mediaId} : 刪除評論媒體
     */
    @DeleteMapping("/{reviewId}/media/{mediaId}")
    @PreAuthorize("isAuthenticated()") // 必須登入 (Service 檢查作者或 Admin)
    public ResponseEntity<ApiResponse<Void>> deleteReviewMedia(
            @PathVariable Long reviewId,
            @PathVariable Long mediaId) {
        logger.warn("刪除評論 ID {} 的媒體 ID {}", reviewId, mediaId);
        reviewService.deleteReviewMedia(reviewId, mediaId);
        return ResponseEntity.ok(ApiResponse.success("評論媒體刪除成功"));
    }
}