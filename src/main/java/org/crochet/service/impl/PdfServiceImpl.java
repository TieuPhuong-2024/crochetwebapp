package org.crochet.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.crochet.model.FreePattern;
import org.crochet.service.PdfService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URL;

@Service
public class PdfServiceImpl implements PdfService {
    @Override
    public byte[] generatePatternPdf(FreePattern pattern) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Font configurations
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
            Font authorFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 12);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            // Title
            Paragraph title = new Paragraph(pattern.getName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Author
            String authorText = "Author: " + (pattern.getAuthor() != null ? pattern.getAuthor() : "Unknown");
            Paragraph author = new Paragraph(authorText, authorFont);
            author.setAlignment(Element.ALIGN_CENTER);
            author.setSpacingAfter(20);
            document.add(author);

            // Description
            if (pattern.getDescription() != null && !pattern.getDescription().trim().isEmpty()) {
                Paragraph descHeading = new Paragraph("Description", headingFont);
                descHeading.setSpacingAfter(5);
                document.add(descHeading);
                
                Paragraph desc = new Paragraph(cleanHtml(pattern.getDescription()), normalFont);
                desc.setSpacingAfter(15);
                document.add(desc);
            }

            // Content (Instructions)
            if (pattern.getContent() != null && !pattern.getContent().trim().isEmpty()) {
                Paragraph contentHeading = new Paragraph("Instructions / Pattern Content", headingFont);
                contentHeading.setSpacingAfter(5);
                document.add(contentHeading);

                Paragraph content = new Paragraph(cleanHtml(pattern.getContent()), normalFont);
                content.setSpacingAfter(15);
                document.add(content);
            }

            // Images
            if (pattern.getImages() != null && !pattern.getImages().isEmpty()) {
                Paragraph imgHeading = new Paragraph("Images", headingFont);
                imgHeading.setSpacingAfter(10);
                document.add(imgHeading);

                for (org.crochet.model.File imgFile : pattern.getImages()) {
                    if (imgFile.getFileContent() != null && imgFile.getFileContent().startsWith("http")) {
                        try {
                            Image image = Image.getInstance(new URL(imgFile.getFileContent()));
                            image.scaleToFit(400, 300);
                            image.setAlignment(Element.ALIGN_CENTER);
                            image.setSpacingAfter(15);
                            document.add(image);
                        } catch (Exception e) {
                            Paragraph errorMsg = new Paragraph("[Could not load image: " + imgFile.getFileName() + "]", normalFont);
                            errorMsg.setSpacingAfter(10);
                            document.add(errorMsg);
                        }
                    }
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF for pattern: " + pattern.getName(), e);
        }
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        String clean = html.replaceAll("(?i)<br\\s*/?>", "\n")
                           .replaceAll("(?i)</p>", "\n\n")
                           .replaceAll("(?i)</h2>", "\n\n")
                           .replaceAll("(?i)</h3>", "\n\n")
                           .replaceAll("<[^>]*>", "")
                           .replaceAll("&nbsp;", " ")
                           .replaceAll("&lt;", "<")
                           .replaceAll("&gt;", ">")
                           .replaceAll("&amp;", "&");
        return clean.trim();
    }
}
