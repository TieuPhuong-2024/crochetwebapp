package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.ResultCode;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.mapper.FreePatternMapper;
import org.crochet.model.Heart;
import org.crochet.model.User;
import org.crochet.payload.response.FreePatternResponse;
import org.crochet.payload.response.PaginationResponse;
import org.crochet.repository.FreePatternRepository;
import org.crochet.repository.HeartRepository;
import org.crochet.service.HeartService;
import org.crochet.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeartServiceImpl implements HeartService {
    private final HeartRepository heartRepository;
    private final FreePatternRepository freePatternRepository;
    
    @Transactional
    @Override
    public boolean toggleHeart(String freePatternId) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException(
                    ResultCode.MSG_USER_LOGIN_REQUIRED.message(),
                    ResultCode.MSG_USER_LOGIN_REQUIRED.code()
            );
        }
        
        // Kiểm tra free pattern có tồn tại không
        var freePattern = freePatternRepository.findById(freePatternId).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.message(),
                        ResultCode.MSG_FREE_PATTERN_NOT_FOUND.code()
                )
        );
        
        // Kiểm tra xem user đã thả tim free pattern này chưa
        Optional<Heart> existingHeart = heartRepository.findByFreePatternIdAndUserId(
                freePatternId, currentUser.getId());
        
        if (existingHeart.isPresent()) {
            // Nếu đã tồn tại, xóa heart (bỏ tim)
            heartRepository.delete(existingHeart.get());
            return false; // Đã bỏ tim
        } else {
            // Tạo heart mới (thả tim)
            Heart heart = Heart.builder()
                    .freePattern(freePattern)
                    .user(currentUser)
                    .build();
            
            heartRepository.save(heart);
            return true; // Đã thả tim
        }
    }
    
    @Override
    public boolean isHearted(String freePatternId, String userId) {
        return heartRepository.existsByFreePatternIdAndUserId(freePatternId, userId);
    }
    
    @Override
    public int countHearts(String freePatternId) {
        return (int) heartRepository.countByFreePatternId(freePatternId);
    }
    
    @Override
    public PaginationResponse<FreePatternResponse> getHeartedFreePatterns(String userId, int pageNo, int pageSize) {
        Pageable pageable = PageRequest.of(pageNo, pageSize);
        Page<Heart> heartPage = heartRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable);
        
        List<FreePatternResponse> freePatternResponses = heartPage.getContent().stream()
                .map(heart -> {
                    FreePatternResponse response = FreePatternMapper.INSTANCE.toResponse(heart.getFreePattern());
                    // Đánh dấu là đã thả tim
                    response.setIsHearted(true);
                    return response;
                })
                .collect(Collectors.toList());
        
        return PaginationResponse.<FreePatternResponse>builder()
                .contents(freePatternResponses)
                .pageNo(pageNo)
                .pageSize(pageSize)
                .totalElements(heartPage.getTotalElements())
                .totalPages(heartPage.getTotalPages())
                .last(heartPage.isLast())
                .build();
    }
    
    @Override
    public List<String> getHeartedFreePatternIds(String userId) {
        return heartRepository.findFreePatternIdsByUserId(userId);
    }
    
    @Override
    public void getHeartInfoForFreePattern(String freePatternId, FreePatternResponse response) {
        // Đếm số lượng tim
        int heartCount = countHearts(freePatternId);
        response.setHeartCount(heartCount);
        
        // Kiểm tra xem người dùng hiện tại đã thả tim chưa
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            boolean isHearted = isHearted(freePatternId, currentUser.getId());
            response.setIsHearted(isHearted);
        } else {
            response.setIsHearted(false);
        }
    }
} 