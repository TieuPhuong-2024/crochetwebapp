package org.crochet.mapper;

import org.crochet.model.Settings;
import org.crochet.payload.response.SettingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SettingMapper {
    SettingMapper INSTANCE = Mappers.getMapper(SettingMapper.class);

    SettingResponse toResponse(Settings settings);

    List<SettingResponse> toResponses(Collection<Settings> settings);
}
