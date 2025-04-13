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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
    private final ShopService shopService;
    private final FileStorageService fileStorageService;

    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository, ShopRepository shopRepository, UserRepository userRepository, ReviewMediaRepository reviewMediaRepository, ReviewMapper reviewMapper, ShopService shopService, FileStorageService fileStorageService) {
        this.reviewRepository = reviewRepository;
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.reviewMediaRepository = reviewMediaRepository;
        this.reviewMapper = reviewMapper;
        this.shopService = shopService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public ReviewDTO createReview(Long shopId, CreateReviewRequest request, List<MultipartFile> files) {
        logger.info("Service: 嘗試為店家 ID {} 創建評論/回覆", shopId);
        User currentUser = getCurrentAuthenticatedUser();
        Shop shop = findShopByIdOrThrow(shopId);
        if (currentUser.getRole() == Role.ROLE_SHOP_OWNER && shop.getOwner() != null && Objects.equals(shop.getOwner().getId(), currentUser.getId())) { throw new UnauthorizedActionException("店家不能評論自己的店鋪"); }
        Review parentReview = null;
        if (request.getParentReviewId() != null) {
            parentReview = findReviewByIdOrThrow(request.getParentReviewId());
            if (!Objects.equals(parentReview.getShop().getId(), shopId)) { throw new IllegalArgumentException("回覆的父評論不屬於指定的店家"); }
            if (parentReview.getParentReview() != null) { throw new BadRequestException("不能回覆一個回覆"); }
            if (request.getRating() != null) { logger.warn("回覆評論時提供的評分被忽略 (Parent ID: {})", request.getParentReviewId()); }
            logger.info("創建對評論 ID {} 的回覆 by User {}", request.getParentReviewId(), currentUser.getUsername());
        } else {
            logger.info("創建新的頂級評論 by User {}", currentUser.getUsername());
            if (request.getRating() == null) { throw new BadRequestException("頂級評論必須提供評分 (1-5)"); }
            if(request.getRating() < 1 || request.getRating() > 5) { throw new BadRequestException("評分必須在 1 到 5 之間"); }
            if (!StringUtils.hasText(request.getContent())) { throw new BadRequestException("評論內容不能為空"); }
        }
        Review review = Review.builder()
                .rating(parentReview == null ? request.getRating() : null)
                .content(request.getContent()) // 保存文本內容
                .user(currentUser)
                .shop(shop)
                .parentReview(parentReview)
                .replyCount(0)
                .media(new ArrayList<>())
                .build();
        Review savedReview;
        try {
            savedReview = reviewRepository.save(review);
            logger.info("評論/回覆基礎信息 (ID: {}) 已保存", savedReview.getId());
        } catch (Exception e) { logger.error("保存評論/回覆基礎信息失敗: {}", e.getMessage(), e); throw new RuntimeException("保存評論失敗", e); }
        List<ReviewMedia> savedMedia = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            logger.info("正在處理評論 {} 的 {} 張照片...", savedReview.getId(), files.size());
            try { savedMedia = uploadReviewMediaInternal(savedReview, files); }
            catch (RuntimeException e) { logger.error("儲存評論 {} 的照片失敗，事務將回滾: {}", savedReview.getId(), e.getMessage(), e); throw e; }
        }
        if (parentReview != null) {
            try { long newReplyCount = countByParentReviewId(parentReview.getId()); parentReview.setReplyCount((int)newReplyCount); reviewRepository.save(parentReview); logger.debug("父評論 ID {} 的回覆數已更新為 {}", parentReview.getId(), newReplyCount); }
            catch (Exception e) { logger.error("更新父評論 {} 回覆數失敗: {}", parentReview.getId(), e.getMessage(), e); }
        }
        if (parentReview == null) {
            try { shopService.updateShopRating(shopId); } // 恢復調用
            catch (Exception e) { logger.error("創建評論後更新店家 {} 評分失敗: {}", shopId, e.getMessage(), e); }
        }
        logger.info("評論/回覆 (ID: {}) 完整創建成功", savedReview.getId());
        Review finalReview = findReviewByIdOrThrow(savedReview.getId());
        return reviewMapper.toReviewDTO(finalReview);
    }


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
        if (StringUtils.hasText(request.getContent()) && !Objects.equals(oldContent, request.getContent())) {
            logger.info("[UPDATE] 內容已更改，設置新內容 for review ID {}", reviewId);
            review.setContent(request.getContent());
            changed = true;
        } else if (request.getContent() != null && !StringUtils.hasText(request.getContent())) {
            if (StringUtils.hasText(oldContent)) {
                logger.info("[UPDATE] 內容被清空，設置新內容 for review ID {}", reviewId);
                review.setContent(""); // 或 null
                changed = true;
            } else {
                logger.debug("[UPDATE] 內容未提供或與空內容相同，不更新。", reviewId);
            }
        } else {
            logger.debug("[UPDATE] 內容未提供或未更改，不更新。", reviewId);
        }

        // 2. 更新評分 (僅限頂級評論)
        if (review.getParentReview() == null && request.getRating() != null) {
            if(request.getRating() < 1 || request.getRating() > 5) { throw new BadRequestException("評分必須在 1 到 5 之間"); }
            if (!Objects.equals(oldRating, request.getRating())) {
                logger.info("[UPDATE] 評分已更改，設置新評分 for review ID {}", reviewId);
                review.setRating(request.getRating());
                changed = true;
            } else {
                logger.debug("[UPDATE] 評分未提供或與之前相同，不更新。", reviewId);
            }
        } else if (review.getParentReview() != null && request.getRating() != null) {
            logger.warn("[UPDATE] 嘗試更新回覆的評分 (ID: {})，操作被忽略。", reviewId);
        }

        // 3. 只有在確實有更改時才執行保存
        if (changed) {
            logger.info("[UPDATE] 檢測到更改，執行保存操作 for review ID {}", reviewId);
            try {
                review = reviewRepository.save(review);
                reviewRepository.flush(); // 強制同步
                logger.info("[UPDATE] 評論/回覆 (ID: {}) 已成功更新到數據庫 (已 flush)", review.getId());

                Review persistedReview = findReviewByIdOrThrow(reviewId); // **重新獲取**
                logger.info("[UPDATE] 從數據庫重新獲取: content='{}', rating={}", persistedReview.getContent(), persistedReview.getRating());

                boolean actualRatingChanged = persistedReview.getParentReview() == null &&
                        request.getRating() != null &&
                        !Objects.equals(oldRating, persistedReview.getRating());

                if (actualRatingChanged) {
                    logger.info("[UPDATE] 評分已實際更改，觸發店家評分更新 for shop ID {}", persistedReview.getShop().getId());
                    try {
                        shopService.updateShopRating(persistedReview.getShop().getId()); // **恢復調用**
                    } catch (Exception e) {
                        logger.error("[UPDATE] 更新評論後更新店家 {} 評分失敗: {}", persistedReview.getShop().getId(), e.getMessage(), e);
                        // 考慮向上拋出異常以回滾事務
                        // throw new RuntimeException("更新店家評分失敗導致評論更新回滾", e);
                    }
                } else {
                    logger.debug("[UPDATE] 評分未實際更改，不觸發店家評分更新。");
                }
                logger.debug("[UPDATE] 更新操作成功，返回 DTO，content: {}", persistedReview.getContent());
                return reviewMapper.toReviewDTO(persistedReview); // 返回更新後的 DTO

            } catch (Exception e) {
                logger.error("[UPDATE] 保存更新後的評論 ID {} 失敗: {}", reviewId, e.getMessage(), e);
                throw new RuntimeException("更新評論時保存失敗", e);
            }
        } else {
            logger.info("[UPDATE] 評論/回覆 (ID: {}) 無需更新。", reviewId);
            logger.debug("[UPDATE] 無更改，返回 DTO，content: {}", review.getContent());
            return reviewMapper.toReviewDTO(review); // 返回未修改的 DTO
        }
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        logger.warn("Service: 嘗試刪除評論/回覆 ID: {}", reviewId);
        Review review = findReviewByIdOrThrow(reviewId);
        User currentUser = getCurrentAuthenticatedUser();
        checkReviewOwnershipOrAdmin(review, currentUser);
        Long shopId = review.getShop().getId();
        boolean isTopLevel = review.getParentReview() == null;
        Long parentReviewId = review.getParentReview() != null ? review.getParentReview().getId() : null;
        deleteReviewMediaFilesInternal(review);
        reviewMediaRepository.deleteAll(reviewMediaRepository.findByReviewId(reviewId));
        reviewRepository.delete(review);
        logger.info("評論/回覆 (ID: {}) 已成功從數據庫刪除", reviewId);
        if (parentReviewId != null) {
            reviewRepository.findById(parentReviewId).ifPresent(pReview -> {
                long newReplyCount = countByParentReviewId(pReview.getId());
                pReview.setReplyCount((int)newReplyCount);
                reviewRepository.save(pReview);
                logger.debug("父評論 ID {} 的回覆數已更新為 {}", pReview.getId(), newReplyCount);
            });
        }
        if (isTopLevel && review.getRating() != null) {
            try { shopService.updateShopRating(shopId); } // 恢復調用
            catch (Exception e) { logger.error("刪除評論後更新店家 {} 評分失敗: {}", shopId, e.getMessage(), e); }
        }
    }

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

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDTO> getReviewReplies(Long parentReviewId) {
        logger.debug("Service: 獲取評論 ID {} 的回覆", parentReviewId);
        if (!reviewRepository.existsById(parentReviewId)) { throw new ResourceNotFoundException("Review", "id", parentReviewId); }
        List<Review> replies = reviewRepository.findByParentReviewIdOrderByCreatedAtAsc(parentReviewId);
        logger.debug("Service: 為評論 ID {} 找到 {} 條回覆", parentReviewId, replies.size());
        return replies.stream().map(reviewMapper::toReviewDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReviewMedia(Long reviewId, Long mediaId) {
        logger.warn("Service: 嘗試刪除評論 ID {} 的媒體 ID {}", reviewId, mediaId);
        Review review = findReviewByIdOrThrow(reviewId);
        User currentUser = getCurrentAuthenticatedUser();
        checkReviewOwnershipOrAdmin(review, currentUser);
        ReviewMedia media = reviewMediaRepository.findByIdAndReviewId(mediaId, reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewMedia", "id/reviewId", mediaId + "/" + reviewId));
        try {
            if (fileStorageService.deleteFile(media.getUrl())) { logger.info("成功刪除物理文件: {}", media.getUrl()); }
            else { logger.warn("物理文件 {} 可能不存在或刪除失敗。", media.getUrl()); }
            reviewMediaRepository.delete(media);
            logger.info("Service: 評論 ID {} 的媒體 ID {} 記錄已成功刪除", reviewId, mediaId);
        } catch (Exception e) {
            logger.error("刪除評論媒體文件或記錄時出錯 (Media ID: {}): {}", mediaId, e.getMessage(), e);
            throw new RuntimeException("刪除評論媒體失敗", e);
        }
    }

    // --- 私有輔助方法 ---
    private List<ReviewMedia> uploadReviewMediaInternal(Review review, List<MultipartFile> files) {
        List<ReviewMedia> savedMediaList = new ArrayList<>();
        if (files == null || files.isEmpty()) { return savedMediaList; }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String contentType = file.getContentType();
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (contentType == null || !contentType.startsWith("image/")) { logger.warn("評論 ID {} 上傳了非圖片文件 ({})，已跳過。", review.getId(), contentType); continue; }
            if (file.getSize() > maxSize) { logger.warn("評論 ID {} 上傳的文件 {} 大小超過限制 ({} > {})", review.getId(), file.getOriginalFilename(), file.getSize(), maxSize); continue; }
            try {
                String storedFileName = fileStorageService.storeFile(file, "reviews/" + review.getId());
                ReviewMedia reviewMedia = ReviewMedia.builder().review(review).url(storedFileName).type("IMAGE").build();
                savedMediaList.add(reviewMediaRepository.save(reviewMedia));
                logger.debug("已保存評論 ID {} 的媒體文件記錄: {}", review.getId(), storedFileName);
            } catch (FileStorageException e) { logger.error("儲存評論 {} 的媒體文件 {} 失敗: {}", review.getId(), file.getOriginalFilename(), e.getMessage()); throw new RuntimeException("儲存評論照片失敗: " + file.getOriginalFilename(), e); }
        }
        return savedMediaList;
    }

    private void deleteReviewMediaFilesInternal(Review review) {
        if (review == null) return;
        List<ReviewMedia> mediaList = reviewMediaRepository.findByReviewId(review.getId());
        if (mediaList.isEmpty()) { logger.debug("評論 ID {} 沒有關聯的媒體文件需要刪除。", review.getId()); return; }
        logger.warn("準備刪除評論 ID {} 的 {} 個媒體文件...", review.getId(), mediaList.size());
        int deletedCount = 0;
        for (ReviewMedia media : mediaList) {
            try { if (fileStorageService.deleteFile(media.getUrl())) { deletedCount++; logger.info("成功刪除物理文件: {}", media.getUrl()); } else { logger.warn("物理文件 {} 可能不存在或刪除失敗。", media.getUrl()); } }
            catch (Exception e) { logger.error("刪除評論媒體文件失敗 (Review ID: {}, Media ID: {}, File: {}): {}", review.getId(), media.getId(), media.getUrl(), e.getMessage()); }
        }
        logger.info("評論 ID {} 的 {} 個媒體文件中的 {} 個已嘗試刪除物理文件。", review.getId(), mediaList.size(), deletedCount);
    }

    private Review findReviewByIdOrThrow(Long id) {
        return reviewRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
    }

    private Shop findShopByIdOrThrow(Long id) {
        return shopRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));
    }

    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal())) { throw new UnauthorizedActionException("用戶未登錄或認證無效"); }
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", "username (from context)", username));
        } else if (principal instanceof User) {
            User principalUser = (User) principal;
            return userRepository.findById(principalUser.getId()).orElseThrow(() -> new ResourceNotFoundException("User", "id (from context)", principalUser.getId()));
        }
        logger.error("無法識別的 Principal 類型: {}", principal.getClass().getName());
        throw new UnauthorizedActionException("無法識別的用戶信息類型");
    }

    private void checkReviewOwnership(Review review, User currentUser) {
        if (review == null || review.getUser() == null) { throw new IllegalArgumentException("評論或評論用戶不能為空"); }
        if (currentUser == null) { throw new UnauthorizedActionException("無法獲取當前用戶信息"); }
        if (!Objects.equals(String.valueOf(review.getUser().getId()), String.valueOf(currentUser.getId()))) { logger.warn("用戶 {} (ID: {}) 嘗試操作不屬於自己的評論 ID {} (作者 ID: {})", currentUser.getUsername(), currentUser.getId(), review.getId(), review.getUser().getId()); throw new UnauthorizedActionException("您只能操作自己的評論"); }
    }

    private void checkReviewOwnershipOrAdmin(Review review, User currentUser) {
        if (review == null) { throw new IllegalArgumentException("評論對象不能為空"); }
        if (currentUser == null) { throw new UnauthorizedActionException("無法獲取當前用戶信息"); }
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
        boolean isOwner = review.getUser() != null && Objects.equals(String.valueOf(review.getUser().getId()), String.valueOf(currentUser.getId()));
        if (!isAdmin && !isOwner) { logger.warn("用戶 {} (ID: {}, 角色: {}) 嘗試操作不屬於自己且非管理員權限的評論 ID {}", currentUser.getUsername(), currentUser.getId(), currentUser.getRole(), review.getId()); throw new UnauthorizedActionException("您沒有權限執行此操作"); }
    }

    private long countByParentReviewId(Long parentReviewId) {
        return reviewRepository.countByParentReviewId(parentReviewId);
    }

    // --- 計算平均評分 (保持不變) ---
    private BigDecimal calculateAverageRating(List<Integer> ratings) {
        if (ratings == null || ratings.isEmpty()) { return BigDecimal.ZERO; }
        double sum = ratings.stream().mapToInt(Integer::intValue).sum();
        return BigDecimal.valueOf(sum / ratings.size()).setScale(2, RoundingMode.HALF_UP);
    }

    // --- 計算加權評分 (保持不變) ---
    private BigDecimal calculateWeightedRating(BigDecimal averageRating, int reviewCount) {
        final double C = 5.0; // 先驗評論數
        final double M = 3.5; // 先驗平均分
        double avg = averageRating.doubleValue();
        if (C + reviewCount == 0) return BigDecimal.valueOf(M).setScale(8, RoundingMode.HALF_UP);
        double weighted = (C * M + avg * reviewCount) / (C + reviewCount);
        return BigDecimal.valueOf(weighted).setScale(8, RoundingMode.HALF_UP);
    }
}