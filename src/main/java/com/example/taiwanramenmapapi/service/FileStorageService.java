package com.example.taiwanramenmapapi.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path; // 引入 Path
import java.util.List; // 引入 List

public interface FileStorageService {

    /**
     * 初始化儲存目錄
     */
    void init();

    /**
     * 儲存單個文件
     * @param file 要儲存的文件
     * @param subDirectory 相對於根目錄的子目錄 (例如 "shops/1", "reviews/5")
     * @return 儲存後的文件名 (或包含子目錄的相對路徑)
     */
    String storeFile(MultipartFile file, String subDirectory);

    /**
     * 儲存多個文件
     * @param files 文件列表
     * @param subDirectory 子目錄
     * @return 儲存後的文件名列表
     */
    List<String> storeFiles(List<MultipartFile> files, String subDirectory);


    /**
     * 加載文件路徑
     * @param filename 包含子目錄的文件名 (例如 "shops/1/image.jpg")
     * @return 文件的絕對路徑
     */
    Path loadFile(String filename);

    /**
     * 刪除文件
     * @param filename 包含子目錄的文件名
     * @return 是否刪除成功
     */
    boolean deleteFile(String filename);
}