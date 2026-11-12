package org.crochet.service.impl;

import org.crochet.exception.BadRequestException;
import org.crochet.model.Category;
import org.crochet.model.File;
import org.crochet.model.FreePattern;
import org.crochet.payload.request.FreePatternRequest;
import org.crochet.payload.response.FileResponse;
import org.crochet.repository.ColFrepRepo;
import org.crochet.repository.CommentRepository;
import org.crochet.repository.FreePatternRepoCustom;
import org.crochet.repository.FreePatternRepository;
import org.crochet.repository.LikeRepository;
import org.crochet.service.CategoryService;
import org.crochet.service.PermissionService;
import org.crochet.service.UploadLifecycleService;
import org.crochet.service.UserService;
import org.crochet.util.SettingsUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreePatternUploadIntegrationTest {
    @Mock private FreePatternRepository repository;
    @Mock private SettingsUtil settingsUtil;
    @Mock private FreePatternRepoCustom customRepository;
    @Mock private PermissionService permissionService;
    @Mock private CategoryService categoryService;
    @Mock private UserService userService;
    @Mock private CommentRepository comments;
    @Mock private ColFrepRepo collections;
    @Mock private LikeRepository likes;
    @Mock private UploadLifecycleService uploads;
    @InjectMocks private FreePatternServiceImpl service;

    @Test
    void attachesImagesDocumentsAndRichTextBeforeSaving() {
        var request = new FreePatternRequest();
        request.setCategoryId("category");
        request.setImages(List.of(new FileResponse("image-key", "https://cdn.test/image.jpg")));
        request.setFiles(List.of(new FileResponse("document-key", "https://cdn.test/pattern.pdf")));
        request.setContent("<img src='https://cdn.test/embedded.jpg'>");
        request.setDescription("Description");
        when(categoryService.findById("category")).thenReturn(new Category());

        service.createOrUpdate(request);

        var entity = ArgumentCaptor.forClass(FreePattern.class);
        verify(repository).save(entity.capture());
        var references = ArgumentCaptor.forClass(UploadLifecycleService.References.class);
        var order = inOrder(uploads, repository);
        order.verify(uploads).attach(references.capture());
        order.verify(repository).save(entity.getValue());
        assertThat(references.getValue().files()).extracting(File::getFileName)
                .containsExactly("image-key", "document-key");
        assertThat(references.getValue().contents()).containsExactly(request.getContent(), "Description");
        assertThat(entity.getValue().getImages()).extracting(File::getFileName).containsExactly("image-key");
        assertThat(entity.getValue().getFiles()).extracting(File::getFileName).containsExactly("document-key");
    }

    @Test
    void partialUpdateAttachesEffectiveReferencesRatherThanNullRequestFields() {
        var existing = existingPattern();
        var request = new FreePatternRequest();
        request.setId("pattern");
        request.setName("Renamed");
        when(repository.findById("pattern")).thenReturn(Optional.of(existing));

        service.createOrUpdate(request);

        var references = ArgumentCaptor.forClass(UploadLifecycleService.References.class);
        verify(uploads).attach(references.capture());
        assertThat(references.getValue().files()).extracting(File::getFileName).containsExactly("existing-key");
        assertThat(references.getValue().contents()).containsExactly("retained content");
        assertThat(existing.getImages()).hasSize(1);
    }

    @Test
    void clearedImagesAreNotReattached() {
        var existing = existingPattern();
        var request = new FreePatternRequest();
        request.setId("pattern");
        request.setImages(List.of());
        when(repository.findById("pattern")).thenReturn(Optional.of(existing));

        service.createOrUpdate(request);

        assertThat(existing.getImages()).isNull();
        var references = ArgumentCaptor.forClass(UploadLifecycleService.References.class);
        verify(uploads).attach(references.capture());
        assertThat(references.getValue().files()).isEmpty();
    }

    @Test
    void attachmentFailurePreventsDomainSave() {
        var request = new FreePatternRequest();
        request.setCategoryId("category");
        request.setImages(List.of(new FileResponse("expired-key", "https://cdn.test/expired.jpg")));
        when(categoryService.findById("category")).thenReturn(new Category());
        doThrow(new BadRequestException("Upload expired")).when(uploads).attach(any());

        assertThatThrownBy(() -> service.createOrUpdate(request))
                .isInstanceOf(BadRequestException.class);

        verify(repository, never()).save(any());
    }

    private FreePattern existingPattern() {
        var image = new File();
        image.setFileName("existing-key");
        image.setFileContent("https://cdn.test/existing.jpg");
        var pattern = new FreePattern();
        pattern.setId("pattern");
        pattern.setImages(new HashSet<>(List.of(image)));
        pattern.setContent("retained content");
        return pattern;
    }
}
