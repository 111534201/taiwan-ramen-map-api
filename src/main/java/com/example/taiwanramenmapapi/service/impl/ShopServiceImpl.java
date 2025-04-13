package com.example.taiwanramenmapapi.service.impl;

import com.example.taiwanramenmapapi.dto.response.PageResponse;
import com.example.taiwanramenmapapi.dto.request.ShopOwnerSignUpRequest;
import com.example.taiwanramenmapapi.dto.request.UpdateShopRequest;
import com.example.taiwanramenmapapi.dto.response.ShopDTO;
import com.example.taiwanramenmapapi.dto.response.ShopMediaDTO;
import com.example.taiwanramenmapapi.entity.*;
import com.example.taiwanramenmapapi.entity.enums.Role;
import com.example.taiwanramenmapapi.exception.FileStorageException;
import com.example.taiwanramenmapapi.exception.GeocodingException;
import com.example.taiwanramenmapapi.exception.ResourceNotFoundException;
import com.example.taiwanramenmapapi.exception.UnauthorizedActionException;
import com.example.taiwanramenmapapi.mapper.ShopMapper;
import com.example.taiwanramenmapapi.mapper.ShopMediaMapper;
import com.example.taiwanramenmapapi.repository.*;
import com.example.taiwanramenmapapi.service.FileStorageService;
import com.example.taiwanramenmapapi.service.GeocodingService;
import com.example.taiwanramenmapapi.service.ShopService;
import com.google.maps.model.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ShopServiceImpl implements ShopService {

    private static final Logger logger = LoggerFactory.getLogger(ShopServiceImpl.class);

    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ShopMediaRepository shopMediaRepository;
    @Autowired private ReviewMediaRepository reviewMediaRepository;
    @Autowired private ShopMapper shopMapper;
    @Autowired private ShopMediaMapper shopMediaMapper;
    @Autowired private GeocodingService geocodingService;
    @Autowired private FileStorageService fileStorageService;

    @Value("${app.ranking.min-reviews:3}") private int minimumReviewsForRanking;
    @Value("${app.ranking.global-avg-rating:3.5}") private BigDecimal globalAverageRating;
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    @Override
    @Transactional
    public ShopDTO createShopWithOwner(ShopOwnerSignUpRequest request, User owner) {
        logger.info("Service: Creating shop '{}' for owner {} (ID: {})", request.getShopName(), owner.getUsername(), owner.getId());
        logger.debug("Step 1: Geocoding address '{}'", request.getAddress());
        LatLng location = geocodingService.getLatLngOrFail(request.getAddress());
        logger.debug("Step 1 Success: Geocoded to Lat={}, Lng={}", location.lat, location.lng);
        logger.debug("Step 2: Building Shop entity...");
        Shop shop = Shop.builder()
                .name(request.getShopName()).address(request.getAddress()).phone(request.getPhone())
                .openingHours(request.getOpeningHours()).description(request.getDescription())
                .latitude(BigDecimal.valueOf(location.lat).setScale(7, RoundingMode.HALF_UP))
                .longitude(BigDecimal.valueOf(location.lng).setScale(7, RoundingMode.HALF_UP))
                .owner(owner).averageRating(BigDecimal.ZERO).reviewCount(0).weightedRating(BigDecimal.ZERO)
                .build();
        logger.info("[CREATE-DEBUG] Before Save - Shop Built: Name=[{}], Address=[{}], Phone=[{}]", shop.getName(), shop.getAddress(), shop.getPhone());
        logger.debug("Step 3: Saving Shop entity...");
        Shop savedShop;
        try {
            savedShop = shopRepository.save(shop);
            logger.info("[CREATE-DEBUG] After Save - Shop Saved: ID={}, Name=[{}], Address=[{}], Phone=[{}]", savedShop.getId(), savedShop.getName(), savedShop.getAddress(), savedShop.getPhone());
        } catch (Exception e) { logger.error("Failed to save basic shop info: {}", e.getMessage(), e); throw new RuntimeException("Failed to save basic shop info", e); }
        if (request.getInitialPhotos() != null && !request.getInitialPhotos().isEmpty()) {
            logger.info("Step 4: Processing {} initial photos for shop ID: {}", request.getInitialPhotos().size(), savedShop.getId());
            try { uploadShopMediaInternal(savedShop, request.getInitialPhotos()); }
            catch (RuntimeException e) { logger.error("儲存店家 {} 的初始照片失敗: {}", savedShop.getId(), e.getMessage(), e); throw new RuntimeException("儲存店家初始照片失敗，註冊已回滾。", e); }
        } else { logger.debug("Step 4: No initial photos to process."); }
        logger.debug("Step 5: Mapping final state to DTO for shop ID: {}", savedShop.getId());
        Shop finalShopState = findShopByIdOrThrow(savedShop.getId());
        logger.info("[CREATE-DEBUG] Final State before DTO mapping: Name=[{}], Address=[{}], Phone=[{}], MediaCount={}", finalShopState.getName(), finalShopState.getAddress(), finalShopState.getPhone(), finalShopState.getMedia() != null ? finalShopState.getMedia().size() : 0);
        ShopDTO resultDTO = mapShopToDTOWithMedia(finalShopState); // 使用輔助方法映射
        logger.info("[CREATE-DEBUG] Mapped DTO: Name=[{}], Address=[{}], Phone=[{}], MediaCount={}", resultDTO.getName(), resultDTO.getAddress(), resultDTO.getPhone(), resultDTO.getMedia() != null ? resultDTO.getMedia().size() : 0);
        if (resultDTO.getMedia() != null && !resultDTO.getMedia().isEmpty()) { logger.info("[CREATE-DEBUG] Mapped DTO Media URL[0]: {}", resultDTO.getMedia().get(0).getUrl()); }
        return resultDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public ShopDTO getShopById(Long id) {
        logger.debug("Service: Getting shop by ID: {}", id);
        Shop shop = findShopByIdOrThrow(id);
        return mapShopToDTOWithMedia(shop);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShopDTO> getAllShops(Pageable pageable, String name, String address, String city) {
        logger.debug("Service: Getting shops - Pageable: {}, Name: '{}', Address: '{}', City: '{}'", pageable, name, address, city);
        Page<Shop> shopPage;
        if (StringUtils.hasText(name)) { shopPage = shopRepository.findByNameContainingIgnoreCase(name, pageable); }
        else if (StringUtils.hasText(city)) { shopPage = shopRepository.findByAddressContainingIgnoreCase(city, pageable); }
        else if (StringUtils.hasText(address)) { shopPage = shopRepository.findByAddressContainingIgnoreCase(address, pageable); }
        else { shopPage = shopRepository.findAll(pageable); }
        logger.info("Service: Found {} shops matching criteria.", shopPage.getTotalElements());
        Page<ShopDTO> dtoPage = shopPage.map(this::mapShopToDTOWithMedia);
        return new PageResponse<>(dtoPage);
    }

    @Override
    @Transactional
    public ShopDTO updateShop(Long id, UpdateShopRequest updateShopRequest) {
        logger.info("Service: Attempting to update shop ID: {}", id);
        Shop shop = findShopByIdOrThrow(id);
        User currentUser = getCurrentAuthenticatedUser();
        checkShopOwnershipOrAdmin(shop, currentUser);
        String oldAddress = shop.getAddress(); String oldName = shop.getName(); String oldPhone = shop.getPhone();
        logger.debug("[UPDATE-DEBUG] Before update: Name=[{}], Address=[{}], Phone=[{}]", oldName, oldAddress, oldPhone);
        logger.debug("Step 1: Applying updates from DTO...");
        shopMapper.updateShopFromDto(updateShopRequest, shop); // 只更新 DTO 中有的字段
        logger.info("[UPDATE-DEBUG] After MapStruct: Name=[{}], Address=[{}], Phone=[{}]", shop.getName(), shop.getAddress(), shop.getPhone());
        if (updateShopRequest.getAddress() != null && !updateShopRequest.getAddress().equals(oldAddress)) {
            logger.info("Step 2: Address changed, re-geocoding for shop ID: {}", id);
            try { LatLng l=geocodingService.getLatLngOrFail(updateShopRequest.getAddress()); shop.setLatitude(BigDecimal.valueOf(l.lat).setScale(7,RoundingMode.HALF_UP)); shop.setLongitude(BigDecimal.valueOf(l.lng).setScale(7,RoundingMode.HALF_UP)); logger.info("Step 2 Success: Re-geocoded to Lat={}, Lng={}",shop.getLatitude(),shop.getLongitude()); }
            catch (GeocodingException e) { logger.error("Re-geocoding failed: {}", id, e); throw new RuntimeException("Update failed: re-geocoding error", e); }
        } else { logger.debug("Step 2: Address not changed."); }
        logger.debug("Step 3: Saving updated Shop entity...");
        logger.info("[UPDATE-DEBUG] Before Save: Name=[{}], Address=[{}], Phone=[{}]", shop.getName(), shop.getAddress(), shop.getPhone());
        Shop updatedShop;
        try { updatedShop = shopRepository.save(shop); logger.info("Service: Shop ID: {} updated successfully in DB.", updatedShop.getId()); }
        catch (Exception e) { logger.error("Failed to save updated shop ID {}: {}", id, e.getMessage(), e); throw new RuntimeException("Failed to save updated shop", e); }
        logger.debug("Step 4: Mapping final state to DTO for shop ID: {}", updatedShop.getId());
        return mapShopToDTOWithMedia(updatedShop); // 返回映射後的 DTO
    }

    @Override
    @Transactional
    public void deleteShopByAdmin(Long id) {
        logger.warn("Service: Admin attempting to delete shop ID: {}", id);
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser.getRole() != Role.ROLE_ADMIN) { throw new UnauthorizedActionException("Only administrators can delete shops"); }
        Shop shop = findShopByIdOrThrow(id);
        deleteShopMediaFiles(shop); // 刪除店家媒體文件
        List<Review> reviewsToDelete = reviewRepository.findByShopId(shop.getId());
        for(Review review : reviewsToDelete) { deleteReviewMediaFilesInternal(review); } // 刪除評論媒體文件
        shopRepository.delete(shop); // 刪除店家及級聯數據
        logger.warn("Service: Shop ID: {} deleted by Admin {}", id, currentUser.getUsername());
    }

    @Override
    @Transactional
    public void updateShopRating(Long shopId) {
        logger.debug("Service: Updating rating for shop ID: {}", shopId);
        Shop shop = findShopByIdOrThrow(shopId);
        List<Integer> ratings = reviewRepository.findAllTopLevelRatingsByShopId(shopId);
        long reviewCount = reviewRepository.countByShopIdAndParentReviewIdIsNull(shopId);
        shop.setReviewCount((int)reviewCount);
        BigDecimal avgRating = calculateAverageRating(ratings);
        shop.setAverageRating(avgRating);
        BigDecimal weightedRating = calculateWeightedRating(avgRating, (int)reviewCount);
        shop.setWeightedRating(weightedRating);
        shopRepository.save(shop);
        logger.info("Service: Shop {} rating updated: Count={}, Avg={}, Weighted={}", shopId, reviewCount, avgRating, weightedRating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDTO> getTopRatedShops(int limit) {
        logger.debug("Service: Getting Top {} shops (Global)", limit);
        Pageable pageable = PageRequest.of(0, limit);
        Page<Shop> topShopsPage = shopRepository.findTopRatedShops(minimumReviewsForRanking, pageable);
        return topShopsPage.stream().map(this::mapShopToDTOWithMedia).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDTO> getTopRatedShopsByRegion(String region, int limit) {
        logger.debug("Service: Getting Top {} shops (Region: {})", limit, region);
        if (!StringUtils.hasText(region)) { return getTopRatedShops(limit); }
        Pageable pageable = PageRequest.of(0, limit);
        Page<Shop> topShopsPage = shopRepository.findTopRatedShopsByRegion(region, minimumReviewsForRanking, pageable);
        return topShopsPage.stream().map(this::mapShopToDTOWithMedia).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShopDTO> getShopsByBounds(BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng) {
        logger.debug("Service: Getting shops by bounds...");
        if (minLat == null || maxLat == null || minLng == null || maxLng == null) { return Collections.emptyList(); }
        List<Shop> shopsInBounds = shopRepository.findByLocationBounds(minLat, maxLat, minLng, maxLng);
        logger.info("Found {} shops within bounds.", shopsInBounds.size());
        return shopsInBounds.stream().map(this::mapShopToDTOWithMedia).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShopDTO uploadShopMedia(Long shopId, List<MultipartFile> files) {
        logger.info("Service: Attempting to upload {} files for shop ID: {}", files != null ? files.size() : 0, shopId);
        if (files == null || files.isEmpty()) { return getShopById(shopId); }
        Shop shop = findShopByIdOrThrow(shopId);
        User currentUser = getCurrentAuthenticatedUser();
        checkShopOwnershipOrAdmin(shop, currentUser);
        uploadShopMediaInternal(shop, files);
        Shop updatedShop = findShopByIdOrThrow(shopId);
        return mapShopToDTOWithMedia(updatedShop);
    }

    @Override
    @Transactional
    public void deleteShopMedia(Long shopId, Long mediaId) {
        logger.warn("Service: Attempting to delete media ID: {} for shop ID: {}", mediaId, shopId);
        Shop shop = findShopByIdOrThrow(shopId);
        User currentUser = getCurrentAuthenticatedUser();
        checkShopOwnershipOrAdmin(shop, currentUser);
        ShopMedia media = shopMediaRepository.findById(mediaId).orElseThrow(() -> new ResourceNotFoundException("ShopMedia", "id", mediaId));
        if (!Objects.equals(media.getShop().getId(), shopId)) { throw new UnauthorizedActionException("Media does not belong to this shop"); }
        try { fileStorageService.deleteFile(media.getUrl()); }
        catch (Exception e) { logger.error("Failed to delete shop media file: {}", media.getUrl(), e); }
        shopMediaRepository.delete(media);
        logger.info("Service: Media ID: {} for shop ID: {} deleted successfully.", mediaId, shopId);
    }


    // --- 私有輔助方法 ---

    /** 將 Shop 映射為 DTO，並手動處理 media URL */
    private ShopDTO mapShopToDTOWithMedia(Shop shop) {
        if (shop == null) return null;
        ShopDTO dto = shopMapper.toShopDTO(shop);
        if (shop.getMedia() != null && !shop.getMedia().isEmpty()) {
            List<ShopMediaDTO> mediaDTOs = shopMediaMapper.toShopMediaDTOs(shop.getMedia());
            dto.setMedia(mediaDTOs);
            logger.trace("Manually mapped media for Shop ID {}. Count: {}", shop.getId(), mediaDTOs.size());
        } else { dto.setMedia(Collections.emptyList()); }
        logger.trace("[MAPPER-CHECK] Final DTO: Name=[{}], Address=[{}], Phone=[{}], Media exist: {}", dto.getName(), dto.getAddress(), dto.getPhone(), dto.getMedia()!=null&&!dto.getMedia().isEmpty());
        return dto;
    }

    /** 內部處理店家媒體上傳 */
    private List<ShopMedia> uploadShopMediaInternal(Shop shop, List<MultipartFile> files) {
        List<ShopMedia> savedMediaList = new ArrayList<>();
        int displayOrder = shopMediaRepository.countByShopId(shop.getId());
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            try {
                String subDirectory = "shops/" + shop.getId();
                String storedFileName = fileStorageService.storeFile(file, subDirectory);
                ShopMedia shopMedia = ShopMedia.builder().shop(shop).url(storedFileName).type(determineMediaType(file.getContentType())).displayOrder(displayOrder++).build();
                savedMediaList.add(shopMediaRepository.save(shopMedia));
                logger.debug("Saved shop media record: {}", storedFileName);
            } catch (FileStorageException e) { logger.error("Failed to store media file {} for shop {}: {}", file.getOriginalFilename(), shop.getId(), e.getMessage()); throw new RuntimeException("Failed to store media file: " + file.getOriginalFilename(), e); }
        }
        return savedMediaList; // *** 返回保存的列表 ***
    }

    /** 內部刪除店家所有媒體文件 */
    private void deleteShopMediaFiles(Shop shop) { if(shop==null)return; logger.warn("Deleting all media files for shop ID: {}...", shop.getId()); List<ShopMedia> l=shopMediaRepository.findByShopIdOrderByDisplayOrderAsc(shop.getId()); deleteMediaFiles(l,"ShopMedia", shop.getId()); }
    /** 內部刪除評論所有媒體文件 */
    private void deleteReviewMediaFilesInternal(Review review) { if(review==null)return; logger.warn("Deleting all media files for review ID: {}...", review.getId()); List<ReviewMedia> l=reviewMediaRepository.findByReviewId(review.getId()); deleteMediaFiles(l,"ReviewMedia", review.getId()); }
    /** 通用媒體文件刪除邏輯 */
    private <T> void deleteMediaFiles(List<T> mediaList, String entityType, Long parentId) { if(mediaList==null||mediaList.isEmpty())return; int d=0; long t=mediaList.size(); for(T m:mediaList){String u=null;Long mid=null;try{if(m instanceof ShopMedia sm){u=sm.getUrl();mid=sm.getId();}else if(m instanceof ReviewMedia rm){u=rm.getUrl();mid=rm.getId();}}catch(ClassCastException e){logger.error("Media list type error",e);continue;}if(u!=null){try{if(fileStorageService.deleteFile(u)){d++;logger.debug("Deleted {} file (ParentID: {}, MediaID: {}, Path: {})",entityType,parentId,mid,u);}}catch(Exception e){logger.error("Error deleting {} file (ParentID: {}, MediaID: {}, Path: {}): {}",entityType,parentId,mid,u,e.getMessage());}}else{logger.warn("{} record (ID: {}) has null URL.",entityType,mid);}}logger.info("Attempted deletion for {} out of {} {} media files (Parent ID: {}).", d, t, entityType, parentId); }
    /** 判斷媒體類型 */
    private String determineMediaType(String contentType) { if(contentType!=null){if(contentType.startsWith("video"))return"video";if(contentType.startsWith("image"))return"image";} return "unknown"; }
    /** 查找店家或拋異常 */
    private Shop findShopByIdOrThrow(Long id) { return shopRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id)); }
    /** 計算平均分 */
    private BigDecimal calculateAverageRating(List<Integer> ratings) { if(ratings==null||ratings.isEmpty())return BigDecimal.ZERO; double avg=ratings.stream().mapToInt(i->i).average().orElse(0.0); return BigDecimal.valueOf(avg).setScale(2,RoundingMode.HALF_UP); }
    /** 計算加權評分 */
    private BigDecimal calculateWeightedRating(BigDecimal avgRating, int reviewCount) { return calculateWeightedRatingInternal(avgRating, reviewCount, minimumReviewsForRanking, globalAverageRating); }
    /** 內部加權評分計算 (已修正 return) */
    private BigDecimal calculateWeightedRatingInternal(BigDecimal R, int v, int m, BigDecimal C) {
        if (R == null || C == null || v < m) { logger.trace("加權評分計算跳過：R={}, v={}, m={}, C={}", R, v, m, C); return BigDecimal.ZERO; }
        try {
            BigDecimal vD = new BigDecimal(v), mD = new BigDecimal(m), vpm = vD.add(mD);
            if (vpm.compareTo(BigDecimal.ZERO) == 0) { logger.warn("加權評分計算錯誤：v+m 為零 (v={}, m={})", v, m); return BigDecimal.ZERO; }
            BigDecimal term1 = vD.divide(vpm, MC).multiply(R, MC);
            BigDecimal term2 = mD.divide(vpm, MC).multiply(C, MC);
            return term1.add(term2).setScale(7, RoundingMode.HALF_UP); // *** 返回計算結果 ***
        } catch (ArithmeticException e) { logger.error("計算加權評分時發生算術錯誤 for R={}, v={}, m={}, C={}", R, v, m, C, e); return BigDecimal.ZERO; }
    }
    /** 獲取當前認證用戶 */
    private User getCurrentAuthenticatedUser() { Authentication a=SecurityContextHolder.getContext().getAuthentication(); if(a==null||!a.isAuthenticated()||a.getPrincipal()==null||"anonymousUser".equals(a.getPrincipal())){throw new UnauthorizedActionException("用戶未登錄或認證無效");} Object p=a.getPrincipal(); if(p instanceof User)return(User)p; if(p instanceof org.springframework.security.core.userdetails.UserDetails){String u=((org.springframework.security.core.userdetails.UserDetails)p).getUsername(); return userRepository.findByUsername(u).orElseThrow(()->new ResourceNotFoundException("User","username (from context)",u));} throw new UnauthorizedActionException("無法識別的用戶信息: "+p.getClass().getName()); }
    /** 檢查店家擁有權或管理員權限 */
    private void checkShopOwnershipOrAdmin(Shop shop, User currentUser) { if(shop==null)throw new IllegalArgumentException("店家對象不能為空"); if(currentUser==null)throw new UnauthorizedActionException("無法獲取當前用戶信息"); User o=shop.getOwner(); boolean iA=currentUser.getRole()==Role.ROLE_ADMIN; boolean iO=o!=null&&Objects.equals(currentUser.getId(),o.getId()); logger.debug("權限檢查: UserID={}, Role={}, TargetShopID={}, OwnerID={}. IsOwner={}, IsAdmin={}",currentUser.getId(),currentUser.getRole(),shop.getId(),o!=null?o.getId():"null",iO,iA); if(!iA&&!iO){logger.warn("權限不足: User {} ({}) 無權操作 shop {}",currentUser.getUsername(),currentUser.getRole(),shop.getId());throw new UnauthorizedActionException("您沒有權限執行此操作");} logger.debug("權限檢查通過"); }

}