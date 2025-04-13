package com.example.taiwanramenmapapi.repository;

import com.example.taiwanramenmapapi.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // 引入 JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
// 繼承 JpaRepository 提供基本的 CRUD 操作
// 繼承 JpaSpecificationExecutor 提供使用 Specification 進行動態條件查詢的能力 (可選但推薦)
public interface ShopRepository extends JpaRepository<Shop, Long>, JpaSpecificationExecutor<Shop> {

    /**
     * 根據店名模糊查詢 (忽略大小寫)，返回分頁結果。
     * Spring Data JPA 會自動根據方法名生成查詢。
     * @param name 店名關鍵字
     * @param pageable 分頁和排序信息
     * @return 店家分頁數據
     */
    Page<Shop> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 根據地址模糊查詢 (忽略大小寫)，返回分頁結果。
     * 可用於實現縣市或其他地址關鍵字篩選。
     * @param address 地址關鍵字
     * @param pageable 分頁和排序信息
     * @return 店家分頁數據
     */
    Page<Shop> findByAddressContainingIgnoreCase(String address, Pageable pageable);

    /**
     * 查找特定擁有者的所有店家，返回分頁結果。
     * @param ownerId 擁有者用戶 ID
     * @param pageable 分頁和排序信息
     * @return 店家分頁數據
     */
    Page<Shop> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * 查找特定店家 ID 且屬於特定擁有者的店家。
     * 主要用於權限驗證，例如店家編輯自己的店鋪時。
     * @param id 店家 ID
     * @param ownerId 擁有者用戶 ID
     * @return 包含店家的 Optional，如果找到且匹配則有值
     */
    Optional<Shop> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * 檢查指定 ownerId 的用戶是否已擁有至少一家店。
     * 用於店家註冊流程，限制一個用戶只能創建一家店。
     * @param ownerId 要檢查的用戶 ID
     * @return 如果已擁有店家則返回 true，否則返回 false
     */
    boolean existsByOwnerId(Long ownerId);

    /**
     * 根據擁有者 ID 查找其擁有的所有店家列表。
     * 用於刪除用戶前獲取其所有關聯店家。
     * @param ownerId 擁有者用戶 ID
     * @return 店家列表
     */
    List<Shop> findByOwnerIdOrderByIdAsc(Long ownerId); // 按 ID 排序以保證順序

    /**
     * 根據地圖的經緯度邊界查找店家。
     * 使用 JPQL (Java Persistence Query Language) 自定義查詢。
     * @param minLat 最小緯度
     * @param maxLat 最大緯度
     * @param minLng 最小經度
     * @param maxLng 最大經度
     * @return 在邊界內的店家列表
     */
    @Query("SELECT s FROM Shop s WHERE s.latitude BETWEEN :minLat AND :maxLat AND s.longitude BETWEEN :minLng AND :maxLng")
    List<Shop> findByLocationBounds(@Param("minLat") BigDecimal minLat,
                                    @Param("maxLat") BigDecimal maxLat,
                                    @Param("minLng") BigDecimal minLng,
                                    @Param("maxLng") BigDecimal maxLng);

    // --- 排行榜相關查詢 ---

    /**
     * 查找評論數達到指定門檻的店家，並按加權評分和評論數降序排序，返回分頁結果。
     * 用於生成全局排行榜。
     * @param minReviewCount 最低評論數門檻
     * @param pageable 分頁信息 (包含 limit)
     * @return 排行榜店家分頁數據
     */
    @Query("SELECT s FROM Shop s WHERE s.reviewCount >= :minReviewCount ORDER BY s.weightedRating DESC, s.reviewCount DESC")
    Page<Shop> findTopRatedShops(@Param("minReviewCount") int minReviewCount, Pageable pageable);

    /**
     * 查找特定區域內（地址模糊匹配）且評論數達到門檻的店家，並按加權評分和評論數降序排序，返回分頁結果。
     * 用於生成區域排行榜。
     * @param region 區域名稱 (用於 LIKE 查詢)
     * @param minReviewCount 最低評論數門檻
     * @param pageable 分頁信息 (包含 limit)
     * @return 區域排行榜店家分頁數據
     */
    @Query("SELECT s FROM Shop s WHERE s.address LIKE %:region% AND s.reviewCount >= :minReviewCount ORDER BY s.weightedRating DESC, s.reviewCount DESC")
    Page<Shop> findTopRatedShopsByRegion(@Param("region") String region, @Param("minReviewCount") int minReviewCount, Pageable pageable);

}