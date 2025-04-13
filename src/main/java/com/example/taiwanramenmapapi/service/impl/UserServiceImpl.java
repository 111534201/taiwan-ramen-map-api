package com.example.taiwanramenmapapi.service.impl;

import com.example.taiwanramenmapapi.dto.response.UserDTO;
import com.example.taiwanramenmapapi.entity.Review; // 引入 Review
import com.example.taiwanramenmapapi.entity.ReviewMedia; // 引入 ReviewMedia
import com.example.taiwanramenmapapi.entity.Shop; // 引入 Shop
import com.example.taiwanramenmapapi.entity.ShopMedia; // 引入 ShopMedia
import com.example.taiwanramenmapapi.entity.User;
import com.example.taiwanramenmapapi.entity.enums.Role;
import com.example.taiwanramenmapapi.exception.BadRequestException;
import com.example.taiwanramenmapapi.exception.ResourceNotFoundException;
import com.example.taiwanramenmapapi.exception.UnauthorizedActionException;
import com.example.taiwanramenmapapi.mapper.UserMapper;
import com.example.taiwanramenmapapi.repository.*; // 引入所有需要的 Repository
import com.example.taiwanramenmapapi.service.FileStorageService; // 引入 FileStorageService
import com.example.taiwanramenmapapi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;     // 引入 Page
import org.springframework.data.domain.Pageable; // 引入 Pageable
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired private UserRepository userRepository;
    @Autowired private UserMapper userMapper;
    @Autowired private ShopRepository shopRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ShopMediaRepository shopMediaRepository;
    @Autowired private ReviewMediaRepository reviewMediaRepository;
    @Autowired private FileStorageService fileStorageService;

    // --- UserDetailsService 接口方法實現 ---
    /**
     * 由 Spring Security 調用，根據用戶名加載用戶信息。
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("正在根據用戶名加載用戶: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("找不到用戶名為 '{}' 的用戶", username);
                    return new UsernameNotFoundException("用戶名或密碼錯誤"); // 為了安全，不提示具體是哪個錯
                });
    }

    // --- UserService 接口方法實現 ---

    /**
     * 獲取所有用戶列表 (僅限管理員)。
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        logger.debug("Service: 管理員請求獲取所有用戶列表");
        checkAdminAuthority(); // 權限檢查
        List<User> users = userRepository.findAll();
        return userMapper.toUserDTOs(users); // 使用 Mapper 轉換
    }

    /**
     * 根據用戶 ID 獲取用戶信息。
     */
    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(Long userId) {
        logger.debug("Service: 根據 ID 獲取用戶: {}", userId);
        // 此處可添加額外權限檢查，例如只允許 Admin 或用戶本人
        User user = findUserByIdOrThrow(userId);
        return userMapper.toUserDTO(user);
    }

    /**
     * 更新用戶角色 (僅限管理員，且不能設為店家)。
     */
    @Override
    @Transactional
    public UserDTO updateUserRole(Long userId, Role newRole) {
        logger.warn("Service: 管理員嘗試將用戶 ID {} 的角色更新為 {}", userId, newRole);
        checkAdminAuthority(); // 權限檢查

        if (newRole == Role.ROLE_SHOP_OWNER) {
            throw new BadRequestException("不能直接將用戶角色設置為店家，需通過店家註冊流程。");
        }

        User userToUpdate = findUserByIdOrThrow(userId);
        User currentUser = getCurrentAuthenticatedUser();

        if (Objects.equals(userToUpdate.getId(), currentUser.getId())) {
            throw new BadRequestException("不能修改自己的角色");
        }

        userToUpdate.setRole(newRole);
        User updatedUser = userRepository.save(userToUpdate);
        logger.info("用戶 ID {} 的角色已成功更新為 {}", userId, newRole);
        return userMapper.toUserDTO(updatedUser);
    }

    /**
     * 啟用或禁用用戶帳號 (僅限管理員)。
     */
    @Override
    @Transactional
    public UserDTO setUserEnabled(Long userId, boolean enabled) {
        logger.warn("Service: 管理員嘗試將用戶 ID {} 的啟用狀態設置為 {}", userId, enabled);
        checkAdminAuthority(); // 權限檢查

        User userToUpdate = findUserByIdOrThrow(userId);
        User currentUser = getCurrentAuthenticatedUser();

        if (!enabled && Objects.equals(userToUpdate.getId(), currentUser.getId())) {
            throw new BadRequestException("不能禁用自己的帳號");
        }

        userToUpdate.setEnabled(enabled);
        User updatedUser = userRepository.save(userToUpdate);
        logger.info("用戶 ID {} 的啟用狀態已成功設置為 {}", userId, enabled);
        return userMapper.toUserDTO(updatedUser);
    }

    /**
     * 刪除指定用戶及其所有關聯數據和文件 (僅限管理員)。
     * @param userId 要刪除的用戶 ID。
     */
    @Override
    @Transactional // 確保文件刪除和數據庫刪除在同一事務中
    public void deleteUser(Long userId) {
        logger.warn("管理員嘗試刪除用戶 ID: {}", userId);
        checkAdminAuthority(); // 權限檢查

        User userToDelete = findUserByIdOrThrow(userId); // 查找用戶
        User currentUser = getCurrentAuthenticatedUser(); // 獲取當前操作的管理員

        // 防止管理員刪除自己
        if (Objects.equals(userToDelete.getId(), currentUser.getId())) {
            throw new BadRequestException("不能刪除自己的帳號");
        }

        // --- 在刪除用戶實體前，清理關聯的物理文件 ---

        // 1. 如果是店家用戶，清理其擁有的所有店家的相關文件
        if (userToDelete.getRole() == Role.ROLE_SHOP_OWNER) {
            // 獲取該用戶擁有的所有店家 (理論上只有一個，但用列表處理更安全)
            List<Shop> ownedShops = shopRepository.findByOwnerIdOrderByIdAsc(userId);
            for (Shop shop : ownedShops) {
                logger.warn("正在清理店家 ID {} (屬於用戶 {}) 的關聯文件...", shop.getId(), userId);
                // 刪除店家本身的媒體文件
                deleteShopMediaFilesInternal(shop);
                // 查找並刪除該店家下所有評論的媒體文件
                List<Review> shopReviews = reviewRepository.findByShopId(shop.getId());
                for(Review review : shopReviews) {
                    deleteReviewMediaFilesInternal(review);
                }
                logger.info("店家 ID {} 的文件清理完畢。", shop.getId());
            }
        }

        // 2. 清理該用戶（無論角色）自己發表的所有評論的媒體文件
        logger.warn("正在清理用戶 ID {} 發表的評論媒體文件...", userId);
        // 使用分頁處理可能更優，但如果評論不多，一次性獲取也可以
        Page<Review> userReviewsPage = reviewRepository.findByUserId(userId, Pageable.unpaged()); // 獲取用戶所有評論
        for(Review review : userReviewsPage.getContent()) {
            deleteReviewMediaFilesInternal(review); // 刪除該評論的媒體文件
        }
        logger.info("用戶 ID {} 的評論文件清理完畢。", userId);


        // --- 執行用戶刪除 ---
        // JPA 的級聯刪除設置 (CascadeType.ALL, orphanRemoval=true) 將自動處理：
        // - 刪除 users 表中的記錄
        // - 刪除 shops 表中 owner_id 為該用戶的記錄
        // - 刪除 reviews 表中 user_id 為該用戶的記錄
        // - 級聯刪除 shop_media, review_media, 以及 reviews 的 replies 記錄
        userRepository.delete(userToDelete);
        logger.warn("用戶 ID: {} 及其所有數據庫關聯記錄已成功刪除。", userId);
    }


    // --- 私有輔助方法 ---

    /** 查找用戶或拋出異常 */
    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    /** 獲取當前已認證的用戶實體 */
    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedActionException("用戶未登錄或認證無效");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) { return (User) principal; }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username (from context)", username));
        }
        throw new UnauthorizedActionException("無法識別的用戶信息: " + principal.getClass().getName());
    }

    /** 檢查當前用戶是否為管理員 */
    private void checkAdminAuthority() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedActionException("用戶未認證");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(Role.ROLE_ADMIN.name()));
        if (!isAdmin) {
            throw new UnauthorizedActionException("只有管理員可以執行此操作");
        }
        logger.debug("管理員權限檢查通過。");
    }

    /** 內部方法：刪除指定店家關聯的所有物理媒體文件 */
    private void deleteShopMediaFilesInternal(Shop shop) {
        if (shop == null) return;
        logger.warn("準備刪除店家 ID {} 的所有媒體文件...", shop.getId());
        List<ShopMedia> mediaList = shopMediaRepository.findByShopIdOrderByDisplayOrderAsc(shop.getId());
        deleteMediaFiles(mediaList, "ShopMedia", shop.getId());
    }

    /** 內部方法：刪除指定評論關聯的所有物理媒體文件 */
    private void deleteReviewMediaFilesInternal(Review review) {
        if (review == null) return;
        logger.warn("準備刪除評論 ID {} 的所有媒體文件...", review.getId());
        List<ReviewMedia> mediaList = reviewMediaRepository.findByReviewId(review.getId());
        deleteMediaFiles(mediaList, "ReviewMedia", review.getId());
    }

    /** 通用的媒體文件刪除邏輯 */
    private <T> void deleteMediaFiles(List<T> mediaList, String entityType, Long parentId) {
        if (mediaList == null || mediaList.isEmpty()) return;
        int deletedCount = 0;
        long totalCount = mediaList.size();

        for (T media : mediaList) {
            String url = null;
            Long mediaId = null;
            // 使用 getter 方法獲取 url 和 id
            try {
                if (media instanceof ShopMedia sm) {
                    url = sm.getUrl();
                    mediaId = sm.getId();
                } else if (media instanceof ReviewMedia rm) {
                    url = rm.getUrl();
                    mediaId = rm.getId();
                }
            } catch (ClassCastException e) {
                logger.error("處理媒體列表時類型轉換錯誤", e);
                continue; // 跳過無法處理的類型
            }


            if (url != null) {
                try {
                    if (fileStorageService.deleteFile(url)) { // 使用 fileStorageService 刪除
                        deletedCount++;
                        logger.debug("已刪除 {} 文件: (ParentID: {}, MediaID: {}, Path: {})", entityType, parentId, mediaId, url);
                    } else {
                        logger.warn("嘗試刪除 {} 文件失敗 (可能文件已不存在): (ParentID: {}, MediaID: {}, Path: {})", entityType, parentId, mediaId, url);
                    }
                } catch (Exception e) {
                    logger.error("刪除 {} 文件時發生錯誤 (ParentID: {}, MediaID: {}, Path: {}): {}", entityType, parentId, mediaId, url, e.getMessage());
                }
            } else {
                logger.warn("{} 記錄 (ID: {}) 的 URL 為空，無法刪除文件。", entityType, mediaId);
            }
        }
        logger.info("{} 個 {} 媒體文件中的 {} 個已嘗試刪除 (Parent ID: {})。", totalCount, entityType, deletedCount, parentId);
    }

}