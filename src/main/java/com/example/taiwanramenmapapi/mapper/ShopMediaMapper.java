package com.example.taiwanramenmapapi.mapper;

import com.example.taiwanramenmapapi.dto.response.ShopMediaDTO;
import com.example.taiwanramenmapapi.entity.ShopMedia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings; // *** 引入 Mappings ***
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Mapper(componentModel = "spring")
@Component
public abstract class ShopMediaMapper {

    @Value("${app.base-url:http://localhost:8080}")
    protected String baseUrl;

    @Value("${app.upload.url-path:/uploads/}")
    protected String uploadUrlPathPattern;

    // --- 轉換方法 ---
    // *** 使用 @Mappings 包裹多個 @Mapping ***
    @Mappings({
            @Mapping(target = "url", expression = "java(createFullUrl(shopMedia.getUrl()))"), // 處理 url
            @Mapping(source = "type", target = "type") // *** 明確添加 type 的映射 ***
            // MapStruct 會自動映射 id 和 displayOrder (如果名稱相同)
    })
    public abstract ShopMediaDTO toShopMediaDTO(ShopMedia shopMedia);
    // *** --- ***

    public abstract List<ShopMediaDTO> toShopMediaDTOs(List<ShopMedia> shopMediaList);

    // --- 輔助方法：生成完整的媒體訪問 URL ---
    protected String createFullUrl(String relativeOrAbsoluteUrl) {
        // ... (方法內容不變) ...
        if (!StringUtils.hasText(relativeOrAbsoluteUrl)) { return null; }
        if (relativeOrAbsoluteUrl.startsWith("http://") || relativeOrAbsoluteUrl.startsWith("https://")) { return relativeOrAbsoluteUrl; }
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String cleanUploadPath = uploadUrlPathPattern.replace("/**", "");
        if (!cleanUploadPath.startsWith("/")) { cleanUploadPath = "/" + cleanUploadPath; }
        if (!cleanUploadPath.endsWith("/")) { cleanUploadPath += "/"; }
        String cleanRelativePath = relativeOrAbsoluteUrl.startsWith("/") ? relativeOrAbsoluteUrl.substring(1) : relativeOrAbsoluteUrl;
        return cleanBaseUrl + cleanUploadPath + cleanRelativePath;
    }
}