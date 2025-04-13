package com.example.taiwanramenmapapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder.Default;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // 引入 Objects 用於輔助方法

@Data // Lombok: getter, setter, toString, equals, hashCode
@Builder // Lombok: Builder 模式
@NoArgsConstructor // Lombok: 無參數構造函數
@AllArgsConstructor // Lombok: 全參數構造函數
@Entity // JPA: 標記為實體
@Table(name = "shops") // 指定數據庫表名
public class Shop {

    @Id // 主鍵
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主鍵自增
    private Long id;

    @Column(nullable = false) // 不允許為空
    private String name; // 店家名稱

    @Column(nullable = false, length = 512) // 不允許為空，增加長度
    private String address; // 店家地址

    // 緯度 (高精度)
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    // 經度 (高精度)
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    // 電話 (可選)
    @Column(length = 30) // 增加電話長度
    private String phone;

    // 營業時間 (使用 TEXT 類型儲存較長描述)
    @Column(name = "opening_hours", columnDefinition = "TEXT")
    private String openingHours;

    // 特色描述 (使用 TEXT 類型)
    @Column(columnDefinition = "TEXT")
    private String description;

    // 平均評分 (DECIMAL 類型，保留 2 位小數，預設為 0.00)
    @Default
    @Column(name = "average_rating", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0.00")
    private BigDecimal averageRating = BigDecimal.ZERO;

    // 評論總數 (INT 類型，預設為 0)
    @Default
    @Column(name = "review_count", columnDefinition = "INT DEFAULT 0")
    private Integer reviewCount = 0;

    // 加權綜合評分 (DECIMAL 類型，保留 7 位小數，預設為 0.0)
    @Default
    @Column(name = "weighted_rating", precision = 10, scale = 7, columnDefinition = "DECIMAL(10,7) DEFAULT 0.0")
    private BigDecimal weightedRating = BigDecimal.ZERO;

    // --- 關聯：店家擁有者 ---
    // ManyToOne: 多個店家可以對應一個用戶 (雖然業務邏輯限制為一對一)
    // FetchType.LAZY: 延遲加載 User 信息
    // JoinColumn: 指定外鍵列名，引用 users 表的 id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id") // 外鍵欄位名
    @ToString.Exclude // 避免 toString 循環引用
    private User owner;

    // --- 關聯：店家的評論列表 ---
    // OneToMany: 一個店家對應多個評論
    // mappedBy = "shop": 指向 Review 實體中名為 "shop" 的字段，由 Review 維護外鍵關係
    // cascade = CascadeType.ALL: 對 Shop 的操作 (包括刪除) 會級聯到 Reviews
    // orphanRemoval = true: 如果從 reviews 集合中移除 Review，該 Review 記錄也會從數據庫刪除
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude // 避免 toString 循環引用
    @Default // Lombok Builder 預設值
    private List<Review> reviews = new ArrayList<>();

    // --- 關聯：店家的媒體列表 ---
    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude // 避免 toString 循環引用
    @Default // Lombok Builder 預設值
    private List<ShopMedia> media = new ArrayList<>();

    // --- 時間戳 ---
    @CreationTimestamp // 創建時自動設置
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // 更新時自動設置
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- 輔助方法 (用於維護雙向關聯，確保數據一致性) ---

    /**
     * 添加評論到店家的評論列表，並設置評論的店家為當前店家。
     * @param review 要添加的評論
     */
    public void addReview(Review review) {
        if (this.reviews == null) {
            this.reviews = new ArrayList<>();
        }
        if (review != null && !this.reviews.contains(review)) {
            // 檢查是否已存在相同 ID 的評論 (雖然 List.contains 通常基於 equals)
            boolean alreadyExists = this.reviews.stream().anyMatch(r -> Objects.equals(r.getId(), review.getId()));
            if (!alreadyExists) {
                this.reviews.add(review);
                review.setShop(this); // 維護關聯的另一端
            }
        }
    }

    /**
     * 從店家的評論列表中移除評論，並將評論的店家設置為 null。
     * @param review 要移除的評論
     */
    public void removeReview(Review review) {
        if (this.reviews != null && review != null) {
            boolean removed = this.reviews.remove(review);
            if (removed) {
                review.setShop(null); // 解除關聯的另一端
            }
        }
    }

    /**
     * 添加媒體到店家的媒體列表，並設置媒體的店家為當前店家。
     * @param mediaItem 要添加的媒體
     */
    public void addMedia(ShopMedia mediaItem) {
        if (this.media == null) {
            this.media = new ArrayList<>();
        }
        if (mediaItem != null && !this.media.contains(mediaItem)) {
            boolean alreadyExists = this.media.stream().anyMatch(m -> Objects.equals(m.getId(), mediaItem.getId()));
            if (!alreadyExists) {
                this.media.add(mediaItem);
                mediaItem.setShop(this);
            }
        }
    }

    /**
     * 從店家的媒體列表中移除媒體，並將媒體的店家設置為 null。
     * @param mediaItem 要移除的媒體
     */
    public void removeMedia(ShopMedia mediaItem) {
        if (this.media != null && mediaItem != null) {
            boolean removed = this.media.remove(mediaItem);
            if (removed) {
                mediaItem.setShop(null);
            }
        }
    }

    // 重寫 equals 和 hashCode 以便正確比較對象 (基於 ID)
    // Lombok 的 @Data 會自動生成，但有時需要手動調整以避免循環引用或只基於 ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shop shop = (Shop) o;
        // 只有當 ID 非空時才比較 ID，否則比較對象引用
        return id != null ? Objects.equals(id, shop.id) : super.equals(o);
    }

    @Override
    public int hashCode() {
        // 只使用 ID 計算 hashCode (如果 ID 非空)
        return id != null ? Objects.hash(id) : super.hashCode();
    }
}