package org.crochet.service;

import org.crochet.model.FreePattern;

public interface PdfService {
    byte[] generatePatternPdf(FreePattern pattern);
}
