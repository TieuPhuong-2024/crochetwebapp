package org.crochet.repository;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.crochet.model.Category;
import org.crochet.model.Product;
import org.springframework.data.jpa.domain.Specification;

@SuppressWarnings({"DataFlowIssue", "unused"})
public class ProductSpecifications {

    public static Specification<Product> fetchJoin() {
        return (r, q, cb) -> {
            if (Long.class != q.getResultType()) {
                r.fetch("images", JoinType.LEFT);
            }
            q.distinct(true);
            return cb.conjunction();
        };
    }

    public static Specification<Product> inHierarchy(String categoryId) {
        return (r, q, cb) -> {
            Subquery<String> subquery = q.subquery(String.class);
            Root<Category> subRoot = subquery.from(Category.class);

            Predicate or = cb.or(cb.equal(subRoot.get("id"), categoryId),
                    cb.equal(subRoot.get("parent").get("id"), categoryId));
            subquery.select(subRoot.get("id"))
                    .where(or);

            q.distinct(true);

            return cb.in(r.get("category").get("id")).value(subquery);
        };
    }
}
