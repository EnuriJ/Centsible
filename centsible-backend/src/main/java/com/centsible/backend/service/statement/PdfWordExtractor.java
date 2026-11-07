package com.centsible.backend.service.statement;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the text of a PDF together with where each run sits on the page (Apache PDFBox),
 * which is what lets {@link StatementTableParser} rebuild a statement's table.
 */
public final class PdfWordExtractor {

    // A statement is a handful of pages; these caps keep a hostile or huge PDF from tying
    // up the server (uploads are also capped at 10MB by Spring).
    private static final int MAX_PAGES = 60;
    private static final int MAX_CHUNKS = 200_000;

    private PdfWordExtractor() {
    }

    /**
     * @throws IllegalArgumentException if the PDF is password-protected or unreasonably large
     * @throws IOException              if the file can't be read as a PDF at all
     */
    public static List<PositionedText> extract(byte[] pdfBytes) throws IOException {
        List<PositionedText> chunks = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new IllegalArgumentException("This PDF has too many pages to import (limit " + MAX_PAGES + ").");
            }
            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> positions) throws IOException {
                    if (positions.isEmpty() || text == null || text.isBlank()) {
                        return;
                    }
                    // PDFBox can hand back one string that spans several columns (when text on
                    // slightly different baselines sits side by side). Split it wherever the gap
                    // between two characters is wider than a word space, so columns stay apart.
                    List<TextPosition> run = new ArrayList<>();
                    for (TextPosition position : positions) {
                        if (!run.isEmpty()) {
                            TextPosition previous = run.get(run.size() - 1);
                            double gap = position.getXDirAdj() - (previous.getXDirAdj() + previous.getWidthDirAdj());
                            if (gap > Math.max(2.0, 0.8 * previous.getHeightDir())) {
                                addChunk(run);
                                run = new ArrayList<>();
                            }
                        }
                        run.add(position);
                    }
                    addChunk(run);
                }

                private void addChunk(List<TextPosition> run) {
                    StringBuilder text = new StringBuilder();
                    for (TextPosition position : run) {
                        text.append(position.getUnicode());
                    }
                    if (text.toString().isBlank()) {
                        return;
                    }
                    if (chunks.size() >= MAX_CHUNKS) {
                        throw new IllegalArgumentException("This PDF contains too much text to import safely.");
                    }
                    TextPosition first = run.get(0);
                    TextPosition last = run.get(run.size() - 1);
                    chunks.add(new PositionedText(
                            getCurrentPageNo(),
                            first.getXDirAdj(),
                            last.getXDirAdj() + last.getWidthDirAdj(),
                            first.getYDirAdj(),
                            first.getHeightDir(),
                            text.toString()));
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(document);
        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException(
                    "This PDF is password-protected. Remove the password (or export a CSV) and try again.");
        }
        return chunks;
    }
}
