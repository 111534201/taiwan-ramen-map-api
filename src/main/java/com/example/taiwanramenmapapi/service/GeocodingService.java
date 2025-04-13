package com.example.taiwanramenmapapi.service;

import com.google.maps.model.LatLng; // 引入
import java.util.Optional; // 引入

public interface GeocodingService {

    /**
     * 將地址轉換為經緯度坐標
     * @param address 要轉換的地址字符串
     * @return 包含 LatLng 的 Optional，如果成功則有值，否則為空
     */
    Optional<LatLng> getLatLng(String address);

    /**
     * 將地址轉換為經緯度坐標，如果失敗則拋出 GeocodingException
     * @param address 要轉換的地址字符串
     * @return LatLng 坐標對象
     * @throws com.example.taiwanramenmapapi.exception.GeocodingException 如果轉換失敗
     */
    LatLng getLatLngOrFail(String address);
}