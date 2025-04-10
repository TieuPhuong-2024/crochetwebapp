package org.crochet.service;

import org.crochet.enums.FileType;
import org.crochet.model.File;

import java.util.List;
import java.util.Optional;

public interface FileService {
    
    File save(File file);
    
    List<File> findAll();
    
    Optional<File> findById(Long id);
    
    void deleteById(Long id);
    
    List<File> findByFileType(FileType fileType);
    
    Optional<File> findByFileNameAndType(String fileName, FileType fileType);
    
    List<File> findFilesByPatternId(Long patternId);
    
    List<File> findImagesByPatternId(Long patternId);
}
