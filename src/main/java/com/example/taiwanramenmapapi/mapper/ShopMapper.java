package com.example.taiwanramenmapapi.mapper;

// import com.example.taiwanramenmapapi.dto.request.CreateShopRequest;
import com.example.taiwanramenmapapi.dto.request.UpdateShopRequest;
import com.example.taiwanramenmapapi.dto.response.ShopDTO;
import com.example.taiwanramenmapapi.entity.Shop;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.NullValuePropertyMappingStrategy;
import java.util.List;

// *** 修改這裡：暫時移除 ShopMediaMapper.class 的依賴 ***
@Mapper(componentModel = "spring", uses = { UserMapper.class /*, ShopMediaMapper.class */ })
public interface ShopMapper {

    // 將 Shop 實體轉換為 ShopDTO
    @Mappings({
            @Mapping(source = "owner", target = "owner"),
            // *** 暫時註釋掉 media 的映射 ***
            // @Mapping(source = "media", target = "media"),
            // *** --- ***
            // --- 強制直接映射 ---
            @Mapping(source = "name", target = "name"),
            @Mapping(source = "address", target = "address"),
            @Mapping(source = "phone", target = "phone")
            // --- ---
    })
    ShopDTO toShopDTO(Shop shop);

    // List 轉換方法 MapStruct 會自動處理，但 media 字段也會受影響
    // 如果 toShopDTOs 被調用，可能也需要調整或接受 media 為空
    List<ShopDTO> toShopDTOs(List<Shop> shops);

    // --- 更新方法保持不變 ---
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateShopFromDto(UpdateShopRequest dto, @MappingTarget Shop shop);

}