package org.crochet.repository;

import org.crochet.model.VideoPopupList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoPopupListRepo extends JpaRepository<VideoPopupList, String> {
    List<VideoPopupList> findByActiveTrueOrderByCreatedDateDesc();
}
