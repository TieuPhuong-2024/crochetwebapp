package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.ResultCode;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.model.UserProfile;
import org.crochet.payload.request.UserProfileRequest;
import org.crochet.payload.response.CommentResponse;
import org.crochet.payload.response.UserProfileResponse;
import org.crochet.repository.CollectionRepo;
import org.crochet.repository.CommentRepository;
import org.crochet.repository.UserProfileRepo;
import org.crochet.repository.UserRepository;
import org.crochet.mapper.UserProfileMapper;
import org.crochet.service.UserProfileService;
import org.crochet.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import org.crochet.payload.request.ChangePasswordRequest;
import org.crochet.exception.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final CollectionRepo colRepo;
    private final CommentRepository commentRepo;
    private final UserProfileRepo userProfileRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Load user profile
     *
     * @return UserProfileResponse
     */
    @Override
    public UserProfileResponse loadUserProfile(String userId) {
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(ResultCode.MSG_USER_NOT_FOUND.message(),
                        ResultCode.MSG_USER_NOT_FOUND.code()));

        // Get collections by user id
        var collections = colRepo.getAllByUserId(user.getId());

        // Get recent comments by user id
        List<CommentResponse> recentComments = commentRepo.getRecentCommentsByUserId(user.getId());

        // Get user profile
        var userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = new UserProfile();
        }

        var response = UserProfileMapper.INSTANCE.toResponse(user, userProfile);
        response.setCollections(collections);
        response.setRecentComments(recentComments);
        return response;
    }

    /**
     * Update user profile
     *
     * @param request UserProfileRequest
     * @return UserProfileResponse
     */
    @Transactional
    @Override
    public UserProfileResponse updateUserProfile(UserProfileRequest request) {
        var user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException(ResultCode.MSG_USER_NOT_FOUND.message(),
                    ResultCode.MSG_USER_NOT_FOUND.code());
        }

        var userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = new UserProfile();
        }

        UserProfileMapper.INSTANCE.updateUserProfile(request, userProfile);
        UserProfileMapper.INSTANCE.updateUserInfo(request, user);
        user = userRepo.save(user);

        userProfile.setUser(user);
        userProfile = userProfileRepo.save(userProfile);

        return UserProfileMapper.INSTANCE.toResponse(user, userProfile);
    }

    /**
     * Change user password
     *
     * @param request ChangePasswordRequest
     */
    @Transactional
    @Override
    public void changePassword(ChangePasswordRequest request) {
        var user = SecurityUtils.getCurrentUser();
        if (user == null) {
            throw new ResourceNotFoundException(ResultCode.MSG_USER_NOT_FOUND.message(),
                    ResultCode.MSG_USER_NOT_FOUND.code());
        }

        var dbUser = userRepo.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ResultCode.MSG_USER_NOT_FOUND.message(),
                        ResultCode.MSG_USER_NOT_FOUND.code()));

        if (!passwordEncoder.matches(request.getOldPassword(), dbUser.getPassword())) {
            throw new BadRequestException(
                    ResultCode.MSG_INCORRECT_PASSWORD.message(),
                    ResultCode.MSG_INCORRECT_PASSWORD.code());
        }

        dbUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(dbUser);
    }
}
