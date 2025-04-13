package com.example.taiwanramenmapapi.mapper;

import com.example.taiwanramenmapapi.dto.response.ReviewDTO;
import com.example.taiwanramenmapapi.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import java.util.List; // 引入 List

// 告訴 MapStruct 這個 Mapper 需要用到 UserMapper 和 ReviewMediaMapper
@Mapper(componentModel = "spring", uses = { UserMapper.class, ReviewMediaMapper.class })
public interface ReviewMapper {

    // 使用 @Mappings 組合多個 @Mapping 規則
    @Mappings({
            // source = "實體中的字段名", target = "DTO 中的字段名"
            @Mapping(source = "user", target = "user"),             // User -> ReviewerDTO (由 UserMapper 處理)
            @Mapping(source = "shop.id", target = "shopId"),         // 從關聯的 Shop 實體獲取 ID
            @Mapping(source = "parentReview.id", target = "parentReviewId"), // 從關聯的父評論獲取 ID
            @Mapping(source = "media", target = "media")            // List<ReviewMedia> -> List<ReviewMediaDTO> (由 ReviewMediaMapper 處理)
            // replyCount 字段名稱相同，會自動映射
            // createdAt, updatedAt 字段名稱相同，會自動映射
            // rating, content 字段名稱相同，會自動映射
            // id 字段名稱相同，會自動映射
    })
    ReviewDTO toReviewDTO(Review review);

    // MapStruct 會自動為 List 生成轉換方法
    List<ReviewDTO> toReviewDTOs(List<Review> reviews);
}