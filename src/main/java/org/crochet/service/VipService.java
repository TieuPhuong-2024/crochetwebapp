package org.crochet.service;

import org.crochet.payload.request.VipSubscriptionRequest;

public interface VipService {
    /**
     * Đăng ký tài khoản VIP cho người dùng hiện tại
     * @param request Thông tin đăng ký VIP
     * @return Trạng thái thành công
     */
    boolean subscribeVip(VipSubscriptionRequest request);
    
    /**
     * Kiểm tra xem người dùng hiện tại có VIP không
     * @return true nếu người dùng là VIP, false nếu không
     */
    boolean isCurrentUserVip();
    
    /**
     * Kiểm tra xem người dùng có ID cụ thể có VIP không
     * @param userId ID của người dùng
     * @return true nếu người dùng là VIP, false nếu không
     */
    boolean isUserVip(String userId);
} 