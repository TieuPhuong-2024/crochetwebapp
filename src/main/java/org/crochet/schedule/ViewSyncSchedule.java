package org.crochet.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewSyncSchedule {

    private final StringRedisTemplate stringRedisTemplate;
    private final JdbcTemplate jdbcTemplate;

    private static final String REDIS_KEY_PREFIX = "pageviews:free-patterns:";
    private static final int SCAN_COUNT = 100;

    @Scheduled(fixedDelay = 300_000) // 5 minutes
    public void syncViewCounts() {
        log.info("Starting view count sync from Redis to database");

        Map<String, Long> viewCounts = new HashMap<>();
        List<String> keysToDelete = new ArrayList<>();

        try {
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(REDIS_KEY_PREFIX + "*")
                    .count(SCAN_COUNT)
                    .build();

            try (RedisConnection connection = Objects.requireNonNull(
                    stringRedisTemplate.getConnectionFactory()).getConnection();
                 Cursor<byte[]> cursor = connection.keyCommands().scan(scanOptions)) {
                if (cursor == null || !cursor.hasNext()) {
                    log.info("No view count keys found in Redis");
                    return;
                }

                while (cursor.hasNext()) {
                    byte[] keyBytes = cursor.next();
                    String key = new String(keyBytes);
                    String slug = key.substring(REDIS_KEY_PREFIX.length());

                    String value = stringRedisTemplate.opsForValue().get(key);
                    if (value != null) {
                        try {
                            long views = Long.parseLong(value);
                            if (views > 0) {
                                viewCounts.put(slug, views);
                                keysToDelete.add(key);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid view count value for key {}: {}", key, value);
                        }
                    }
                }
            }

            if (viewCounts.isEmpty()) {
                log.info("No view counts to sync");
                return;
            }

            batchUpdateViewCounts(viewCounts);
            stringRedisTemplate.delete(keysToDelete);

            log.info("Synced {} view counts to database", viewCounts.size());
        } catch (Exception e) {
            log.error("Error syncing view counts", e);
        }
    }

    private void batchUpdateViewCounts(Map<String, Long> viewCounts) {
        String sql = "UPDATE free_pattern SET view_count = view_count + ? WHERE id = ?";
        List<Object[]> batchArgs = new ArrayList<>();

        for (Map.Entry<String, Long> entry : viewCounts.entrySet()) {
            batchArgs.add(new Object[]{entry.getValue(), entry.getKey()});
        }

        try {
            int[] updateCounts = jdbcTemplate.batchUpdate(sql, batchArgs);
            int totalUpdated = 0;
            for (int count : updateCounts) {
                if (count != java.sql.Statement.EXECUTE_FAILED) {
                    totalUpdated += count;
                }
            }
            log.info("Updated {} rows in database", totalUpdated);
        } catch (DataAccessException e) {
            log.error("Error batch updating view counts", e);
            throw e;
        }
    }
}
