package com.centsible.backend.statement;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds small, entirely made-up statement PDFs for tests. Positions are given the way a
 * statement is laid out: x from the left edge and y measured down from the top of the page.
 * (Real statements contain people's names and account numbers and must never be committed.)
 */
final class SyntheticStatementPdf {

    private static final float FONT_SIZE = 7f;
    private static final PDType1Font FONT = PDType1Font.HELVETICA;

    private record Text(float x, float y, String text) {
    }

    private final List<List<Text>> pages = new ArrayList<>();

    SyntheticStatementPdf page() {
        pages.add(new ArrayList<>());
        return this;
    }

    SyntheticStatementPdf text(float x, float y, String text) {
        pages.get(pages.size() - 1).add(new Text(x, y, text));
        return this;
    }

    /** Right-aligned: the text ends at {@code endX}. */
    SyntheticStatementPdf textRight(float endX, float y, String text) throws IOException {
        float width = FONT.getStringWidth(text) / 1000f * FONT_SIZE;
        return text(endX - width, y, text);
    }

    byte[] build() throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (List<Text> items : pages) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream out = new PDPageContentStream(document, page)) {
                    for (Text t : items) {
                        out.beginText();
                        out.setFont(FONT, FONT_SIZE);
                        out.newLineAtOffset(t.x, PDRectangle.A4.getHeight() - t.y);
                        out.showText(t.text);
                        out.endText();
                    }
                }
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            document.save(bytes);
            return bytes.toByteArray();
        }
    }
}
