package org.crochet.repository;

import org.crochet.enums.FileType;
import org.crochet.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    
    List<File> findByFileTypeOrderByOrderAsc(FileType fileType);
    
    @Query("SELECT f FROM File f WHERE f.fileName = :fileName AND f.fileType = :fileType")
    Optional<File> findByFileNameAndType(@Param("fileName") String fileName, @Param("fileType") FileType fileType);
    
    @Query("SELECT f FROM File f JOIN f.freePatterns p WHERE p.id = :patternId ORDER BY f.order ASC")
    List<File> findFilesByPatternId(@Param("patternId") Long patternId);
    
    @Query("SELECT f FROM File f JOIN f.freePatternsAsImage p WHERE p.id = :patternId ORDER BY f.order ASC")
    List<File> findImagesByPatternId(@Param("patternId") Long patternId);
}
