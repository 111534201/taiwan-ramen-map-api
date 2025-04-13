package com.example.taiwanramenmapapi.repository;

import com.example.taiwanramenmapapi.entity.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {

    // 根據 Review ID 查找所有媒體
    List<ReviewMedia> findByReviewId(Long reviewId);

    // 根據 Review ID 刪除所有媒體
    // void deleteByReviewId(Long reviewId);
}