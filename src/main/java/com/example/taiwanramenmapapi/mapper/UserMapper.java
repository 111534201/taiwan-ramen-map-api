package com.example.taiwanramenmapapi.mapper;

import com.example.taiwanramenmapapi.dto.response.ReviewerDTO;
import com.example.taiwanramenmapapi.dto.response.UserDTO;
import com.example.taiwanramenmapapi.entity.Shop; // 引入 Shop
import com.example.taiwanramenmapapi.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping; // 引入 Mapping
import org.mapstruct.Named; // 引入 Named
import java.util.Collections; // 引入 Collections
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring") // 生成 Spring Bean
public interface UserMapper {

    // 將 User 轉換為 UserDTO
    // source = "ownedShops": 指定來源是 User 實體的 ownedShops 列表
    // target = "ownedShopIds": 指定目標是 UserDTO 的 ownedShopIds 列表
    // qualifiedByName = "shopsToIds": 調用下面 @Named("shopsToIds") 的方法進行轉換
    @Mapping(target = "ownedShopIds", source = "ownedShops", qualifiedByName = "shopsToIds")
    UserDTO toUserDTO(User user);

    // 將 User 轉換為 ReviewerDTO (通常只需要 ID 和 username)
    ReviewerDTO toReviewerDTO(User user);

    // 將 User 列表轉換為 UserDTO 列表
    List<UserDTO> toUserDTOs(List<User> users);


    // --- 自定義的命名轉換方法 ---
    @Named("shopsToIds") // 給這個轉換方法命名
    default List<Long> shopsToIds(List<Shop> shops) {
        if (shops == null || shops.isEmpty()) {
            // 返回空列表而不是 null，避免後續操作出錯
            return Collections.emptyList();
        }
        return shops.stream()
                .map(Shop::getId) // 獲取每個 Shop 的 ID
                .filter(id -> id != null) // 過濾掉 ID 為 null 的情況 (理論上不應發生)
                .collect(Collectors.toList());
    }
}