package com.example.taiwanramenmapapi.config;

import org.slf4j.Logger; // 引入 Logger
import org.slf4j.LoggerFactory; // 引入 LoggerFactory
import org.springframework.beans.factory.annotation.Value; // 引入 Value
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // 引入 WebMvcConfigurer

import java.nio.file.Path; // 引入 Path
import java.nio.file.Paths; // 引入 Paths

@Configuration // 標記為配置類
public class WebConfig implements WebMvcConfigurer { // 實現 WebMvcConfigurer 接口

    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    // 從 application.properties 讀取上傳目錄和 URL 路徑
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    // 注意：這裡的 url-path 應該是請求路徑模式，例如 /uploads/**
    @Value("${app.upload.url-path:/uploads/**}")
    private String uploadUrlPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 將 URL 路徑模式 (例如 /uploads/**) 映射到文件系統上的實際目錄
        // Path.of(uploadDir).toAbsolutePath().toString() 會得到絕對路徑
        // 需要確保路徑以 "file:" 開頭，並且結尾有 "/"

        Path uploadPathAbsolute = Paths.get(uploadDir).toAbsolutePath();
        String resourceLocation = "file:" + uploadPathAbsolute.toString().replace("\\", "/") + "/"; // 標準化路徑並添加 file: 前綴和結尾 /

        // 清理 URL 路徑，移除末尾的 **
        String urlPathPattern = uploadUrlPath.endsWith("/**") ? uploadUrlPath : uploadUrlPath + "/**";


        logger.info("配置靜態資源映射： URL 路徑 '{}' -> 文件系統位置 '{}'", urlPathPattern, resourceLocation);

        registry.addResourceHandler(urlPathPattern) // 處理匹配此模式的請求
                .addResourceLocations(resourceLocation); // 將請求映射到這個本地文件夾
        // .setCachePeriod(3600); // 可選：設置緩存時間 (秒)
    }

    // 如果你需要全局配置 CORS，也可以在這裡實現 addCorsMappings 方法，
    // 但我們已經在 SecurityConfig 中通過 CorsConfigurationSource Bean 進行了配置，通常一種方式即可。
    /*
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // 應用到 /api 路徑
                .allowedOrigins("http://localhost:5173", "http://localhost:3000") // 允許的前端來源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // 允許的方法
                .allowedHeaders("*") // 允許所有頭
                .allowCredentials(true) // 允許憑證
                .maxAge(3600);
    }
    */
}