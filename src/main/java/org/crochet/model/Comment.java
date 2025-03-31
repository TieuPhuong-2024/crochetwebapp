package org.crochet.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "comment")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", referencedColumnName = "id", nullable = false)
    @JsonBackReference
    private BlogPost blogPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    // Comment cha (null nếu là root comment)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    @JsonBackReference
    private Comment parent;
    
    // Danh sách comment con
    @OneToMany(mappedBy = "parent")
    @JsonManagedReference
    private Set<Comment> children;
    
    // ID của người dùng được nhắc đến (mention)
    @Column(name = "mentioned_user_id")
    private String mentionedUserId;
}
