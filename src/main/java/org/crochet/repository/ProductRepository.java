package org.crochet.repository;

import org.crochet.model.Product;
import org.crochet.payload.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {

    @Query("""
            SELECT
              new org.crochet.payload.response.ProductResponse (
                p.id,
                p.name,
                p.description,
                p.price,
                p.currencyCode,
                i.fileContent,
                p.viewCount,
                p.likeCount
              )
            FROM
              Product p
              LEFT JOIN p.images i
            WITH
              i.order = 0
            WHERE
              p.isHome = TRUE
            """)
    List<ProductResponse> findLimitedNumProduct(Pageable pageable);


    @Query("""
            SELECT
              p
            FROM
              Product p
              LEFT JOIN
            FETCH p.images
            JOIN
            FETCH p.category
            WHERE
              p.id = :id
            """)
    Optional<Product> findProductById(@Param("id") String id);

    @Query("""
            SELECT
              new org.crochet.payload.response.ProductResponse (
                p.id,
                p.name,
                p.description,
                p.price,
                p.currencyCode,
                i.fileContent,
                p.viewCount,
                p.likeCount
              )
            FROM
              Product p
              LEFT JOIN p.images i
            WITH
              i.order = 0
            WHERE
              p.id IN :ids
            """)
    Page<ProductResponse> findProductWithIds(@Param("ids") List<String> ids, Pageable pageable);

    @Query("""
            SELECT
              new org.crochet.payload.response.ProductResponse (
                p.id,
                p.name,
                p.description,
                p.price,
                p.currencyCode,
                i.fileContent,
                p.viewCount,
                p.likeCount
              )
            FROM
              Product p
              LEFT JOIN p.images i
            WITH
              i.order = 0
            """)
    Page<ProductResponse> findProductWithPageable(Pageable pageable);

    @Query("""
            SELECT
              p.id
            FROM
              Product p
            ORDER BY
              p.createdDate DESC
            """)
    List<String> getProductIds(Pageable pageable);

    @Modifying
    @Query("delete from Product p where p.id in :ids")
    void deleteMultiple(@Param("ids") List<String> ids);

    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    int incrementViewCount(@Param("id") String id);

    @Modifying
    @Query("UPDATE Product p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    int incrementLikeCount(@Param("id") String id);

    @Modifying
    @Query("UPDATE Product p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :id")
    int decrementLikeCount(@Param("id") String id);
}
