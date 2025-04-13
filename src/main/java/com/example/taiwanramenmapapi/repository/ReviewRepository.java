package com.example.taiwanramenmapapi.repository;

import com.example.taiwanramenmapapi.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // 可選，用於複雜查詢
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // 引入 Optional

@Repository
// 繼承 JpaRepository 提供 CRUD
// 繼承 JpaSpecificationExecutor 提供動態查詢能力 (可選)
public interface ReviewRepository extends JpaRepository<Review, Long>, JpaSpecificationExecutor<Review> {

    /**
     * 查找特定店家的頂級評論 (即 parentReview 為 null 的評論)。
     * 支持分頁和排序。
     * @param shopId 店家 ID
     * @param pageable 分頁和排序信息
     * @return 頂級評論的分頁數據
     */
    Page<Review> findByShopIdAndParentReviewIsNull(Long shopId, Pageable pageable);

    /**
     * 查找特定父評論的所有直接回覆。
     * 通常按創建時間升序排列。
     * @param parentReviewId 父評論 ID
     * @return 回覆列表
     */
    List<Review> findByParentReviewIdOrderByCreatedAtAsc(Long parentReviewId);

    /**
     * 查找特定用戶發表的所有評論 (包括頂級評論和回覆)。
     * 支持分頁和排序。可用於用戶個人頁面等。
     * @param userId 用戶 ID
     * @param pageable 分頁和排序信息
     * @return 該用戶評論的分頁數據
     */
    Page<Review> findByUserId(Long userId, Pageable pageable);

    /**
     * 計算特定店家的頂級評論總數。
     * 用於更新 Shop 實體的 reviewCount。
     * @param shopId 店家 ID
     * @return 頂級評論的數量
     */
    long countByShopIdAndParentReviewIdIsNull(Long shopId);

    /**
     * 計算特定父評論下的回覆總數。
     * 用於更新 Review 實體的 replyCount。
     * @param parentReviewId 父評論 ID
     * @return 回覆的數量
     */
    long countByParentReviewId(Long parentReviewId);

    /**
     * 獲取特定店家所有頂級評論的評分列表。
     * 用於計算店家的平均評分。
     * @param shopId 店家 ID
     * @return 評分整數列表
     */
    @Query("SELECT r.rating FROM Review r WHERE r.shop.id = :shopId AND r.parentReview IS NULL AND r.rating IS NOT NULL") // 確保評分不為 null
    List<Integer> findAllTopLevelRatingsByShopId(@Param("shopId") Long shopId);

    /**
     * 查找特定用戶對特定店家發表的頂級評論。
     * 可用於判斷用戶是否已經評論過某店家（如果業務邏輯限制只能評論一次）。
     * @param userId 用戶 ID
     * @param shopId 店家 ID
     * @return 包含評論的 Optional，如果找到則有值
     */
    Optional<Review> findByUserIdAndShopIdAndParentReviewIsNull(Long userId, Long shopId);

    /**
     * 檢查特定用戶是否已對特定店家發表頂級評論。
     * 效率比 findByUserIdAndShopIdAndParentReviewIsNull 更高，如果只需要知道是否存在。
     * @param userId 用戶 ID
     * @param shopId 店家 ID
     * @return 如果存在返回 true，否則返回 false
     */
    boolean existsByUserIdAndShopIdAndParentReviewIsNull(Long userId, Long shopId);


    /**
     * 根據評論 ID 和用戶 ID 查找評論。
     * 主要用於驗證用戶是否有權限編輯或刪除自己的評論。
     * @param id 評論 ID
     * @param userId 用戶 ID
     * @return 包含評論的 Optional，如果找到且匹配則有值
     */
    Optional<Review> findByIdAndUserId(Long id, Long userId);

    /**
     * 根據店家 ID 查找所有評論 (包括頂級評論和所有回覆)。
     * 用於刪除店家前，需要找到所有關聯評論以清理其媒體文件。
     * @param shopId 店家 ID
     * @return 屬於該店家的所有評論列表
     */
    List<Review> findByShopId(Long shopId);

}