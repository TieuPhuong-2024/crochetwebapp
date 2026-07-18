package org.crochet.service;

import org.crochet.enums.ChartStatus;
import org.crochet.model.File;
import org.crochet.model.FreePattern;
import org.crochet.service.impl.PdfServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class PdfServiceImplTest {

    private final PdfService pdfService = new PdfServiceImpl();

    @Test
    public void testGeneratePatternPdf() {
        FreePattern pattern = FreePattern.builder()
                .name("Cute Octopus")
                .author("Tieu Phuong")
                .description("<p>This is a cute octopus crochet pattern.</p>")
                .content("<h2>Abbreviation</h2><p>MR: Magic ring<br/>X: Single crochet</p>")
                .images(Set.of(new File() {{
                    setFileName("octopus.jpg");
                    setFileContent("https://images.example.com/octopus.jpg");
                }}))
                .status(ChartStatus.SUCCESS)
                .build();

        byte[] pdfBytes = pdfService.generatePatternPdf(pattern);

        Assertions.assertNotNull(pdfBytes);
        Assertions.assertTrue(pdfBytes.length > 0);
    }
}
