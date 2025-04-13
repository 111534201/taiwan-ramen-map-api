package com.example.taiwanramenmapapi.controller;

import com.example.taiwanramenmapapi.dto.response.ApiResponse;
import com.example.taiwanramenmapapi.dto.response.UserDTO;
import com.example.taiwanramenmapapi.entity.enums.Role;
import com.example.taiwanramenmapapi.exception.BadRequestException;
import com.example.taiwanramenmapapi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 引入 PreAuthorize
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController // 標記為 RESTful Controller
@RequestMapping("/api/admin/users") // 所有此 Controller 的請求都基於 /api/admin/users
@PreAuthorize("hasRole('ADMIN')") // *** 整個 Controller 都需要管理員權限 ***
@Validated // 啟用方法級別的 @PathVariable 或 @RequestParam 驗證 (如果有的話)
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService; // 注入 UserService

    /**
     * GET /api/admin/users : 獲取所有用戶列表。
     * 僅限管理員訪問 (由類級別的 @PreAuthorize 保證)。
     * @return 包含 UserDTO 列表的 ApiResponse。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllUsers() {
        logger.info("管理員請求：獲取所有用戶列表");
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users)); // 返回 200 OK 和用戶列表
    }

    /**
     * GET /api/admin/users/{userId} : 根據 ID 獲取特定用戶信息。
     * 僅限管理員訪問。
     * @param userId 要獲取的用戶 ID。
     * @return 包含 UserDTO 的 ApiResponse。
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long userId) {
        logger.info("管理員請求：獲取用戶 ID {} 的信息", userId);
        UserDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user)); // 返回 200 OK 和用戶信息
    }

    /**
     * PATCH /api/admin/users/{userId}/role : 更新指定用戶的角色。
     * 僅限管理員訪問，且不能將用戶設置為店家角色，也不能修改自己的角色。
     * @param userId 要更新角色的用戶 ID。
     * @param payload 包含 "role" 鍵的請求體，值為 "ROLE_USER" 或 "ROLE_ADMIN"。
     * @return 包含更新後 UserDTO 的 ApiResponse。
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserDTO>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> payload) { // 使用 Map 接收請求體

        String roleString = payload.get("role"); // 從請求體獲取角色字符串
        if (roleString == null) {
            throw new BadRequestException("請求體中缺少 'role' 字段"); // 拋出異常，由 GlobalExceptionHandler 處理
        }

        Role newRole;
        try {
            newRole = Role.valueOf(roleString.toUpperCase()); // 轉換為枚舉，忽略大小寫
            // 驗證角色是否合法 (不能是店家)
            if (newRole != Role.ROLE_USER && newRole != Role.ROLE_ADMIN) {
                throw new IllegalArgumentException(); // 觸發下面的 catch
            }
        } catch (IllegalArgumentException e) {
            // 如果轉換失敗或角色無效
            throw new BadRequestException("無效的角色值: '" + roleString + "'。有效值為 ROLE_USER 或 ROLE_ADMIN。");
        }

        logger.warn("管理員嘗試將用戶 ID {} 的角色更新為 {}", userId, newRole); // 記錄為警告
        UserDTO updatedUser = userService.updateUserRole(userId, newRole); // 調用 Service 更新
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "用戶角色更新成功"));
    }

    /**
     * PATCH /api/admin/users/{userId}/enabled : 啟用或禁用指定用戶帳號。
     * 僅限管理員訪問，且不能禁用自己的帳號。
     * @param userId 要操作的用戶 ID。
     * @param payload 包含 "enabled" 鍵的請求體，值為 true 或 false。
     * @return 包含更新後 UserDTO 的 ApiResponse。
     */
    @PatchMapping("/{userId}/enabled")
    public ResponseEntity<ApiResponse<UserDTO>> setUserEnabled(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> payload) { // 使用 Map 接收請求體

        Boolean enabled = payload.get("enabled"); // 獲取啟用狀態
        if (enabled == null) {
            throw new BadRequestException("請求體中缺少 'enabled' 字段 (true 或 false)");
        }

        logger.warn("管理員嘗試將用戶 ID {} 的啟用狀態設置為 {}", userId, enabled); // 記錄為警告
        UserDTO updatedUser = userService.setUserEnabled(userId, enabled); // 調用 Service 更新
        String message = enabled ? "用戶帳號已啟用" : "用戶帳號已禁用";
        return ResponseEntity.ok(ApiResponse.success(updatedUser, message));
    }

    /**
     * DELETE /api/admin/users/{userId} : 刪除指定用戶 (及其所有關聯數據和文件)。
     * 僅限管理員訪問，且不能刪除自己。
     * @param userId 要刪除的用戶 ID。
     * @return 表示成功的 ApiResponse (無數據)。
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        logger.warn("管理員請求刪除用戶 ID: {}", userId); // 記錄為警告
        userService.deleteUser(userId); // 調用 Service 層實現的刪除方法
        return ResponseEntity.ok(ApiResponse.success("用戶及其所有關聯數據已成功刪除"));
    }
}