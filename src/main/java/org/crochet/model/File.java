package org.crochet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.crochet.enums.FileType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "file")
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class File extends BaseEntity {
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_content", columnDefinition = "TEXT")
    private String fileContent;
    
    @Column(name = "display_order")
    private Integer order;
    
    @Column(name = "last_modified", columnDefinition = "datetime default current_timestamp")
    private LocalDateTime lastModified;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", columnDefinition = "VARCHAR(20)")
    private FileType fileType;
    
    @ManyToMany(mappedBy = "patternFiles")
    @Builder.Default
    private Set<FreePattern> freePatterns = new HashSet<>();
    
    @ManyToMany(mappedBy = "patternImages")
    @Builder.Default
    private Set<FreePattern> freePatternsAsImage = new HashSet<>();

    @ManyToMany(mappedBy = "postFiles")
    @Builder.Default
    private Set<BlogPost> posts = new HashSet<>();

    @ManyToMany(mappedBy = "productImages")
    @Builder.Default
    private Set<Product> products = new HashSet<>();
}
