package com.example.taiwanramenmapapi.mapper;

import com.example.taiwanramenmapapi.dto.response.ReviewMediaDTO;
import com.example.taiwanramenmapapi.entity.ReviewMedia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping; // 引入 Mapping
import org.mapstruct.Mappings; // 引入 Mappings

import java.util.List;

@Mapper(componentModel = "spring") // 只保留 componentModel
// 不再需要 @Component 或 abstract class，因為沒有注入和輔助方法了
public interface ReviewMediaMapper { // 改回 interface

    // --- 移除 @Value 注入 ---
    // @Value(...) protected String baseUrl;
    // @Value(...) protected String uploadUrlPathPattern;

    /**
     * 將 ReviewMedia 實體轉換為 ReviewMediaDTO。
     * 直接映射 url 和 type，不再拼接完整 URL。
     * @param reviewMedia ReviewMedia 實體對象。
     * @return 對應的 ReviewMediaDTO 對象。
     */
    @Mappings({
            // *** 確保直接映射 url ***
            @Mapping(source = "url", target = "url"),
            // *** 確保直接映射 type ***
            @Mapping(source = "type", target = "type")
            // id 會自動映射
    })
    ReviewMediaDTO toReviewMediaDTO(ReviewMedia reviewMedia); // 改為普通方法

    /**
     * 將 ReviewMedia 實體列表轉換為 ReviewMediaDTO 列表。
     * @param reviewMediaList ReviewMedia 實體列表。
     * @return 對應的 ReviewMediaDTO 列表。
     */
    List<ReviewMediaDTO> toReviewMediaDTOs(List<ReviewMedia> reviewMediaList); // 改為普通方法


}