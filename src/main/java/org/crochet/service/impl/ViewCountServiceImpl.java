package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.model.FreePattern;
import org.crochet.service.FreePatternService;
import org.crochet.service.ViewCountService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ViewCountServiceImpl implements ViewCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private final FreePatternService freePatternService;

    @Transactional
    @Override
    public void incrementViewCount(String patternId, String ipAddress) {
        String key = "pattern:" + patternId + ":ip:" + ipAddress;

        if (!redisTemplate.hasKey(key)) {
            redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);

            FreePattern freePattern = freePatternService.findById(patternId);
            freePattern.setViewCount(freePattern.getViewCount() + 1);
            freePatternService.save(freePattern);
        }
    }
}
