package org.crochet.service;

import org.crochet.model.User;
import org.crochet.payload.request.Filter;
import org.crochet.payload.request.SignUpRequest;
import org.crochet.payload.request.UserUpdateRequest;
import org.crochet.payload.response.UserPaginationResponse;
import org.crochet.payload.response.UserResponse;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    User createUser(SignUpRequest signUpRequest);

    UserPaginationResponse getAll(int pageNo, int pageSize, String sortBy, String sortDir,
                                  Filter[] filters);

    @Transactional
    void updateUser(UserUpdateRequest request);

    @Transactional
    void deleteUser(String id);

    User getByEmail(String email);

    UserResponse getDetail(String id);

    void updatePassword(String password, String email);

    void verifyEmail(String email);

    User checkLogin(String email, String password);
}
