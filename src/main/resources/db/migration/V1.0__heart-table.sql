CREATE TABLE IF NOT EXISTS heart (
    id VARCHAR(50) NOT NULL PRIMARY KEY,
    created_by VARCHAR(50),
    created_date DATETIME NOT NULL,
    last_modified_by VARCHAR(50),
    last_modified_date DATETIME NOT NULL,
    free_pattern_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    CONSTRAINT fk_heart_free_pattern FOREIGN KEY (free_pattern_id) REFERENCES free_pattern (id) ON DELETE CASCADE,
    CONSTRAINT fk_heart_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT uk_heart_free_pattern_user UNIQUE (free_pattern_id, user_id)
); 