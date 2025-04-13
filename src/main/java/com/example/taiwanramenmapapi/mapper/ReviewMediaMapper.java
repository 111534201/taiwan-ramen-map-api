package com.example.taiwanramenmapapi.mapper;

import com.example.taiwanramenmapapi.dto.response.ReviewMediaDTO;
import com.example.taiwanramenmapapi.entity.ReviewMedia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils; // 引入

import java.util.List;

@Mapper(componentModel = "spring")
@Component
public abstract class ReviewMediaMapper {

    @Value("${app.base-url:http://localhost:8080}")
    protected String baseUrl;

    @Value("${app.upload.url-path:/uploads/}")
    protected String uploadUrlPathPattern;

    @Mapping(target = "url", expression = "java(createFullUrl(reviewMedia.getUrl()))")
    public abstract ReviewMediaDTO toReviewMediaDTO(ReviewMedia reviewMedia);

    public abstract List<ReviewMediaDTO> toReviewMediaDTOs(List<ReviewMedia> reviewMediaList);

    // 輔助方法：生成完整的媒體訪問 URL (與 ShopMediaMapper 相同)
    protected String createFullUrl(String relativeOrAbsoluteUrl) {
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