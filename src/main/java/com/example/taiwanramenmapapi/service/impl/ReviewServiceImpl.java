package com.example.taiwanramenmapapi.service.impl;

import com.example.taiwanramenmapapi.dto.response.PageResponse;
import com.example.taiwanramenmapapi.dto.request.CreateReviewRequest;
import com.example.taiwanramenmapapi.dto.request.UpdateReviewRequest;
import com.example.taiwanramenmapapi.dto.response.ReviewDTO;
import com.example.taiwanramenmapapi.entity.*;
import com.example.taiwanramenmapapi.entity.enums.Role;
import com.example.taiwanramenmapapi.exception.BadRequestException;
import com.example.taiwanramenmapapi.exception.FileStorageException;
import com.example.taiwanramenmapapi.exception.ResourceNotFoundException;
import com.example.taiwanramenmapapi.exception.UnauthorizedActionException;
import com.example.taiwanramenmapapi.mapper.ReviewMapper;
import com.example.taiwanramenmapapi.repository.*;
import com.example.taiwanramenmapapi.service.FileStorageService;
import com.example.taiwanramenmapapi.service.ReviewService;
import com.example.taiwanramenmapapi.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReviewMediaRepository reviewMediaRepository;
    @Autowired private ReviewMapper reviewMapper;
    @Autowired private ShopService shopService;
    @Autowired private FileStorageService fileStorageService;

    @Override
    @Transactional
    public ReviewDTO createReview(Long shopId, CreateReviewRequest request, List<MultipartFile> files) {
        logger.info("Service: 嘗試為店家 ID {} 創建評論/回覆", shopId);
        User currentUser = getCurrentAuthenticatedUser();
        Shop shop = findShopByIdOrThrow(shopId);

        // 權限檢查：店家不能評論自己
        if (currentUser.getRole() == Role.ROLE_SHOP_OWNER && shop.getOwner() != null && Objects.equals(shop.getOwner().getId(), currentUser.getId())) {
            throw new UnauthorizedActionException("店家不能評論自己的店鋪");
        }

        Review parentReview = null;
        // 處理回覆邏輯
        if (request.getParentReviewId() != null) {
            parentReview = findReviewByIdOrThrow(request.getParentReviewId());
            if (!Objects.equals(parentReview.getShop().getId(), shopId)) { throw new IllegalArgumentException("回覆的父評論不屬於指定的店家"); }
            if (parentReview.getParentReview() != null) { throw new BadRequestException("不能回覆一個回覆"); }
            if (request.getRating() != null) { logger.warn("回覆評論時提供的評分被忽略 (Parent ID: {})", request.getParentReviewId()); }
            logger.info("創建對評論 ID {} 的回覆 by User {}", request.getParentReviewId(), currentUser.getUsername());
        }
        // 處理頂級評論邏輯
        else {
            logger.info("創建新的頂級評論 by User {}", currentUser.getUsername());
            if (request.getRating() == null) { throw new BadRequestException("頂級評論必須提供評分 (1-5)"); }
            if(request.getRating() < 1 || request.getRating() > 5) { throw new BadRequestException("評分必須在 1 到 5 之間"); }
            // 可選：檢查是否重複評論
        }

        // 創建 Review 實體
        Review review = Review.builder()
                .rating(parentReview == null ? request.getRating() : null)
                .content(request.getContent())
                .user(currentUser)
                .shop(shop)
                .parentReview(parentReview)
                .build();

        // 先保存 Review 以獲取 ID
        Review savedReview;
        try {
            savedReview = reviewRepository.save(review);
        } catch (Exception e) {
            logger.error("保存評論/回覆基礎信息失敗: {}", e.getMessage(), e);
            throw new RuntimeException("保存評論失敗", e);
        }

        // 如果是回覆，更新父評論回覆數
        if (parentReview != null) {
            long newReplyCount = reviewRepository.countByParentReviewId(parentReview.getId());
            parentReview.setReplyCount((int)newReplyCount);
            reviewRepository.save(parentReview);
            logger.debug("父評論 ID {} 的回覆數已更新為 {}", parentReview.getId(), newReplyCount);
        }

        // 處理上傳的照片
        if (files != null && !files.isEmpty()) {
            logger.info("正在處理評論 {} 的 {} 張照片...", savedReview.getId(), files.size());
            try {
                uploadReviewMediaInternal(savedReview, files);
                // *** 修改 catch 塊：只捕獲 RuntimeException ***
            } catch (RuntimeException e) {
                // *** --- ***
                logger.error("儲存評論 {} 的照片失敗: {}", savedReview.getId(), e.getMessage(), e);
                // 判斷具體異常類型（可選）
                if (e instanceof FileStorageException) {
                    logger.error("-> 文件儲存特定錯誤 (FileStorageException)");
                }
                // 拋出異常以回滾事務
                throw new RuntimeException("儲存評論照片失敗，評論創建已回滾。", e);
            }
        }

        // 觸發店家評分更新 (僅頂級評論)
        if (parentReview == null) {
            try {
                shopService.updateShopRating(shopId);
            } catch (Exception e) {
                logger.error("創建評論後更新店家 {} 評分失敗: {}", shopId, e.getMessage(), e);
            }
        }

        logger.info("評論/回覆 (ID: {}) 創建成功", savedReview.getId());

        // 重新從數據庫獲取最終狀態的 Review 實體 (包含關聯的 Media)
        Review finalReviewState = findReviewByIdOrThrow(savedReview.getId());
        // 使用包含最新狀態的 finalReviewState 進行轉換
        return reviewMapper.toReviewDTO(finalReviewState);
    }

    // --- 其他方法 (updateReview, deleteReview, getShopReviews, getReviewReplies, deleteReviewMedia) 保持不變 ---
    @Override
    @Transactional
    public ReviewDTO updateReview(Long reviewId, UpdateReviewRequest request) {
        logger.info("Service: 嘗試更新評論/回覆 ID: {}", reviewId);
        Review review = findReviewByIdOrThrow(reviewId);
        User currentUser = getCurrentAuthenticatedUser();
        checkReviewOwnership(review, currentUser); // 權限：作者

        boolean ratingChanged = false;
        if(request.getContent() != null) { review.setContent(request.getContent()); }
        if (review.getParentReview() == null && request.getRating() != null) {
            if (!Objects.equals(review.getRating(), request.getRating())) {
                if(request.getRating() < 1 || request.getRating() > 5) { throw new BadRequestException("評分必須在 1 到 5 之間"); }
                review.setRating(request.getRating());
                ratingChanged = true;
            }
        } else if (review.getParentReview() != null && request.getRating() != null) {
            logger.warn("嘗試更新回覆的評分 (ID: {})，操作被忽略。", reviewId);
        }

        Review updatedReview = reviewRepository.save(review);

        if (ratingChanged) {
            try { shopService.updateShopRating(review.getShop().getId()); }
            catch (Exception e) { logger.error("更新評論後更新店家 {} 評分失敗: {}", review.getShop().getId(), e.getMessage(), e); }
        }
        logger.info("評論/回覆 (ID: {}) 更新成功", updatedReview.getId());
        return reviewMapper.toReviewDTO(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        logger.warn("Service: 嘗試刪除評論/回覆 ID: {}", reviewId);
        Review review = findReviewByIdOrThrow(reviewId);
        User currentUser = getCurrentAuthenticatedUser();
        checkReviewOwnershipOrAdmin(review, currentUser); // 權限：作者或 Admin

        Long shopId = review.getShop().getId();
        boolean isTopLevel = review.getParentReview() == null;
        Long parentReviewId = review.getParentReview() != null ? review.getParentReview().getId() : null;

        // 1. 刪除關聯媒體文件
        deleteReviewMediaFilesInternal(review);

        // 2. 刪除評論
        reviewRepository.delete(review);
        logger.info("評論/回覆 (ID: {}) 已成功刪除", reviewId);

        // 3. 更新父評論回覆數
        if (parentReviewId != null) {
            reviewRepository.findById(parentReviewId).ifPresent(pReview -> {
                long newReplyCount = reviewRepository.countByParentReviewId(pReview.getId());
                pReview.setReplyCount((int)newReplyCount);
                reviewRepository.save(pReview);
                logger.debug("父評論 ID {} 的回覆數已更新為 {}", pReview.getId(), newReplyCount);
            });
        }

        // 4. 更新店家評分
        if (isTopLevel) {
            try { shopService.updateShopRating(shopId); }
            catch (Exception e) { logger.error("刪除評論後更新店家 {} 評分失敗: {}", shopId, e.getMessage(), e); }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewDTO> getShopReviews(Long shopId, Pageable pageable) {
        logger.debug("Service: 獲取店家 ID {} 的頂級評論，分頁: {}", shopId, pageable);
        if (!shopRepository.existsById(shopId)) { throw new ResourceNotFoundException("Shop", "id", shopId); }
        Page<Review> reviewPage = reviewRepository.findByShopIdAndParentReviewIsNull(shopId, pageable);
        Page<ReviewDTO> dtoPage = reviewPage.map(reviewMapper::toReviewDTO);
        return new PageResponse<>(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDTO> getReviewReplies(Long parentReviewId) {
        logger.debug("Service: 獲取評論 ID {} 的回覆", parentReviewId);
        if (!reviewRepository.existsById(parentReviewId)) { throw new ResourceNotFoundException("Review", "id", parentReviewId); }
        List<Review> replies = reviewRepository.findByParentReviewIdOrderByCreatedAtAsc(parentReviewId);
        return replies.stream().map(reviewMapper::toReviewDTO).collect(Collectors.toList());
    }


    @Override
    @Transactional
    public void deleteReviewMedia(Long reviewId, Long mediaId) {
        logger.warn("Service: 嘗試刪除評論 ID {} 的媒體 ID {}", reviewId, mediaId);
        Review review = findReviewByIdOrThrow(reviewId);
        User currentUser = getCurrentAuthenticatedUser();
        checkReviewOwnershipOrAdmin(review, currentUser);

        ReviewMedia media = reviewMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewMedia", "id", mediaId));
        if (!Objects.equals(media.getReview().getId(), reviewId)) {
            throw new UnauthorizedActionException("無法刪除非本評論的媒體文件");
        }

        try {
            fileStorageService.deleteFile(media.getUrl());
        } catch (Exception e) {
            logger.error("刪除評論媒體文件失敗: {}", media.getUrl(), e);
        }
        reviewMediaRepository.delete(media);
        logger.info("Service: 評論 ID {} 的媒體 ID {} 已成功刪除", reviewId, mediaId);
    }

    // --- 私有輔助方法 (保持不變) ---
    private List<ReviewMedia> uploadReviewMediaInternal(Review review, List<MultipartFile> files) { /* ... */
        List<ReviewMedia> savedMediaList = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) { logger.warn("評論 ID {} 上傳了非圖片文件 ({})，已跳過。", review.getId(), contentType); continue; }
            try {
                String storedFileName = fileStorageService.storeFile(file, "reviews/" + review.getId());
                ReviewMedia reviewMedia = ReviewMedia.builder().review(review).url(storedFileName).type("image").build();
                savedMediaList.add(reviewMediaRepository.save(reviewMedia));
                logger.debug("已保存評論媒體文件記錄: {}", storedFileName);
            } catch (FileStorageException e) {
                logger.error("儲存評論 {} 的媒體文件 {} 失敗: {}", review.getId(), file.getOriginalFilename(), e.getMessage());
                throw new RuntimeException("儲存評論照片失敗: " + file.getOriginalFilename(), e);
            }
        }
        return savedMediaList;
    }
    private void deleteReviewMediaFilesInternal(Review review) { /* ... */
        if (review == null) return;
        logger.warn("準備刪除評論 ID {} 的所有媒體文件...", review.getId());
        List<ReviewMedia> mediaList = reviewMediaRepository.findByReviewId(review.getId());
        int deletedCount = 0;
        for (ReviewMedia media : mediaList) {
            try { if (fileStorageService.deleteFile(media.getUrl())) { deletedCount++; } }
            catch (Exception e) { logger.error("刪除評論媒體文件失敗 (Review ID: {}, Media ID: {}, File: {}): {}", review.getId(), media.getId(), media.getUrl(), e.getMessage()); }
        }
        logger.info("評論 ID {} 的 {} 個媒體文件中的 {} 個已嘗試刪除。", review.getId(), mediaList.size(), deletedCount);
    }
    private Review findReviewByIdOrThrow(Long id) { /* ... */
        return reviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
    }
    private Shop findShopByIdOrThrow(Long id) { /* ... */
        return shopRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));
    }
    private User getCurrentAuthenticatedUser() { /* ... */
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal())) { throw new UnauthorizedActionException("用戶未登錄或認證無效"); }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) return (User) principal;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", "username (from context)", username));
        }
        throw new UnauthorizedActionException("無法識別的用戶信息");
    }
    private void checkReviewOwnership(Review review, User currentUser) { /* ... */
        if (review == null || review.getUser() == null) throw new IllegalArgumentException("評論或評論用戶不能為空");
        if (currentUser == null) throw new UnauthorizedActionException("無法獲取當前用戶信息");
        if (!Objects.equals(review.getUser().getId(), currentUser.getId())) { throw new UnauthorizedActionException("您只能操作自己的評論"); }
    }
    private void checkReviewOwnershipOrAdmin(Review review, User currentUser) { /* ... */
        if (review == null) throw new IllegalArgumentException("評論對象不能為空");
        if (currentUser == null) throw new UnauthorizedActionException("無法獲取當前用戶信息");
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
        boolean isOwner = review.getUser() != null && Objects.equals(review.getUser().getId(), currentUser.getId());
        if (!isAdmin && !isOwner) { throw new UnauthorizedActionException("您沒有權限執行此操作"); }
    }
    private long countByParentReviewId(Long parentReviewId) { /* ... */
        // 確保 ReviewRepository 有 countByParentReviewId 方法
        return reviewRepository.countByParentReviewId(parentReviewId);
    }
}