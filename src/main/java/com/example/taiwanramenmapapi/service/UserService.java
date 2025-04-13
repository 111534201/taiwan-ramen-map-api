package com.example.taiwanramenmapapi.service;

import com.example.taiwanramenmapapi.dto.response.UserDTO; // 引入 UserDTO
import com.example.taiwanramenmapapi.entity.enums.Role; // 引入 Role 枚舉
import org.springframework.security.core.userdetails.UserDetailsService; // 繼承 UserDetailsService

import java.util.List; // 引入 List

/**
 * 用戶服務接口，定義了與用戶相關的業務邏輯操作。
 * 同時繼承 Spring Security 的 UserDetailsService 以整合身份驗證。
 */
public interface UserService extends UserDetailsService {

    // --- UserDetailsService 接口繼承的方法 ---
    // Spring Security 會調用此方法根據用戶名加載用戶信息
    // loadUserByUsername(String username) throws UsernameNotFoundException;
    // (無需在此顯式聲明，已通過 extends 繼承)

    // --- 用戶管理相關的方法 (主要供管理員使用) ---

    /**
     * 獲取系統中所有用戶的列表。
     * @return 包含所有用戶 DTO 的列表。
     */
    List<UserDTO> getAllUsers();

    /**
     * 根據用戶 ID 獲取用戶的詳細信息。
     * @param userId 要查詢的用戶 ID。
     * @return 對應的用戶 DTO。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果用戶不存在。
     */
    UserDTO getUserById(Long userId);

    /**
     * 更新指定用戶的角色 (由管理員操作)。
     * 注意：此方法不應用於將用戶設置為 ROLE_SHOP_OWNER。
     * @param userId 要更新角色的用戶 ID。
     * @param newRole 新的角色 (應為 ROLE_USER 或 ROLE_ADMIN)。
     * @return 更新後的用戶 DTO。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果用戶不存在。
     * @throws com.example.taiwanramenmapapi.exception.BadRequestException 如果嘗試設置非法角色或修改自己。
     * @throws com.example.taiwanramenmapapi.exception.UnauthorizedActionException 如果操作者不是管理員。
     */
    UserDTO updateUserRole(Long userId, Role newRole);

    /**
     * 啟用或禁用指定用戶的帳號 (由管理員操作)。
     * @param userId 要操作的用戶 ID。
     * @param enabled true 表示啟用，false 表示禁用。
     * @return 更新後的用戶 DTO。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果用戶不存在。
     * @throws com.example.taiwanramenmapapi.exception.BadRequestException 如果管理員嘗試禁用自己。
     * @throws com.example.taiwanramenmapapi.exception.UnauthorizedActionException 如果操作者不是管理員。
     */
    UserDTO setUserEnabled(Long userId, boolean enabled);

    /**
     * 刪除指定 ID 的用戶 (及其所有關聯數據和文件)。
     * 此操作具有高風險性，應僅由管理員執行。
     * @param userId 要刪除的用戶 ID。
     * @throws com.example.taiwanramenmapapi.exception.ResourceNotFoundException 如果用戶不存在。
     * @throws com.example.taiwanramenmapapi.exception.BadRequestException 如果管理員嘗試刪除自己。
     * @throws com.example.taiwanramenmapapi.exception.UnauthorizedActionException 如果操作者不是管理員。
     */
    void deleteUser(Long userId); // *** 添加刪除用戶的方法聲明 ***

}