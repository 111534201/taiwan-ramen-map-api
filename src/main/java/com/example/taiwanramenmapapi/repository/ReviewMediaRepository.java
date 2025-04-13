// src/main/java/com/example/taiwanramenmapapi/repository/ReviewMediaRepository.java
package com.example.taiwanramenmapapi.repository;

import com.example.taiwanramenmapapi.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // 建議添加 @Repository 註解

import java.util.List;
import java.util.Optional; // 引入 Optional

@Repository // 標記這是一個 Spring 管理的 Repository Bean
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {

    /**
     * 根據評論 ID 查找所有關聯的媒體文件。
     * @param reviewId 評論 ID
     * @return 媒體文件列表
     */
    List<ReviewMedia> findByReviewId(Long reviewId);

    /**
     * 根據媒體自身的 ID 和它所屬的評論 ID 查找媒體文件。
     * Spring Data JPA 會根據方法名自動生成查詢。
     * @param id 媒體 ID
     * @param reviewId 評論 ID
     * @return 包含找到的 ReviewMedia 的 Optional，如果找不到則為空
     */
    Optional<ReviewMedia> findByIdAndReviewId(Long id, Long reviewId); // <--- 添加的方法

    // 可選：如果需要在刪除評論時批量刪除媒體記錄（未使用級聯刪除時）
    // @Transactional // 如果需要事務支持
    // void deleteAllByReviewId(Long reviewId);
}