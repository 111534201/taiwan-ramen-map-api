package com.example.taiwanramenmapapi.entity;

import com.example.taiwanramenmapapi.entity.enums.Role; // 引入 Role 枚舉
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder.Default; // 引入 Builder.Default
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data // Lombok: 自動生成 getter, setter, toString, equals, hashCode
@Builder // Lombok: 提供 Builder 模式
@NoArgsConstructor // Lombok: 無參數構造函數
@AllArgsConstructor // Lombok: 全參數構造函數
@Entity // 標記為 JPA 實體
@Table(name = "users", uniqueConstraints = { // 指定表名和唯一約束
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
public class User implements UserDetails { // 實現 UserDetails 以整合 Spring Security

    @Id // 主鍵
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 主鍵自增
    private Long id;

    @Column(nullable = false, unique = true) // 不為空且唯一
    private String username;

    @Column(nullable = false)
    private String password; // 儲存加密後的密碼

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING) // 將枚舉按名稱存儲 (例如 "ROLE_USER")
    @Column(nullable = false)
    private Role role; // 用戶角色

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE") // 默認為 true
    @Default // Lombok Builder 預設值
    private boolean enabled = true; // 帳號是否啟用

    // --- 關聯：店家擁有的店舖 ---
    // cascade = CascadeType.ALL: 對 User 的所有操作 (persist, merge, remove, refresh, detach) 都會級聯到 ownedShops
    // orphanRemoval = true: 如果從 User 的 ownedShops 列表中移除一個 Shop，該 Shop 將從數據庫刪除
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude // 避免 toString() 產生無限循環
    @Default // Lombok Builder 預設值
    private List<Shop> ownedShops = new ArrayList<>();

    // --- 關聯：用戶發表的評論 ---
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @Default
    private List<Review> reviews = new ArrayList<>();

    // --- 時間戳 ---
    @CreationTimestamp // 首次創建時自動設置
    @Column(name = "created_at", updatable = false) // 列名，且不允許更新
    private LocalDateTime createdAt;

    @UpdateTimestamp // 每次更新時自動設置
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- UserDetails 接口實現 ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 將 Role 枚舉轉換為 Spring Security 的權限對象
        return List.of(new SimpleGrantedAuthority(this.role.name()));
    }

    // getPassword() 和 getUsername() 由 @Data 生成

    @Override
    public boolean isAccountNonExpired() {
        return true; // 帳號永不過期
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 帳號永不鎖定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 憑證(密碼)永不過期
    }

    @Override
    public boolean isEnabled() {
        return this.enabled; // 返回我們定義的 enabled 字段狀態
    }

    // --- 輔助方法 (用於維護雙向關聯，可選) ---
    /**
     * 將一家店添加到此用戶擁有的店舖列表，並設置店舖的擁有者為此用戶。
     * @param shop 要添加的店舖
     */
    public void addOwnedShop(Shop shop) {
        if (this.ownedShops == null) {
            this.ownedShops = new ArrayList<>();
        }
        if (shop != null && !this.ownedShops.contains(shop)) {
            this.ownedShops.add(shop);
            shop.setOwner(this); // 維護另一端的關聯
        }
    }

    /**
     * 將一條評論添加到此用戶發表的評論列表，並設置評論的用戶為此用戶。
     * @param review 要添加的評論
     */
    public void addReview(Review review) {
        if (this.reviews == null) {
            this.reviews = new ArrayList<>();
        }
        if (review != null && !this.reviews.contains(review)) {
            this.reviews.add(review);
            review.setUser(this); // 維護另一端的關聯
        }
    }
}