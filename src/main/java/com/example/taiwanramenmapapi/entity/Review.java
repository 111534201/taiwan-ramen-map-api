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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull; // 確保關聯不為空

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects; // 引入 Objects

@Data // Lombok: getter, setter, toString, equals, hashCode
@Builder // Lombok: Builder 模式
@NoArgsConstructor // Lombok: 無參數構造函數
@AllArgsConstructor // Lombok: 全參數構造函數
@Entity // JPA: 實體標記
@Table(name = "reviews") // 指定表名
public class Review {

    @Id // 主鍵
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增主鍵
    private Long id;

    // 評分 (1-5 星)，對於頂級評論是必須的，對於回覆可以是 null
    @Min(value = 1, message = "評分不能低於 1")
    @Max(value = 5, message = "評分不能高於 5")
    @Column(nullable = true) // 允許回覆沒有評分
    private Integer rating;

    // 評論/回覆內容 (允許較長文本)
    @Column(columnDefinition = "TEXT")
    private String content;

    // --- 關聯：評論者 (User) ---
    // ManyToOne: 多個評論可以來自同一個用戶
    // FetchType.LAZY: 延遲加載用戶信息
    // JoinColumn: 外鍵 user_id，不允許為空
    @NotNull // 確保每個評論都有用戶關聯
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude // 避免 toString 循環
    private User user;

    // --- 關聯：所屬店家 (Shop) ---
    @NotNull // 確保每個評論都屬於某個店家
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    @ToString.Exclude
    private Shop shop;

    // --- 關聯：評論附帶的媒體 (照片) ---
    // OneToMany: 一條評論可以有多張照片
    // mappedBy: 指向 ReviewMedia 中的 review 字段
    // cascade: ALL 表示對 Review 的操作級聯到 ReviewMedia
    // orphanRemoval: true 表示從 media 列表移除時也刪除數據庫記錄
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @Default // Lombok Builder 預設值
    private List<ReviewMedia> media = new ArrayList<>();

    // --- 關聯：父評論 (用於實現回覆) ---
    // ManyToOne: 多個回覆可以指向同一個父評論
    // JoinColumn: 外鍵 parent_review_id，允許為 null (表示頂級評論)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_review_id")
    @ToString.Exclude // 避免與父評論形成 toString 循環
    private Review parentReview;

    // --- 關聯：此評論下的所有回覆 ---
    // OneToMany: 一個頂級評論可以有多個回覆
    // mappedBy: 指向 Review 實體中的 parentReview 字段
    // cascade: ALL 確保刪除頂級評論時，其下的回覆也會被刪除
    @OneToMany(mappedBy = "parentReview", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude // 避免與子評論形成 toString 循環
    @Default // Lombok Builder 預設值
    private List<Review> replies = new ArrayList<>();

    // --- 回覆計數 (冗餘字段，方便查詢，預設為 0) ---
    // 需要在 Service 層手動維護此計數
    @Column(name = "reply_count", columnDefinition = "INT DEFAULT 0")
    @Default
    private Integer replyCount = 0;

    // --- 時間戳 ---
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- 輔助方法 (用於維護雙向關聯) ---

    /**
     * 添加媒體到評論的媒體列表，並設置媒體的評論為當前評論。
     * @param mediaItem 要添加的媒體
     */
    public void addMedia(ReviewMedia mediaItem) {
        if (this.media == null) {
            this.media = new ArrayList<>();
        }
        if (mediaItem != null && !this.media.contains(mediaItem)) {
            boolean alreadyExists = this.media.stream().anyMatch(m -> Objects.equals(m.getId(), mediaItem.getId()));
            if (!alreadyExists) {
                this.media.add(mediaItem);
                mediaItem.setReview(this); // 設置關聯
            }
        }
    }

    /**
     * 從評論的媒體列表中移除媒體，並將媒體的評論設置為 null。
     * @param mediaItem 要移除的媒體
     */
    public void removeMedia(ReviewMedia mediaItem) {
        if (this.media != null && mediaItem != null) {
            boolean removed = this.media.remove(mediaItem);
            if (removed) {
                mediaItem.setReview(null); // 解除關聯
            }
        }
    }

    /**
     * 添加一個回覆到當前評論的回覆列表，並設置回覆的父評論為當前評論。
     * 注意：通常不直接這樣維護 replies 列表，而是通過設置回覆的 parentReview。
     * 但如果需要級聯操作或方便訪問，可以保留。
     * @param reply 要添加的回覆評論
     */
    public void addReply(Review reply) {
        if (this.replies == null) {
            this.replies = new ArrayList<>();
        }
        if (reply != null && this.parentReview == null && !this.replies.contains(reply)) { // 確保當前是頂級評論
            boolean alreadyExists = this.replies.stream().anyMatch(r -> Objects.equals(r.getId(), reply.getId()));
            if (!alreadyExists) {
                this.replies.add(reply);
                reply.setParentReview(this); // 設置父評論
            }
        }
    }

    /**
     * 從當前評論的回覆列表中移除一個回覆，並將該回覆的父評論設置為 null。
     * @param reply 要移除的回覆評論
     */
    public void removeReply(Review reply) {
        if (this.replies != null && reply != null) {
            boolean removed = this.replies.remove(reply);
            if (removed) {
                reply.setParentReview(null); // 解除父評論關聯
            }
        }
    }

    // --- equals 和 hashCode ---
    // 主要基於 ID 比較，避免因集合變化導致的行為不一致
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return id != null ? Objects.equals(id, review.id) : super.equals(o);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : super.hashCode();
    }
}