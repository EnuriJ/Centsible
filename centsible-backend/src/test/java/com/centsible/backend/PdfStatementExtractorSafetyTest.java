package com.centsible.backend;

import com.centsible.backend.service.PdfStatementExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain unit tests (no Spring context) for the PDF extractor's decompression limits.
 */
public class PdfStatementExtractorSafetyTest {

    private static byte[] deflate(byte[] raw) {
        Deflater deflater = new Deflater();
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            int n = deflater.deflate(buffer);
            out.write(buffer, 0, n);
        }
        deflater.end();
        return out.toByteArray();
    }

    private static byte[] wrapInMinimalPdf(byte[] streamBytes) throws Exception {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n1 0 obj\n<< /Filter /FlateDecode >>\nstream\n".getBytes(StandardCharsets.US_ASCII));
        pdf.write(streamBytes);
        pdf.write("\nendstream\nendobj\n%%EOF".getBytes(StandardCharsets.US_ASCII));
        return pdf.toByteArray();
    }

    @Test
    public void testNormalCompressedTextStreamStillExtracts() throws Exception {
        byte[] content = "BT\n(Hello Centsible) Tj\nET\n".getBytes(StandardCharsets.ISO_8859_1);
        byte[] pdf = wrapInMinimalPdf(deflate(content));

        List<String> lines = PdfStatementExtractor.extractLines(pdf);

        assertEquals(List.of("Hello Centsible"), lines);
    }

    @Test
    public void testDecompressionBombIsRejectedInsteadOfExhaustingMemory() throws Exception {
        // 40 MB of zeros deflates to roughly 40 KB - a tiny file that would otherwise expand
        // far beyond any legitimate statement page.
        byte[] bomb = deflate(new byte[40 * 1024 * 1024]);
        byte[] pdf = wrapInMinimalPdf(bomb);

        assertThrows(IllegalArgumentException.class, () -> PdfStatementExtractor.extractLines(pdf));
    }
}
