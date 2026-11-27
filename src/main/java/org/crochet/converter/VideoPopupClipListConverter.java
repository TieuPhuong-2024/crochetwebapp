package org.crochet.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.crochet.model.VideoPopupClip;

import java.util.ArrayList;
import java.util.List;

@Converter
public class VideoPopupClipListConverter implements AttributeConverter<List<VideoPopupClip>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<VideoPopupClip>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<VideoPopupClip> attribute) {
        try {
            if (attribute == null || attribute.isEmpty()) {
                return "[]";
            }
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot serialize video popup clips", ex);
        }
    }

    @Override
    public List<VideoPopupClip> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return new ArrayList<>();
            }
            List<VideoPopupClip> clips = MAPPER.readValue(dbData, TYPE);
            return clips == null ? new ArrayList<>() : clips;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot parse video popup clips", ex);
        }
    }
}
