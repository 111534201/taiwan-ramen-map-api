package com.example.taiwanramenmapapi.controller;

import com.example.taiwanramenmapapi.dto.response.PageResponse;
import com.example.taiwanramenmapapi.dto.request.UpdateShopRequest;
import com.example.taiwanramenmapapi.dto.response.ApiResponse;
import com.example.taiwanramenmapapi.dto.response.ShopDTO;
import com.example.taiwanramenmapapi.service.ShopService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/shops")
@Validated
public class ShopController {

    private static final Logger logger = LoggerFactory.getLogger(ShopController.class);

    @Autowired
    private ShopService shopService;

    /**
     * GET /api/shops : 獲取店家列表 (分頁/篩選/排序) 或 根據地圖邊界獲取
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllShops(
            @RequestParam(value = "minLat", required = false) BigDecimal minLat,
            @RequestParam(value = "maxLat", required = false) BigDecimal maxLat,
            @RequestParam(value = "minLng", required = false) BigDecimal minLng,
            @RequestParam(value = "maxLng", required = false) BigDecimal maxLng,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "12") @Min(1) @Max(1000) int size, // 列表預設 12，地圖請求可更大
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "DESC") String sortDir,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam(value = "city", required = false) String city
    ) {
        logger.info("獲取店家請求: city=[{}], name=[{}], address=[{}], page={}, size={}, sort={},{}" ,
                city, name, address, page, size, sortBy, sortDir);
        boolean hasBounds = minLat != null && maxLat != null && minLng != null && maxLng != null;
        if (hasBounds) {
            List<ShopDTO> shops = shopService.getShopsByBounds(minLat, maxLat, minLng, maxLng);
            return ResponseEntity.ok(ApiResponse.success(shops, "獲取範圍內店家成功"));
        } else {
            List<String> allowedSortBy = List.of("id", "name", "address", "averageRating", "reviewCount", "weightedRating", "createdAt", "updatedAt");
            if (!allowedSortBy.contains(sortBy)) { sortBy = "createdAt"; }
            Sort.Direction direction = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
            PageResponse<ShopDTO> shops = shopService.getAllShops(pageable, name, address, city);
            return ResponseEntity.ok(ApiResponse.success(shops));
        }
    }

    /**
     * GET /api/shops/{id} : 根據 ID 獲取單個店家詳情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShopDTO>> getShopById(@PathVariable Long id) {
        logger.info("獲取店家詳情請求 ID: {}", id);
        ShopDTO shop = shopService.getShopById(id);
        return ResponseEntity.ok(ApiResponse.success(shop));
    }

    /**
     * PUT /api/shops/{id} : 更新店家資訊 (店家本人或管理員)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_OWNER')")
    public ResponseEntity<ApiResponse<ShopDTO>> updateShop(@PathVariable Long id, @Valid @RequestBody UpdateShopRequest updateShopRequest) {
        logger.info("更新店家請求 ID: {}", id);
        ShopDTO updatedShop = shopService.updateShop(id, updateShopRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedShop, "店家更新成功"));
    }

    /**
     * DELETE /api/shops/{id} : 刪除店家 (僅限管理員)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteShop(@PathVariable Long id) {
        logger.warn("管理員刪除店家請求 ID: {}", id);
        shopService.deleteShopByAdmin(id);
        return ResponseEntity.ok(ApiResponse.success("店家刪除成功"));
    }

    /**
     * GET /api/shops/top : 獲取 Top N 排行榜店家
     */
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<ShopDTO>>> getTopShops(
            @RequestParam(value = "limit", defaultValue = "10") @Min(1) @Max(50) int limit,
            @RequestParam(value = "region", required = false) String region
    ) {
        if (StringUtils.hasText(region)) {
            logger.info("獲取 Top {} 店家請求 (區域: {})", limit, region);
            List<ShopDTO> topShops = shopService.getTopRatedShopsByRegion(region, limit);
            return ResponseEntity.ok(ApiResponse.success(topShops, "Top " + limit + " 店家排行榜 (區域: " + region + ")"));
        } else {
            logger.info("獲取 Top {} 店家請求 (全局)", limit);
            List<ShopDTO> topShops = shopService.getTopRatedShops(limit);
            return ResponseEntity.ok(ApiResponse.success(topShops, "Top " + limit + " 店家排行榜 (全台灣)"));
        }
    }

    /**
     * POST /api/shops/{shopId}/media : 為指定店家上傳媒體文件
     */
    @PostMapping(value = "/{shopId}/media", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_OWNER')")
    public ResponseEntity<ApiResponse<ShopDTO>> uploadShopMedia(
            @PathVariable Long shopId,
            @RequestParam("files") List<MultipartFile> files) {
        logger.info("為店家 ID {} 上傳 {} 個媒體文件", shopId, files != null ? files.size() : 0);
        if (files == null || files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty)) {
            return ResponseEntity.badRequest().body(ApiResponse.failure("未選擇任何文件進行上傳"));
        }
        ShopDTO updatedShop = shopService.uploadShopMedia(shopId, files);
        return ResponseEntity.ok(ApiResponse.success(updatedShop, "店家媒體上傳成功"));
    }

    /**
     * DELETE /api/shops/{shopId}/media/{mediaId} : 刪除指定的店家媒體文件
     */
    @DeleteMapping("/{shopId}/media/{mediaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_OWNER')")
    public ResponseEntity<ApiResponse<Void>> deleteShopMedia(
            @PathVariable Long shopId,
            @PathVariable Long mediaId) {
        logger.warn("刪除店家 ID {} 的媒體 ID {}", shopId, mediaId);
        shopService.deleteShopMedia(shopId, mediaId);
        return ResponseEntity.ok(ApiResponse.success("店家媒體刪除成功"));
    }
}