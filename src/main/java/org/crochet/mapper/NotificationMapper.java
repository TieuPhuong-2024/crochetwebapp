package org.crochet.mapper;

import org.crochet.model.Notification;
import org.crochet.payload.response.NotificationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.Collection;
import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {
    NotificationMapper INSTANCE = Mappers.getMapper(NotificationMapper.class);
    
    @Mapping(source = "sender.name", target = "senderName")
    @Mapping(source = "sender.imageUrl", target = "senderImageUrl")
    NotificationResponse toResponse(Notification notification);
    
    List<NotificationResponse> toResponses(Collection<Notification> notifications);
} 