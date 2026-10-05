package org.crochet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;

import org.crochet.mapper.FreePatternMapper;
import org.crochet.model.File;
import org.crochet.model.FreePattern;
import org.crochet.payload.request.FreePatternRequest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class FreePatternMapperTest {
    private final FreePatternMapper mapper = FreePatternMapper.INSTANCE;

    @Test
    void testUpdate_ShouldOnlyUpdateNonNullFields() {
        FreePattern entity = new FreePattern();
        entity.setName("Old Name");
        entity.setDescription("Old Description");

        FreePatternRequest req = new FreePatternRequest();
        req.setName("New Name");
        req.setDescription(null);

        mapper.update(req, entity);
        assertEquals("New Name", entity.getName());
        assertEquals("Old Description", entity.getDescription());
    }

    @Test
    void testUpdate_ShouldClearImages_WhenImagesIsEmpty() {
        FreePattern entity = new FreePattern();
        entity.setImages(new HashSet<>(List.of(new File())));

        FreePatternRequest req = new FreePatternRequest();
        req.setImages(Collections.emptyList());

        mapper.update(req, entity);

        assertNull(entity.getImages());
    }
}
