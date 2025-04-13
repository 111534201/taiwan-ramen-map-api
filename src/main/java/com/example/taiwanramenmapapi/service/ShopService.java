package com.example.taiwanramenmapapi.service;

import com.example.taiwanramenmapapi.dto.response.PageResponse;
import com.example.taiwanramenmapapi.dto.request.CreateShopRequest; // 引入
import com.example.taiwanramenmapapi.dto.request.ShopOwnerSignUpRequest; // 引入
import com.example.taiwanramenmapapi.dto.request.UpdateShopRequest;
import com.example.taiwanramenmapapi.dto.response.ShopDTO;
import com.example.taiwanramenmapapi.entity.User; // 引入
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile; // 引入

import java.math.BigDecimal;
import java.util.List;

public interface ShopService {

    // --- 給管理員或內部調用的創建方法 ---
    // ShopDTO createShop(CreateShopRequest createShopRequest); // 這個可能不再直接被外部調用

    // --- 給 AuthService 調用的，在店家註冊時創建店家 ---
    ShopDTO createShopWithOwner(ShopOwnerSignUpRequest request, User owner);

    ShopDTO getShopById(Long id);

    PageResponse<ShopDTO> getAllShops(Pageable pageable, String name, String address, String city);

    ShopDTO updateShop(Long id, UpdateShopRequest updateShopRequest);

    // --- 店家不能刪除，只有管理員可以 ---
    void deleteShopByAdmin(Long id);

    // --- 更新店家評分 (內部調用) ---
    void updateShopRating(Long shopId);

    // --- 排行榜 ---
    List<ShopDTO> getTopRatedShops(int limit);
    List<ShopDTO> getTopRatedShopsByRegion(String region, int limit);

    // --- 地圖邊界查詢 ---
    List<ShopDTO> getShopsByBounds(BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng);

    // --- 媒體處理 ---
    /**
     * 為指定店家上傳媒體文件 (照片)
     * @param shopId 店家 ID
     * @param files 上傳的文件列表
     * @return 更新後的店家 DTO (包含新的媒體列表)
     */
    ShopDTO uploadShopMedia(Long shopId, List<MultipartFile> files);

    /**
     * 刪除指定的店家媒體
     * @param shopId 店家 ID
     * @param mediaId 要刪除的媒體 ID
     */
    void deleteShopMedia(Long shopId, Long mediaId);

}