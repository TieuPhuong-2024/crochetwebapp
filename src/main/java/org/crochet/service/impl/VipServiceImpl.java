package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.crochet.enums.ResultCode;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.User;
import org.crochet.payload.request.VipSubscriptionRequest;
import org.crochet.repository.UserRepository;
import org.crochet.service.VipService;
import org.crochet.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VipServiceImpl implements VipService {
    
    private final UserRepository userRepository;
    
    @Transactional
    @Override
    public boolean subscribeVip(VipSubscriptionRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException(ResultCode.MSG_USER_NOT_FOUND.message(),
                    ResultCode.MSG_USER_NOT_FOUND.code());
        }
        
        // Tính toán ngày hết hạn VIP
        LocalDateTime expiryDate;
        if (currentUser.isVip() && currentUser.getVipExpiryDate() != null && 
                currentUser.getVipExpiryDate().isAfter(LocalDateTime.now())) {
            // Nếu đã là VIP và chưa hết hạn, gia hạn từ ngày hết hạn hiện tại
            expiryDate = currentUser.getVipExpiryDate().plusMonths(request.getMonths());
        } else {
            // Nếu chưa là VIP hoặc đã hết hạn, tính từ hiện tại
            expiryDate = LocalDateTime.now().plusMonths(request.getMonths());
        }
        
        // Cập nhật trạng thái VIP
        currentUser.setVip(true);
        currentUser.setVipExpiryDate(expiryDate);
        
        userRepository.save(currentUser);
        log.info("User {} subscribed to VIP until {}", currentUser.getEmail(), expiryDate);
        
        return true;
    }
    
    @Override
    public boolean isCurrentUserVip() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        
        return isUserVipInternal(currentUser);
    }
    
    @Override
    public boolean isUserVip(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ResultCode.MSG_USER_NOT_FOUND.message(),
                        ResultCode.MSG_USER_NOT_FOUND.code()));
        
        return isUserVipInternal(user);
    }
    
    /**
     * Kiểm tra xem người dùng có phải là VIP hợp lệ không
     * @param user Đối tượng người dùng
     * @return true nếu người dùng là VIP và chưa hết hạn
     */
    private boolean isUserVipInternal(User user) {
        if (!user.isVip()) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (user.getVipExpiryDate() == null || user.getVipExpiryDate().isBefore(now)) {
            // VIP đã hết hạn, cập nhật trạng thái
            if (user.isVip()) {
                user.setVip(false);
                userRepository.save(user);
                log.info("User {} VIP subscription expired", user.getEmail());
            }
            return false;
        }
        
        return true;
    }
} 