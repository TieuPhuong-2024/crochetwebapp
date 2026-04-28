package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.ResultCode;
import org.crochet.exception.IllegalArgumentException;
import org.crochet.mapper.CategoryMapper;
import org.crochet.model.Category;
import org.crochet.payload.request.CategoryCreationRequest;
import org.crochet.payload.request.CategoryUpdateRequest;
import org.crochet.payload.response.CategoryResponse;
import org.crochet.repository.CategoryRepo;
import org.crochet.service.CategoryService;
import org.crochet.service.PermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepo categoryRepo;
    private final PermissionService permissionService;

    /**
     * Create a new category or multiple categories
     *
     * @param request the request object containing the parent IDs and category name
     * @return a list of CategoryResponse objects
     */
    @Transactional
    @Override
    public List<CategoryResponse> create(CategoryCreationRequest request) {
        String name = request.getName();

        // Check if the category already exists as a root (parent) category
        if (categoryRepo.existsByNameAndParentIsNull(name)) {
            throw new IllegalArgumentException(
                    ResultCode.ERROR_PARENT_CATEGORY_EXISTS.message(),
                    ResultCode.ERROR_PARENT_CATEGORY_EXISTS.code()
            );
        }

        // Fetch parent categories with parent pre-fetched to avoid N+1 in circular
        // reference check
        List<Category> parents = categoryRepo.findAllByIdWithParent(request.getParentIds());

        // Use HashSet for O(1) lookup instead of ArrayList O(N)
        Set<String> invalidParentIds = new HashSet<>(
                categoryRepo.findParentIdsDirectlyHavingChildName(name, request.getParentIds()));

        List<Category> children = new ArrayList<>();

        if (parents.isEmpty()) {
            // If no parents are provided, create a root category
            if (categoryRepo.existsByNameAndParentIsNotNull(name)) {
                throw new IllegalArgumentException(
                        ResultCode.ERROR_CHILD_CATEGORY_EXISTS.message(),
                        ResultCode.ERROR_CHILD_CATEGORY_EXISTS.code()
                );
            }
            Category category = new Category();
            category.setName(name);
            children.add(category);
        } else {
            for (Category parent : parents) {
                if (invalidParentIds.contains(parent.getId()))
                    continue;

                // Check for circular reference (parent already pre-fetched, no N+1)
                if (isCircularReference(parent, name)) {
                    continue;
                }

                // Create a new child category under this parent
                Category category = new Category();
                category.setName(name);
                category.setParent(parent);
                children.add(category);
            }
        }

        // If no children were created, throw an error
        if (children.isEmpty()) {
            throw new IllegalArgumentException(
                    ResultCode.MSG_DUPLICATE_CATEGORY_NAME_UNDER_PROVIDED_PARENTS.message(),
                    ResultCode.MSG_DUPLICATE_CATEGORY_NAME_UNDER_PROVIDED_PARENTS.code()
            );
        }

        // Save all new categories to the database
        List<Category> savedCategories = categoryRepo.saveAll(children);

        // Map child categories to CategoryResponse objects and return them
        return CategoryMapper.INSTANCE.toResponses(savedCategories);
    }

    /**
     * Iterative method to detect circular references.
     * Parents are pre-fetched via LEFT JOIN FETCH, so traversal does not trigger
     * additional LAZY load queries.
     *
     * @param parent the parent category (with parent chain pre-loaded)
     * @param name   the name of the category being created
     * @return true if a circular reference is detected, false otherwise
     */
    private boolean isCircularReference(Category parent, String name) {
        Category current = parent;
        while (current != null) {
            if (current.getName().equals(name)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    /**
     * Update a category
     *
     * @param request the request object containing the category ID and new name
     * @return a CategoryResponse object
     */
    @Transactional
    @Override
    public CategoryResponse update(CategoryUpdateRequest request) {
        // Find the existing category by ID
        Category category = findById(request.getId());

        permissionService.checkUserPermission(category, "update");

        // Extract the new name from the request
        String newName = request.getName();

        // Check if the new name already exists as a parent category (excluding current
        // if it is root)
        if (categoryRepo.existsRootByNameAndIdNot(newName, category.getId())) {
            throw new IllegalArgumentException(
                    ResultCode.ERROR_PARENT_CATEGORY_EXISTS.message(),
                    ResultCode.ERROR_PARENT_CATEGORY_EXISTS.code()
            );
        }

        // Check if the new name already exists as a sibling category under this parent
        if (category.getParent() != null && categoryRepo.existsSiblingByName(newName,
                category.getParent().getId(), category.getId())) {
            throw new IllegalArgumentException(
                    ResultCode.ERROR_CHILD_CATEGORY_EXISTS.message(),
                    ResultCode.ERROR_CHILD_CATEGORY_EXISTS.code()
            );
        }

        // Update the category's name
        category.setName(newName);

        // Save the updated category
        category = categoryRepo.save(category);

        // Map the updated category to a response object and return it
        return CategoryMapper.INSTANCE.toResponse(category);
    }

    /**
     * Get all categories (root categories with children pre-fetched)
     *
     * @return a list of CategoryResponse objects
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        var parentCategories = categoryRepo.getRootCategories();
        return CategoryMapper.INSTANCE.toResponses(parentCategories);
    }

    /**
     * Get a category by ID
     *
     * @param id the category ID
     * @return a CategoryResponse object
     */
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(String id) {
        var category = findById(id);
        return CategoryMapper.INSTANCE.toResponse(category);
    }

    /**
     * Get all child categories of a parent category
     *
     * @param id the parent category ID
     * @return a list of CategoryResponse objects
     */
    @Override
    @Transactional(readOnly = true)
    public Category findById(String id) {
        return categoryRepo.findCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        ResultCode.MSG_CATEGORY_NOT_FOUND.message(),
                        ResultCode.MSG_CATEGORY_NOT_FOUND.code()
                ));
    }

    /**
     * Delete a category
     *
     * @param id the category ID
     */
    @Transactional
    @Override
    public void delete(String id) {
        var category = findById(id);
        permissionService.checkUserPermission(category, "delete");
        categoryRepo.delete(category);
    }
}
