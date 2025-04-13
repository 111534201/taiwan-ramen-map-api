package com.example.taiwanramenmapapi.repository;

import com.example.taiwanramenmapapi.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // 建議加上 @Repository 註解

import java.util.List;
import java.util.Optional;

@Repository // 加上 @Repository 是一個好的實踐，雖然對於 JpaRepository 不是嚴格必需的
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {

    /**
     * 根據評論 ID 查找所有關聯的媒體。
     * Spring Data JPA 會自動生成實現。
     * @param reviewId 評論的 ID
     * @return 關聯的媒體列表
     */
    List<ReviewMedia> findByReviewId(Long reviewId);

    /**
     * 根據媒體 ID 和評論 ID 查找特定的媒體記錄。
     * 用於驗證媒體是否屬於該評論。
     * @param id 媒體的 ID
     * @param reviewId 評論的 ID
     * @return 包含找到的媒體的 Optional，如果未找到則為空
     */
    Optional<ReviewMedia> findByIdAndReviewId(Long id, Long reviewId);

    /**
     * 根據評論 ID 統計關聯的媒體數量。
     * Spring Data JPA 會根據方法名稱自動生成 SQL count 查詢。
     * @param reviewId 評論的 ID
     * @return 該評論關聯的媒體數量
     */
    long countByReviewId(Long reviewId); // <--- 這就是新加的方法定義

    // 你可以在這裡添加其他需要的自定義查詢方法...
}