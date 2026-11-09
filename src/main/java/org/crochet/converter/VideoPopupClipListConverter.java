package org.crochet.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.crochet.payload.VideoPopupClipPayload;

import java.util.ArrayList;
import java.util.List;

@Converter
public class VideoPopupClipListConverter implements AttributeConverter<List<VideoPopupClipPayload>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<VideoPopupClipPayload>> TYPE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<VideoPopupClipPayload> attribute) {
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
    public List<VideoPopupClipPayload> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) {
                return new ArrayList<>();
            }
            List<VideoPopupClipPayload> clips = MAPPER.readValue(dbData, TYPE);
            return clips == null ? new ArrayList<>() : clips;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot parse video popup clips", ex);
        }
    }
}
