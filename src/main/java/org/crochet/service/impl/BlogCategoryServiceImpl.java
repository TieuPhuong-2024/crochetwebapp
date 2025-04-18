package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.ResultCode;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.mapper.BlogCategoryMapper;
import org.crochet.model.BlogCategory;
import org.crochet.payload.request.BlogCategoryRequest;
import org.crochet.payload.response.BlogCategoryResponse;
import org.crochet.repository.BlogCategoryRepo;
import org.crochet.service.BlogCategoryService;
import org.crochet.service.PermissionService;
import org.crochet.util.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogCategoryServiceImpl implements BlogCategoryService {
    private final BlogCategoryRepo blogCategoryRepo;
    private final PermissionService permissionService;

    @Transactional
    @Override
    public void createOrUpdate(BlogCategoryRequest request) {
        BlogCategory blogCategory;
        if (!ObjectUtils.hasText(request.getId())) {
            blogCategory = new BlogCategory();
        } else {
            blogCategory = getById(request.getId());
            permissionService.checkUserPermission(blogCategory, "update");
        }
        blogCategory.setName(request.getName());
        blogCategoryRepo.save(blogCategory);
    }

    /**
     * Get a blog category detail
     *
     * @param id the blog category id
     * @return the response object
     */
    @Override
    public BlogCategoryResponse getDetail(String id) {
        BlogCategory blogCategory = getById(id);
        return BlogCategoryMapper.INSTANCE.toResponse(blogCategory);
    }

    /**
     * Get all blog categories
     *
     * @return the list of blog categories
     */
    @Override
    public List<BlogCategoryResponse> getAll() {
        List<BlogCategory> blogCategories = blogCategoryRepo.findAll();
        return BlogCategoryMapper.INSTANCE.toResponses(blogCategories);
    }

    /**
     * Delete a blog category
     *
     * @param id the blog category id
     */
    @Transactional
    @Override
    public void delete(String id) {
        BlogCategory blogCategory = getById(id);
        permissionService.checkUserPermission(blogCategory, "delete");
        blogCategoryRepo.delete(blogCategory);
    }

    /**
     * Get a blog category by id
     *
     * @param id the blog category id
     * @return the blog category
     */
    @Override
    public BlogCategory getById(String id) {
        return blogCategoryRepo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException(
                        ResultCode.MSG_BLOG_CATEGORY_NOT_FOUND.message(),
                        ResultCode.MSG_BLOG_CATEGORY_NOT_FOUND.code()
                )
        );
    }
}
