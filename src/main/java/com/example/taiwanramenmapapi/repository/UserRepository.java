package com.example.taiwanramenmapapi.repository;

import com.example.taiwanramenmapapi.entity.User;
import com.example.taiwanramenmapapi.entity.enums.Role; // 引入 Role
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List; // 引入 List

@Repository // 標記為 Spring 管理的 Repository Bean
public interface UserRepository extends JpaRepository<User, Long> { // 泛型：實體類型, 主鍵類型

    // Spring Data JPA 會根據方法名自動生成查詢
    // 根據用戶名查找用戶 (返回 Optional 避免 NullPointerException)
    Optional<User> findByUsername(String username);

    // 根據 Email 查找用戶
    Optional<User> findByEmail(String email);

    // 檢查用戶名是否存在
    boolean existsByUsername(String username);

    // 檢查 Email 是否存在
    boolean existsByEmail(String email);

    // 查找特定角色的所有用戶 (例如，用於管理員查找所有店家用戶)
    List<User> findByRole(Role role);

    // 根據用戶名模糊查詢 (用於用戶管理搜索，可選)
    List<User> findByUsernameContainingIgnoreCase(String username);

}