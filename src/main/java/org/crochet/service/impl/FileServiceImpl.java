package org.crochet.service.impl;

import lombok.RequiredArgsConstructor;
import org.crochet.enums.FileType;
import org.crochet.model.File;
import org.crochet.repository.FileRepository;
import org.crochet.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;

    @Override
    public File save(File file) {
        return fileRepository.save(file);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<File> findAll() {
        return fileRepository.findAll();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<File> findById(Long id) {
        return fileRepository.findById(id);
    }
    
    @Override
    public void deleteById(Long id) {
        fileRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<File> findByFileType(FileType fileType) {
        return fileRepository.findByFileTypeOrderByOrderAsc(fileType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<File> findByFileNameAndType(String fileName, FileType fileType) {
        return fileRepository.findByFileNameAndType(fileName, fileType);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<File> findFilesByPatternId(Long patternId) {
        return fileRepository.findFilesByPatternId(patternId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<File> findImagesByPatternId(Long patternId) {
        return fileRepository.findImagesByPatternId(patternId);
    }
}
