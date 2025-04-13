// src/main/java/com/example/taiwanramenmapapi/service/impl/ReviewServiceImpl.java
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
import com.example.taiwanramenmapapi.mapper.UserMapper; // 確保引入 UserMapper
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal; // 雖然這裡沒直接用，但 ShopService 會用
import java.math.RoundingMode; // 雖然這裡沒直接用，但 ShopService 會用
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper; // 注入 UserMapper
    private final ShopService shopService; // 保持注入，用於更新評分
    private final FileStorageService fileStorageService; // 文件儲存服務

    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository, ShopRepository shopRepository, UserRepository userRepository, ReviewMediaRepository reviewMediaRepository, ReviewMapper reviewMapper, UserMapper userMapper, ShopService shopService, FileStorageService fileStorageService) {
        this.reviewRepository = reviewRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.reviewMediaRepository = reviewMediaRepository;
        this.reviewMapper = reviewMapper;
        this.userMapper = userMapper;
        this.shopService = shopService;
        this.fileStorageService = fileStorageService; // 初始化 FileStorageService
    }

    /**
     * 創建評論或回覆 (使用文件上傳方式)
     */
    @Override
    @Transactional
    public ReviewDTO createReview(Long shopId, CreateReviewRequest request, List<MultipartFile> files) {
        logger.info("Service: 嘗試為店家 ID {} 創建評論/回覆", shopId);
        User currentUser = getCurrentAuthenticatedUser();
        Shop shop = findShopByIdOrThrow(shopId);

        Review parentReview = null;
        // 處理回覆邏輯
        if (request.getParentReviewId() != null) {
            parentReview = findReviewByIdOrThrow(request.getParentReviewId());
            if (!Objects.equals(parentReview.getShop().getId(), shopId)) throw new IllegalArgumentException("回覆的父評論不屬於指定的店家");
            if (parentReview.getParentReview() != null) throw new BadRequestException("不能回覆一個回覆");
            if (request.getRating() != null) logger.warn("回覆評論時提供的評分被忽略 (Parent ID: {})", request.getParentReviewId());
            logger.info("創建對評論 ID {} 的回覆 by User {}", request.getParentReviewId(), currentUser.getUsername());
            // *** 店家可以回覆自己的店鋪評論，移除這裡的檢查 ***
        }
        // 處理頂級評論邏輯
        else {
            // *** 只有在創建頂級評論時，才檢查店家是否評論自己 ***
            if (currentUser.getRole() == Role.ROLE_SHOP_OWNER && shop.getOwner() != null && Objects.equals(shop.getOwner().getId(), currentUser.getId())) {
                throw new UnauthorizedActionException("店家不能評論自己的店鋪");
            }
            // *** --- ***
            logger.info("創建新的頂級評論 by User {}", currentUser.getUsername());
            if (request.getRating() == null) throw new BadRequestException("頂級評論必須提供評分 (1-5)");
            if(request.getRating() < 1 || request.getRating() > 5) throw new BadRequestException("評分必須在 1 到 5 之間");
            // 允許內容和圖片都為空嗎？根據業務決定，這裡假設至少要有內容或圖片
            if (!StringUtils.hasText(request.getContent()) && (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty))) {
                throw new BadRequestException("評論內容和照片不能同時為空");
            }
        }

        // 創建 Review 實體
        Review review = Review.builder()
                .rating(parentReview == null ? request.getRating() : null)
                .content(request.getContent())
                .user(currentUser).shop(shop).parentReview(parentReview)
                .replyCount(0).media(new ArrayList<>())
                .build();

        Review savedReview;
        try {
            savedReview = reviewRepository.save(review); // 先保存 Review
            logger.info("評論/回覆基礎信息 (ID: {}) 已保存", savedReview.getId());
        } catch (Exception e) { logger.error("保存評論/回覆基礎信息失敗: {}", e.getMessage(), e); throw new RuntimeException("保存評論失敗", e); }

        // 處理文件上傳
        if (files != null && !files.isEmpty()) {
            logger.info("正在處理評論 {} 的 {} 張照片...", savedReview.getId(), files.size());
            try {
                uploadReviewMediaInternal(savedReview, files); // 調用內部方法處理
            } catch (RuntimeException e) { logger.error("儲存評論 {} 的照片失敗，事務將回滾: {}", savedReview.getId(), e.getMessage(), e); throw e; }
        }

        // 更新父評論回覆數
        if (parentReview != null) {
            try { long newReplyCount = countByParentReviewId(parentReview.getId()); parentReview.setReplyCount((int)newReplyCount); reviewRepository.save(parentReview); logger.debug("父評論 ID {} 的回覆數已更新為 {}", parentReview.getId(), newReplyCount); }
            catch (Exception e) { logger.error("更新父評論 {} 回覆數失敗: {}", parentReview.getId(), e.getMessage(), e); }
        }
        // 更新店家評分
        if (parentReview == null && savedReview.getRating() != null) {
            try { shopService.updateShopRating(shopId); }
            catch (Exception e) { logger.error("創建評論後更新店家 {} 評分失敗: {}", shopId, e.getMessage(), e); }
        }

        logger.info("評論/回覆 (ID: {}) 完整創建成功", savedReview.getId());
        return reviewMapper.toReviewDTO(findReviewByIdOrThrow(savedReview.getId())); // 返回最新數據
    }


    /**
     * 更新評論或回覆的文字內容和/或評分 (不處理圖片)。
     */
    @Override
    @Transactional
    public ReviewDTO updateReview(Long reviewId, UpdateReviewRequest request) {
        logger.info("Service: 嘗試更新評論/回覆 ID: {}", reviewId);
        Review review = findReviewByIdOrThrow(reviewId);
        User currentUser = getCurrentAuthenticatedUser();
        checkReviewOwnership(review, currentUser);

        boolean changed = false;
        Integer oldRating = review.getRating();
        String oldContent = review.getContent();

        logger.debug("[UPDATE] 收到請求: content='{}', rating={}", request.getContent(), request.getRating());
        logger.debug("[UPDATE] 更新前實體: content='{}', rating={}", oldContent, oldRating);

        // 1. 更新內容
        if (request.getContent() != null && !Objects.equals(oldContent, request.getContent())) {
            review.setContent(request.getContent()); changed = true;
            logger.info("[UPDATE] 內容已更新 for review ID {}", reviewId);
        } else { logger.debug("[UPDATE] 內容未提供或未更改，不更新。", reviewId); }

        // 2. 更新評分 (僅限頂級評論)
        if (review.getParentReview() == null && request.getRating() != null) {
            if(request.getRating() < 1 || request.getRating() > 5) throw new BadRequestException("評分必須在 1 到 5 之間");
            if (!Objects.equals(oldRating, request.getRating())) {
                review.setRating(request.getRating()); changed = true;
                logger.info("[UPDATE] 評分已更新 for review ID {}", reviewId);
            } else { logger.debug("[UPDATE] 評分未提供或與之前相同，不更新。", reviewId); }
        } else if (review.getParentReview() != null && request.getRating() != null) {
            logger.warn("[UPDATE] 嘗試更新回覆的評分 (ID: {})，操作被忽略。", reviewId);
        }

        // 3. 如果有更改，保存並更新店家評分
        if (changed) {
            logger.info("[UPDATE] 檢測到更改，執行保存操作 for review ID {}", reviewId);
            try {
                review = reviewRepository.saveAndFlush(review);
                logger.info("[UPDATE] 評論/回覆 (ID: {}) 已成功更新到數據庫 (已 flush)", review.getId());

                Review persistedReview = findReviewByIdOrThrow(reviewId); // 重新獲取
                logger.info("[UPDATE] 從數據庫重新獲取: content='{}', rating={}", persistedReview.getContent(), persistedReview.getRating());

                boolean actualRatingChanged = persistedReview.getParentReview() == null && !Objects.equals(oldRating, persistedReview.getRating());
                if (actualRatingChanged) {
                    logger.info("[UPDATE] 評分更改，觸發店家評分更新 for shop ID {}", persistedReview.getShop().getId());
                    shopService.updateShopRating(persistedReview.getShop().getId());
                } else { logger.debug("[UPDATE] 評分未實際更改，不觸發店家評分更新。"); }

                return reviewMapper.toReviewDTO(persistedReview); // 返回 DTO

            } catch (Exception e) { logger.error("[UPDATE] 保存或處理更新後的評論 ID {} 失敗: {}", reviewId, e.getMessage(), e); throw new RuntimeException("更新評論時發生錯誤", e); }
        } else {
            logger.info("[UPDATE] 評論/回覆 (ID: {}) 無需更新。", reviewId);
            return reviewMapper.toReviewDTO(review); // 返回當前 DTO
        }
    }


    /**
     * 刪除評論或回覆 (及其所有關聯數據和文件)。
     */
    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        logger.warn("Service: 嘗試刪除評論/回覆 ID: {}", reviewId);
        Review review = findReviewByIdOrThrow(reviewId);
        User currentUser = getCurrentAuthenticatedUser();
        checkReviewOwnershipOrAdmin(review, currentUser); // 權限檢查

        Long shopId = review.getShop().getId();
        boolean isTopLevel = review.getParentReview() == null;
        boolean hadRating = review.getRating() != null;
        Long parentReviewId = review.getParentReview() != null ? review.getParentReview().getId() : null;

        // 1. 清理該評論及其所有子回覆關聯的物理媒體文件和 DB 記錄
        //    遞歸或迭代清理所有子孫回覆的媒體文件
        deleteReviewAndRepliesMediaRecursive(review);

        // 2. 刪除評論實體 (級聯刪除應該會處理 DB 記錄)
        reviewRepository.delete(review);
        logger.info("評論/回覆 (ID: {}) 及其數據庫關聯記錄已成功刪除", reviewId);

        // 3. 更新父評論的回覆數
        if (parentReviewId != null) {
            reviewRepository.findById(parentReviewId).ifPresent(pReview -> {
                long newReplyCount = countByParentReviewId(pReview.getId());
                pReview.setReplyCount((int)newReplyCount);
                reviewRepository.save(pReview);
                logger.debug("父評論 ID {} 的回覆數已更新為 {}", pReview.getId(), newReplyCount);
            });
        }
        // 4. 更新店家評分
        if (isTopLevel && hadRating) {
            try { shopService.updateShopRating(shopId); }
            catch (Exception e) { logger.error("刪除評論後更新店家 {} 評分失敗: {}", shopId, e.getMessage(), e); }
        }
    }

    /**
     * 遞歸刪除評論及其所有子回覆的媒體文件和記錄。
     * @param review 要處理的評論或回覆
     */
    private void deleteReviewAndRepliesMediaRecursive(Review review) {
        if (review == null) return;
        // 先刪除當前評論/回覆的媒體
        deleteReviewMediaFilesInternal(review);
        // 遞歸刪除子回覆的媒體
        List<Review> replies = reviewRepository.findByParentReviewIdOrderByCreatedAtAsc(review.getId());
        for (Review reply : replies) {
            deleteReviewAndRepliesMediaRecursive(reply); // 遞歸調用
        }
    }


    /**
     * 獲取店家頂級評論 (分頁)。
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewDTO> getShopReviews(Long shopId, Pageable pageable) {
        logger.debug("Service: 獲取店家 ID {} 的頂級評論，分頁: {}", shopId, pageable);
        if (!shopRepository.existsById(shopId)) { throw new ResourceNotFoundException("Shop", "id", shopId); }
        Page<Review> reviewPage = reviewRepository.findByShopIdAndParentReviewIsNull(shopId, pageable);
        logger.debug("Service: 為店家 ID {} 找到 {} 條頂級評論 (當前頁)", shopId, reviewPage.getNumberOfElements());
        Page<ReviewDTO> dtoPage = reviewPage.map(reviewMapper::toReviewDTO);
        return new PageResponse<>(dtoPage);
    }

    /**
     * 獲取評論的回覆列表。
     */
    @Override
    @Transactional(readOnly = true)
    public List<ReviewDTO> getReviewReplies(Long parentReviewId) {
        logger.debug("Service: 獲取評論 ID {} 的回覆", parentReviewId);
        if (!reviewRepository.existsById(parentReviewId)) { throw new ResourceNotFoundException("Review", "id", parentReviewId); }
        List<Review> replies = reviewRepository.findByParentReviewIdOrderByCreatedAtAsc(parentReviewId);
        logger.debug("Service: 為評論 ID {} 找到 {} 條回覆", parentReviewId, replies.size());
        return replies.stream().map(reviewMapper::toReviewDTO).collect(Collectors.toList());
    }

    /**
     * 為評論添加媒體文件。
     */
    @Override
    @Transactional
    public ReviewDTO addMediaToReview(Long reviewId, List<MultipartFile> files) {
        logger.info("Service: 嘗試為評論 ID {} 添加 {} 個媒體文件", reviewId, files != null ? files.size() : 0);
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            throw new BadRequestException("必須提供要上傳的文件");
        }
        Review review = findReviewByIdOrThrow(reviewId);
        User currentUser = getCurrentAuthenticatedUser();
        checkReviewOwnershipOrAdmin(review, currentUser);
        try {
            uploadReviewMediaInternal(review, files); // 處理上傳
            logger.info("成功為評論 ID {} 添加了媒體文件", reviewId);
        } catch (RuntimeException e) { logger.error("為評論 {} 添加媒體時失敗: {}", reviewId, e.getMessage(), e); throw e; }
        return reviewMapper.toReviewDTO(findReviewByIdOrThrow(reviewId)); // 返回最新 DTO
    }

    /**
     * 刪除指定的評論媒體文件和記錄。
     */
    @Override
    @Transactional
    public void deleteReviewMedia(Long reviewId, Long mediaId) { // <--- 方法名保持不變，與 Controller 對應
        logger.warn("Service: 嘗試刪除評論 ID {} 的媒體 ID {}", reviewId, mediaId);
        Review review = findReviewByIdOrThrow(reviewId);
        User currentUser = getCurrentAuthenticatedUser();
        checkReviewOwnershipOrAdmin(review, currentUser);

        ReviewMedia media = reviewMediaRepository.findByIdAndReviewId(mediaId, reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewMedia", "id/reviewId", mediaId + "/" + reviewId));

        try {
            // 刪除物理文件
            if (StringUtils.hasText(media.getUrl())) {
                if (fileStorageService.deleteFile(media.getUrl())) {
                    logger.info("成功刪除物理文件: {}", media.getUrl());
                } else {
                    logger.warn("物理文件 {} 可能不存在或刪除失敗。", media.getUrl());
                }
            }
            // 刪除數據庫記錄
            reviewMediaRepository.delete(media);
            logger.info("Service: 評論 ID {} 的媒體 ID {} 記錄已成功刪除", reviewId, mediaId);
        } catch (Exception e) {
            logger.error("刪除評論媒體文件或記錄時出錯 (Media ID: {}): {}", mediaId, e.getMessage(), e);
            throw new RuntimeException("刪除評論媒體失敗", e);
        }
    }

    // --- 私有輔助方法 ---

    /** 內部處理評論媒體上傳和保存 */
    private List<ReviewMedia> uploadReviewMediaInternal(Review review, List<MultipartFile> files) {
        List<ReviewMedia> savedMediaList = new ArrayList<>();
        if (files == null || files.isEmpty()) return savedMediaList;
        int maxPhotos = 5; long currentMediaCount = reviewMediaRepository.countByReviewId(review.getId());
        long remainingSlots = maxPhotos - currentMediaCount;
        if (remainingSlots <= 0) throw new BadRequestException("照片數量已達上限 (" + maxPhotos + " 張)，無法再上傳。");
        long uploadedCount = 0;
        for (MultipartFile file : files) {
            if (uploadedCount >= remainingSlots) { logger.warn("..."); break; }
            if (file == null || file.isEmpty()) continue;
            String contentType = file.getContentType(); long maxSize = 5 * 1024 * 1024;
            if (contentType == null || !contentType.startsWith("image/")) { logger.warn("...非圖片..."); continue; }
            if (file.getSize() > maxSize) { logger.warn("...大小超限..."); continue; }
            try {
                String subDirectory = "reviews/" + review.getId();
                String storedFileName = fileStorageService.storeFile(file, subDirectory);
                ReviewMedia reviewMedia = ReviewMedia.builder().review(review).url(storedFileName).type("IMAGE").build();
                savedMediaList.add(reviewMediaRepository.save(reviewMedia));
                logger.debug("已保存評論 ID {} 的媒體文件記錄: {}", review.getId(), storedFileName);
                uploadedCount++;
            } catch (FileStorageException e) { logger.error("儲存評論 {} 的媒體文件 {} 失敗: {}", review.getId(), file.getOriginalFilename(), e.getMessage()); throw new RuntimeException("儲存評論照片失敗: " + file.getOriginalFilename(), e); }
        }
        if (uploadedCount < files.size() && uploadedCount < remainingSlots) logger.warn("...");
        return savedMediaList;
    }

    /** 內部方法：刪除指定評論關聯的所有物理媒體文件和 DB 記錄 */
    private void deleteReviewMediaFilesInternal(Review review) {
        if (review == null) return;
        List<ReviewMedia> mediaList = reviewMediaRepository.findByReviewId(review.getId());
        if (mediaList.isEmpty()) return;
        logger.warn("準備刪除評論 ID {} 的 {} 個媒體文件和記錄...", review.getId(), mediaList.size());
        int deletedFileCount = 0; List<Long> mediaIdsToDelete = new ArrayList<>();
        for (ReviewMedia media : mediaList) {
            mediaIdsToDelete.add(media.getId());
            if (StringUtils.hasText(media.getUrl())) {
                try { if (fileStorageService.deleteFile(media.getUrl())) deletedFileCount++; else logger.warn("..."); }
                catch (Exception e) { logger.error("..."); }
            } else { logger.warn("..."); }
        }
        if (!mediaIdsToDelete.isEmpty()) { reviewMediaRepository.deleteAllByIdInBatch(mediaIdsToDelete); logger.info("..."); }
        logger.info("評論 ID {} 的 {} 個媒體文件中，{} 個已嘗試刪除物理文件。", review.getId(), mediaList.size(), deletedFileCount);
    }

    /** 查找評論或拋出異常 */
    private Review findReviewByIdOrThrow(Long id) { return reviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id)); }
    /** 查找店家或拋出異常 */
    private Shop findShopByIdOrThrow(Long id) { return shopRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id)); }
    /** 獲取當前已認證的用戶實體 */
    private User getCurrentAuthenticatedUser() { Authentication a=SecurityContextHolder.getContext().getAuthentication(); if(a==null||!a.isAuthenticated()||a.getPrincipal()==null||"anonymousUser".equals(a.getPrincipal()))throw new UnauthorizedActionException("用戶未登錄或認證無效"); Object p=a.getPrincipal(); if(p instanceof User)return(User)p; if(p instanceof org.springframework.security.core.userdetails.UserDetails){String u=((org.springframework.security.core.userdetails.UserDetails)p).getUsername(); return userRepository.findByUsername(u).orElseThrow(()->new ResourceNotFoundException("User","username (from context)",u));} logger.error("無法識別的 Principal 類型: {}",p.getClass().getName()); throw new UnauthorizedActionException("無法識別的用戶信息類型"); }
    /** 檢查評論是否屬於當前用戶 */
    private void checkReviewOwnership(Review review, User currentUser) { if(review==null||review.getUser()==null)throw new IllegalArgumentException("評論或評論用戶不能為空"); if(currentUser==null)throw new UnauthorizedActionException("無法獲取當前用戶信息"); if(!Objects.equals(review.getUser().getId(),currentUser.getId())){logger.warn("...");throw new UnauthorizedActionException("您只能操作自己的評論");} }
    /** 檢查評論是否屬於當前用戶或當前用戶是否為管理員 */
    private void checkReviewOwnershipOrAdmin(Review review, User currentUser) { if(review==null)throw new IllegalArgumentException("評論對象不能為空"); if(currentUser==null)throw new UnauthorizedActionException("無法獲取當前用戶信息"); boolean iA=currentUser.getRole()==Role.ROLE_ADMIN; boolean iO=review.getUser()!=null&&Objects.equals(review.getUser().getId(),currentUser.getId()); if(!iA&&!iO){logger.warn("...");throw new UnauthorizedActionException("您沒有權限執行此操作");} }
    /** 計算父評論下的回覆數量 */
    private long countByParentReviewId(Long parentReviewId) { return reviewRepository.countByParentReviewId(parentReviewId); }
}