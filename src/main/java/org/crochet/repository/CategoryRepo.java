package org.crochet.repository;

import org.crochet.model.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepo extends JpaRepository<Category, String> {

  @Query("""
      SELECT
        (count(c.id) > 0)
      FROM
        Category c
      WHERE
        c.name = :name
        AND c.parent IS NULL
      """)
  boolean existsByNameAndParentIsNull(@Param("name") String name);

  @Query("""
      SELECT
        (count(c.id) > 0)
      FROM
        Category c
      WHERE
        c.name = :name
        AND c.parent IS NOT NULL
      """)
  boolean existsByNameAndParentIsNotNull(@Param("name") String name);

  @EntityGraph(attributePaths = { "children" })
  @Query("select c from Category c")
  List<Category> getCategories();

  @EntityGraph(attributePaths = { "children" })
  @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
  List<Category> getRootCategories();

  @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.id IN :ids")
  List<Category> findAllByIdWithParent(@Param("ids") List<String> ids);

  @EntityGraph(attributePaths = { "children" })
  @Query("""
      SELECT
        c
      FROM
        Category c
      WHERE
        c.id = :id
      """)
  Optional<Category> findCategoryById(@Param("id") String id);

  @Query("SELECT c.parent.id FROM Category c WHERE c.name = :name AND c.parent.id IN :parentIds")
  List<String> findParentIdsDirectlyHavingChildName(@Param("name") String name,
      @Param("parentIds") List<String> parentIds);

  @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND c.parent IS NULL AND c.id <> :id")
  boolean existsRootByNameAndIdNot(@Param("name") String name, @Param("id") String id);

  @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND c.parent.id = :parentId AND c.id <> :id")
  boolean existsSiblingByName(@Param("name") String name, @Param("parentId") String parentId, @Param("id") String id);
}