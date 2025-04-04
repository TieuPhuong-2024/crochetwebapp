package org.crochet.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "blog_post")
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BlogPost extends BaseEntity {
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "home", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean home;

    @OneToMany(mappedBy = "blogPost", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Comment> comments;

    @BatchSize(size = 10)
    @OrderBy("order ASC")
    @ElementCollection
    @CollectionTable(name = "blog_post_file",
            joinColumns = @JoinColumn(name = "blog_post_id", referencedColumnName = "id", nullable = false))
    @AttributeOverrides({
            @AttributeOverride(name = "fileName", column = @Column(name = "file_name")),
            @AttributeOverride(name = "fileContent", column = @Column(name = "file_content")),
            @AttributeOverride(name = "order", column = @Column(name = "display_order")),
            @AttributeOverride(name = "lastModified", column = @Column(name = "last_modified", columnDefinition = "datetime default current_timestamp"))
    })
    private Set<File> files;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_category_id", referencedColumnName = "id")
    @JsonBackReference
    private BlogCategory blogCategory;
}
