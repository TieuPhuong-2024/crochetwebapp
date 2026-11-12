package org.crochet.mapper;

import org.crochet.model.Notification;
import org.crochet.payload.request.NotificationRequest;
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "receiver", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "isRead", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    Notification toEntity(NotificationRequest request);
} 