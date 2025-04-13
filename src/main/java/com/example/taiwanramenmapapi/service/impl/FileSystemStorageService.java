package com.example.taiwanramenmapapi.service.impl;

import com.example.taiwanramenmapapi.exception.FileStorageException; // 引入 (稍後創建)
import com.example.taiwanramenmapapi.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList; // 引入
import java.util.List; // 引入
import java.util.UUID; // 引入 UUID

@Service
public class FileSystemStorageService implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorageService.class);

    @Value("${app.upload.dir:./uploads}") // 從配置讀取根上傳目錄
    private String uploadRootDir;

    private Path rootLocation;

    @Override
    @PostConstruct // 在 Bean 初始化後執行
    public void init() {
        try {
            this.rootLocation = Paths.get(uploadRootDir).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation); // 創建根目錄 (如果不存在)
            logger.info("文件儲存根目錄初始化完成: {}", rootLocation);
        } catch (IOException e) {
            logger.error("無法初始化文件儲存根目錄: {}", uploadRootDir, e);
            throw new FileStorageException("無法初始化文件儲存目錄", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename()); // 清理文件名
        logger.debug("準備儲存文件: {}, 到子目錄: {}", originalFilename, subDirectory);

        if (file.isEmpty()) {
            throw new FileStorageException("無法儲存空文件 " + originalFilename);
        }
        if (originalFilename.contains("..")) {
            // 安全檢查，防止路徑遍歷攻擊
            throw new FileStorageException("文件名包含無效的路徑序列 " + originalFilename);
        }

        try {
            // --- 生成唯一文件名以避免衝突 ---
            String fileExtension = "";
            int lastDot = originalFilename.lastIndexOf('.');
            if (lastDot > 0) {
                fileExtension = originalFilename.substring(lastDot); // 保留擴展名
            }
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            // --- ---

            // --- 確定目標路徑 (包含子目錄) ---
            Path targetDirectory = this.rootLocation;
            if (StringUtils.hasText(subDirectory)) {
                // 清理子目錄路徑，移除開頭的斜線 (如果有的話)
                String cleanSubDirectory = StringUtils.cleanPath(subDirectory);
                if(cleanSubDirectory.startsWith("/") || cleanSubDirectory.startsWith("\\")) {
                    cleanSubDirectory = cleanSubDirectory.substring(1);
                }
                targetDirectory = this.rootLocation.resolve(cleanSubDirectory).normalize();
                // 再次檢查，確保目標目錄仍在根目錄下
                if (!targetDirectory.startsWith(this.rootLocation)) {
                    throw new FileStorageException("子目錄路徑超出根目錄範圍: " + subDirectory);
                }
                Files.createDirectories(targetDirectory); // 創建子目錄 (如果不存在)
            }
            // --- ---

            // 目標文件完整路徑
            Path targetPath = targetDirectory.resolve(uniqueFilename);

            // 複製文件到目標位置
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("文件已成功儲存到: {}", targetPath);
            }

            // 返回包含子目錄的相對路徑或僅文件名，取決於你的需求
            // 返回包含子目錄的相對路徑比較好，方便後續查找和刪除
            if (StringUtils.hasText(subDirectory)) {
                String cleanSubDirectory = StringUtils.cleanPath(subDirectory);
                if(cleanSubDirectory.startsWith("/") || cleanSubDirectory.startsWith("\\")) {
                    cleanSubDirectory = cleanSubDirectory.substring(1);
                }
                return Paths.get(cleanSubDirectory, uniqueFilename).toString().replace("\\", "/"); // 標準化路徑分隔符
            } else {
                return uniqueFilename;
            }

        } catch (IOException e) {
            logger.error("儲存文件失敗: {}", originalFilename, e);
            throw new FileStorageException("儲存文件失敗 " + originalFilename, e);
        }
    }

    @Override
    public List<String> storeFiles(List<MultipartFile> files, String subDirectory) {
        List<String> storedFilenames = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    try {
                        storedFilenames.add(storeFile(file, subDirectory));
                    } catch (FileStorageException e) {
                        // 可以選擇記錄錯誤並繼續處理其他文件，或者直接拋出異常中斷
                        logger.error("儲存文件列表中的文件時出錯: {}", file.getOriginalFilename(), e);
                        // throw e; // 如果希望一個文件失敗就中斷
                    }
                }
            }
        }
        return storedFilenames;
    }


    @Override
    public Path loadFile(String filename) {
        // filename 應該是包含子目錄的相對路徑
        return this.rootLocation.resolve(filename).normalize();
    }

    @Override
    public boolean deleteFile(String filename) {
        if (!StringUtils.hasText(filename)) {
            logger.warn("嘗試刪除的文件名為空");
            return false;
        }
        try {
            Path filePath = loadFile(filename);
            // 安全檢查：確保要刪除的文件在 uploads 目錄下
            if (!filePath.startsWith(this.rootLocation)) {
                logger.error("嘗試刪除根目錄之外的文件: {}", filename);
                return false;
            }
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                logger.info("文件已刪除: {}", filename);
            } else {
                logger.warn("嘗試刪除不存在的文件: {}", filename);
            }

            // 可選：刪除空的父目錄 (如果需要)
            // Path parentDir = filePath.getParent();
            // if (parentDir != null && !parentDir.equals(this.rootLocation) && Files.isDirectory(parentDir) && Files.list(parentDir).findAny().isEmpty()) {
            //     Files.delete(parentDir);
            //     logger.info("已刪除空目錄: {}", parentDir);
            // }

            return deleted;
        } catch (IOException e) {
            logger.error("刪除文件失敗: {}", filename, e);
            // 不拋出異常，讓調用者決定如何處理
            return false;
        }
    }
}