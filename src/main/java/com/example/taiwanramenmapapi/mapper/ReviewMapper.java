package com.example.taiwanramenmapapi.mapper;

import com.example.taiwanramenmapapi.dto.response.ReviewDTO;
import com.example.taiwanramenmapapi.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import java.util.List; // 引入 List

/**
 * Mapper 介面，負責 Review 實體和 ReviewDTO 之間的轉換。
 * 使用 MapStruct 自動生成實現。
 */
@Mapper(
        componentModel = "spring", // 生成 Spring Bean
        uses = { UserMapper.class, ReviewMediaMapper.class } // 依賴其他 Mapper 進行關聯對象的轉換
)
public interface ReviewMapper {

    /**
     * 將 Review 實體轉換為 ReviewDTO。
     * @param review Review 實體對象。
     * @return 對應的 ReviewDTO 對象。
     */
    @Mappings({
            // --- 關聯對象映射 ---
            @Mapping(source = "user", target = "user"),             // 使用 UserMapper 將 User 轉為 ReviewerDTO
            @Mapping(source = "shop.id", target = "shopId"),         // 從關聯的 Shop 獲取 ID
            @Mapping(source = "parentReview.id", target = "parentReviewId"), // 從關聯的父評論獲取 ID
            @Mapping(source = "media", target = "media"),            // 使用 ReviewMediaMapper 將 List<ReviewMedia> 轉為 List<ReviewMediaDTO>

            // --- 強制 Content 直接映射 ---
            // 使用 expression 強制調用 review.getContent()，避免 MapStruct 錯誤調用其他方法
            @Mapping(target = "content", expression = "java(review.getContent())"),

            // --- 其他同名基礎類型字段會自動映射 ---
            // @Mapping(source = "id", target = "id"), // 自動
            // @Mapping(source = "rating", target = "rating"), // 自動
            // @Mapping(source = "replyCount", target = "replyCount"), // 自動
            // @Mapping(source = "createdAt", target = "createdAt"), // 自動
            // @Mapping(source = "updatedAt", target = "updatedAt") // 自動
    })
    ReviewDTO toReviewDTO(Review review);

    /**
     * 將 Review 實體列表轉換為 ReviewDTO 列表。
     * MapStruct 會自動調用上面的 toReviewDTO 方法進行單個元素的轉換。
     * @param reviews Review 實體列表。
     * @return 對應的 ReviewDTO 列表。
     */
    List<ReviewDTO> toReviewDTOs(List<Review> reviews);
}