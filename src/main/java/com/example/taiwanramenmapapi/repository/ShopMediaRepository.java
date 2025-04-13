package com.example.taiwanramenmapapi.repository;

import com.example.taiwanramenmapapi.entity.ShopMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // 標記為 Spring 管理的 Repository Bean
public interface ShopMediaRepository extends JpaRepository<ShopMedia, Long> { // 泛型：實體類型, 主鍵類型

    /**
     * 根據 Shop ID 查找所有關聯的媒體記錄。
     * 結果按照 displayOrder 字段升序排列。
     * @param shopId 店家 ID
     * @return 該店家對應的媒體記錄列表 (ShopMedia)
     */
    List<ShopMedia> findByShopIdOrderByDisplayOrderAsc(Long shopId);

    /**
     * 根據 Shop ID 計算關聯的媒體記錄數量。
     * 用於確定新上傳媒體的起始 displayOrder。
     * @param shopId 店家 ID
     * @return 該店家擁有的媒體數量
     */
    int countByShopId(Long shopId); // Spring Data JPA 會自動生成 COUNT 查詢

    /**
     * 根據 Shop ID 刪除所有關聯的媒體記錄。
     * 注意：如果 Shop 實體的 @OneToMany(cascade=...) 設置了級聯刪除，
     * 則不需要手動調用此方法，刪除 Shop 時 JPA 會自動處理。
     * 如果沒有設置級聯刪除，或者需要在不刪除 Shop 的情況下清除所有媒體，
     * 則可以啟用並調用此方法。
     * @param shopId 店家 ID
     * @return 刪除的記錄數量 (返回類型可以是 void 或 int/long)
     */
    // void deleteByShopId(Long shopId); // 或者 long deleteByShopId(Long shopId);

}