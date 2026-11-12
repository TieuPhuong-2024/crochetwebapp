package org.crochet.service.impl;

import org.crochet.model.BannerType;
import org.crochet.model.User;
import org.crochet.model.UserProfile;
import org.crochet.payload.request.BannerRequest;
import org.crochet.payload.request.UserProfileRequest;
import org.crochet.repository.BannerRepo;
import org.crochet.repository.BannerTypeRepo;
import org.crochet.repository.CollectionRepo;
import org.crochet.repository.CommentRepository;
import org.crochet.repository.UserProfileRepo;
import org.crochet.repository.UserRepository;
import org.crochet.service.UploadLifecycleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadReferencePersistenceTest {
    @Mock private UploadLifecycleService uploads;
    @Mock private BannerRepo banners;
    @Mock private BannerTypeRepo bannerTypes;
    @Mock private CollectionRepo collections;
    @Mock private CommentRepository comments;
    @Mock private UserProfileRepo profiles;
    @Mock private UserRepository users;
    @Mock private PasswordEncoder passwords;
    @InjectMocks private BannerServiceImpl bannerService;
    @InjectMocks private UserProfileServiceImpl profileService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bannerPreservesKeyUrlPairWhenAttaching() {
        var request = new BannerRequest();
        request.setBannerTypeId("banner-type");
        request.setFileName("development/banner/uncategorized/image.jpg");
        request.setFileContent("https://cdn.test/development/banner/uncategorized/image.jpg");
        request.setContent("<img src='https://cdn.test/embedded.jpg'>");
        when(banners.findAll()).thenReturn(new ArrayList<>());
        when(bannerTypes.findById("banner-type")).thenReturn(Optional.of(new BannerType()));
        when(banners.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        bannerService.batchInsertOrUpdate(List.of(request));

        ArgumentCaptor<UploadLifecycleService.References> references =
                ArgumentCaptor.forClass(UploadLifecycleService.References.class);
        var order = inOrder(uploads, banners);
        order.verify(uploads).attach(references.capture());
        order.verify(banners).deleteAll(anyList());
        order.verify(banners).saveAll(anyList());
        assertThat(references.getValue().files()).singleElement().satisfies(file -> {
            assertThat(file.getFileName()).isEqualTo(request.getFileName());
            assertThat(file.getFileContent()).isEqualTo(request.getFileContent());
        });
        assertThat(references.getValue().contents()).containsExactly(request.getContent());
    }

    @Test
    void profileAttachesBothUrlsBeforeEitherEntityIsSaved() {
        var user = User.builder().id("owner").imageUrl("https://cdn.test/old.jpg").build();
        var profile = new UserProfile();
        profile.setBackgroundImageUrl("https://cdn.test/background.jpg");
        user.setUserProfile(profile);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var request = new UserProfileRequest();
        request.setImageUrl("https://cdn.test/new.jpg");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        when(profiles.save(any())).thenAnswer(call -> call.getArgument(0));

        var response = profileService.updateUserProfile(request);

        var order = inOrder(uploads, users, profiles);
        ArgumentCaptor<UploadLifecycleService.References> references =
                ArgumentCaptor.forClass(UploadLifecycleService.References.class);
        order.verify(uploads).attach(references.capture());
        order.verify(users).save(user);
        order.verify(profiles).save(profile);
        assertThat(references.getValue().urls())
                .containsExactly("https://cdn.test/new.jpg", "https://cdn.test/background.jpg");
        assertThat(response.getImageUrl()).isEqualTo(request.getImageUrl());
        assertThat(response.getBackgroundImageUrl()).isEqualTo(profile.getBackgroundImageUrl());
    }
}
