package com.example.taiwanramenmapapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shop_media")
public class ShopMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 媒體類型 (例如 "image", "video") - 可選，方便前端區分
    @Column(length = 20)
    private String type;

    // 儲存的媒體文件路徑或 URL (相對路徑或完整 URL)
    @Column(nullable = false, length = 1024) // 路徑/URL 可能較長
    private String url;

    // 排序序號 (可選，用於決定顯示順序)
    private Integer displayOrder;

    // --- 關聯到店家 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    @ToString.Exclude
    private Shop shop;
}