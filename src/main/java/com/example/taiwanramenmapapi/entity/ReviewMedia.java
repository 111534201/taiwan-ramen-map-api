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
@Table(name = "review_media")
public class ReviewMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 媒體類型 (主要應該是 "image")
    @Column(length = 20)
    @Builder.Default // 預設為圖片
    private String type = "image";

    // 儲存的媒體文件路徑或 URL
    @Column(nullable = false, length = 1024)
    private String url;

    // --- 關聯到評論 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    @ToString.Exclude
    private Review review;
}