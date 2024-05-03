package org.crochet.service;

import org.crochet.payload.request.PatternRequest;
import org.crochet.payload.response.PatternPaginationResponse;
import org.crochet.payload.response.PatternResponse;
import org.crochet.repository.Filter;

import java.util.List;

public interface PatternService {
    PatternResponse createOrUpdate(PatternRequest request);

    PatternPaginationResponse getPatterns(int pageNo, int pageSize, String sortBy, String sortDir,
                                          String searchText, String categoryId, List<Filter> filters);

    List<PatternResponse> getLimitedPatterns();

    PatternResponse getDetail(String id);

    void deletePattern(String id);
}
