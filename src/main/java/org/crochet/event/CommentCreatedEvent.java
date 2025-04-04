package org.crochet.event;

import org.crochet.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommentCreatedEvent {
    private final Comment comment;
}
