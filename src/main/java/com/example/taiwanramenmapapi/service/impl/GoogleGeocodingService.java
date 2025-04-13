package com.example.taiwanramenmapapi.service.impl;

import com.example.taiwanramenmapapi.exception.GeocodingException; // 引入
import com.example.taiwanramenmapapi.service.GeocodingService;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import jakarta.annotation.PostConstruct; // 引入 PostConstruct
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // 引入 StringUtils

import java.util.Optional;

@Service
public class GoogleGeocodingService implements GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGeocodingService.class);

    @Value("${google.maps.api-key}") // 從配置讀取 API Key
    private String apiKey;

    private GeoApiContext context;

    // 使用 @PostConstruct 確保在 Bean 初始化後執行此方法
    @PostConstruct
    private void init() {
        if (!StringUtils.hasText(apiKey) || "YOUR_GOOGLE_MAPS_API_KEY".equals(apiKey)) {
            logger.error("!!! Google Maps API Key 未配置或仍為預設值，地理編碼功能將無法使用 !!!");
            // 可以在這裡拋出異常阻止啟動，或僅記錄錯誤
            context = null; // 設置為 null，後續方法會檢查
        } else {
            logger.info("初始化 Google Geocoding Service...");
            try {
                this.context = new GeoApiContext.Builder()
                        .apiKey(apiKey)
                        .build();
                logger.info("Google Geocoding Service 初始化成功。");
            } catch (Exception e) {
                logger.error("初始化 Google Geocoding Service 失敗: {}", e.getMessage(), e);
                this.context = null;
            }
        }
    }

    @Override
    public Optional<LatLng> getLatLng(String address) {
        if (context == null) {
            logger.warn("Geocoding Service 未初始化，無法處理地址: {}", address);
            return Optional.empty();
        }
        if (!StringUtils.hasText(address)) {
            logger.warn("嘗試對空地址進行地理編碼");
            return Optional.empty();
        }

        try {
            // 調用 Google Geocoding API
            GeocodingResult[] results = GeocodingApi.geocode(context, address)
                    .region("tw") // 優先返回台灣地區結果
                    .language("zh-TW") // 返回繁體中文結果
                    .await();

            if (results != null && results.length > 0) {
                // 通常取第一個結果
                LatLng location = results[0].geometry.location;
                logger.info("地址 '{}' 成功編碼為: {}, {}", address, location.lat, location.lng);
                return Optional.of(location);
            } else {
                logger.warn("地址 '{}' 地理編碼未找到結果。", address);
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("地址 '{}' 地理編碼時發生錯誤: {}", address, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public LatLng getLatLngOrFail(String address) {
        return getLatLng(address)
                .orElseThrow(() -> new GeocodingException("無法根據地址獲取經緯度: " + address));
    }

    // 在應用關閉時關閉 GeoApiContext (可選但推薦)
    // @PreDestroy
    // public void shutdown() {
    //     if (this.context != null) {
    //         this.context.shutdown();
    //         logger.info("Google Geocoding Service 已關閉。");
    //     }
    // }
}