-- Thêm cột product_id và free_pattern_id vào bảng comment
ALTER TABLE comment
    ADD COLUMN product_id VARCHAR(50) NULL,
    ADD COLUMN free_pattern_id VARCHAR(50) NULL;

-- Thay đổi post_id thành nullable
ALTER TABLE comment
    MODIFY COLUMN post_id VARCHAR(50) NULL;

-- Thêm các foreign key constraints
ALTER TABLE comment
    ADD CONSTRAINT fk_comment_product
        FOREIGN KEY (product_id)
            REFERENCES product (id)
            ON DELETE CASCADE;

ALTER TABLE comment
    ADD CONSTRAINT fk_comment_free_pattern
        FOREIGN KEY (free_pattern_id)
            REFERENCES free_pattern (id)
            ON DELETE CASCADE; 