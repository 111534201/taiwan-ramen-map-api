package com.example.taiwanramenmapapi.config; // 或其他包

import com.example.taiwanramenmapapi.entity.User;
import com.example.taiwanramenmapapi.entity.enums.Role;
import com.example.taiwanramenmapapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component // 標記為 Spring 組件
public class DataInitializer implements CommandLineRunner { // 實現 CommandLineRunner 接口

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository; // 注入 UserRepository

    @Autowired
    private PasswordEncoder passwordEncoder; // 注入密碼編碼器

    /**
     * 在 Spring Boot 應用啟動完成後執行，用於初始化數據。
     */
    @Override
    @Transactional // 將初始化操作包在事務中
    public void run(String... args) throws Exception {
        logger.info("執行數據初始化器 DataInitializer...");

        // 檢查是否已存在管理員帳號
        if (userRepository.findByRole(Role.ROLE_ADMIN).isEmpty()) {
            logger.warn("資料庫中未找到管理員帳號，正在創建預設管理員...");

            // === !!! 極度重要：請修改預設的密碼和 Email !!! ===
            String adminUsername = "admin";
            String adminPassword = "adminpassword"; // 僅供演示，生產環境必須更換！
            String adminEmail = "admin@ramenmap.local"; // 請更換為真實有效的 Email
            // === --- ===

            // 創建預設管理員用戶
            User adminUser = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword)) // 使用注入的 Encoder 加密
                    .email(adminEmail)
                    .role(Role.ROLE_ADMIN)
                    .enabled(true) // 預設啟用
                    .build();

            try {
                userRepository.save(adminUser); // 保存到數據庫
                // 打印重要的提示信息
                logger.info("============================================================");
                logger.info("=== 預設管理員帳號已創建成功！ ===");
                logger.info("=== 用戶名: {}", adminUsername);
                logger.info("=== 密碼: {} (!!! 請立即登入並修改密碼 !!!)", adminPassword);
                logger.info("=== Email: {}", adminEmail);
                logger.info("============================================================");
            } catch (Exception e) {
                logger.error("創建預設管理員帳號時發生錯誤: {}", e.getMessage(), e);
                // 如果初始化失敗，可以考慮是否讓應用停止啟動
                // throw new RuntimeException("Failed to initialize default admin user", e);
            }
        } else {
            logger.info("管理員帳號已存在，跳過初始化。");
        }

        logger.info("數據初始化器 DataInitializer 執行完畢。");
    }
}