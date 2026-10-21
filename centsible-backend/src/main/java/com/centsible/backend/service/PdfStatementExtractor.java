package com.centsible.backend.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Pure Java, zero-dependency PDF text stream extractor.
 * Decompresses FlateDecode content streams and reconstructs text lines
 * from PDF text layout operators (Tj, TJ, ', \", Td, TD, Tm, T*).
 */
public class PdfStatementExtractor {

    // Decompression limits. A tiny, maliciously crafted PDF ("zip bomb") can inflate to
    // gigabytes and exhaust the server's memory, so every stream - and the document as a
    // whole - is capped. Real statement pages are a few KB per content stream; these
    // limits are far above anything legitimate.
    private static final int MAX_STREAM_OUTPUT_BYTES = 8 * 1024 * 1024;
    private static final long MAX_TOTAL_OUTPUT_BYTES = 32L * 1024 * 1024;

    public static List<String> extractLines(byte[] pdfBytes) {
        List<String> lines = new ArrayList<>();
        if (pdfBytes == null || pdfBytes.length == 0) {
            return lines;
        }

        // 1. Locate all stream...endstream blocks
        List<byte[]> streams = extractStreams(pdfBytes);

        // 2. Parse text operators from each decompressed stream
        StringBuilder pageText = new StringBuilder();
        long totalDecompressed = 0;
        for (byte[] stream : streams) {
            int allowed = (int) Math.min(MAX_STREAM_OUTPUT_BYTES, MAX_TOTAL_OUTPUT_BYTES - totalDecompressed);
            String decompressed = decompressStream(stream, allowed);
            totalDecompressed += decompressed.length();
            if (totalDecompressed > MAX_TOTAL_OUTPUT_BYTES) {
                throw new IllegalArgumentException("PDF contains too much data to process safely");
            }
            if (!decompressed.isBlank()) {
                parseTextFromContentStream(decompressed, pageText);
            }
        }

        // 3. Normalize into clean, non-empty lines
        for (String line : pageText.toString().split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }

        return lines;
    }

    private static List<byte[]> extractStreams(byte[] bytes) {
        List<byte[]> streams = new ArrayList<>();
        byte[] streamMarker = "stream".getBytes(StandardCharsets.US_ASCII);
        byte[] endstreamMarker = "endstream".getBytes(StandardCharsets.US_ASCII);

        int pos = 0;
        while (pos < bytes.length) {
            int streamStart = indexOf(bytes, streamMarker, pos);
            if (streamStart == -1) break;

            // Advance past 'stream' keyword and trailing CRLF or LF
            int contentStart = streamStart + streamMarker.length;
            if (contentStart < bytes.length && bytes[contentStart] == '\r') contentStart++;
            if (contentStart < bytes.length && bytes[contentStart] == '\n') contentStart++;

            int streamEnd = indexOf(bytes, endstreamMarker, contentStart);
            if (streamEnd == -1) break;

            int len = streamEnd - contentStart;
            // Trim trailing CRLF before endstream if present
            while (len > 0 && (bytes[contentStart + len - 1] == '\r' || bytes[contentStart + len - 1] == '\n')) {
                len--;
            }

            if (len > 0) {
                byte[] streamData = new byte[len];
                System.arraycopy(bytes, contentStart, streamData, 0, len);
                streams.add(streamData);
            }

            pos = streamEnd + endstreamMarker.length;
        }

        return streams;
    }

    private static String decompressStream(byte[] data, int maxOutputBytes) {
        // Try decompressing with standard zlib header
        try {
            byte[] result = inflateBounded(data, false, maxOutputBytes);
            if (result.length > 0) {
                return new String(result, StandardCharsets.ISO_8859_1);
            }
        } catch (DataFormatException ignored) {
        }

        // Try raw deflate (nowrap = true)
        try {
            byte[] result = inflateBounded(data, true, maxOutputBytes);
            if (result.length > 0) {
                return new String(result, StandardCharsets.ISO_8859_1);
            }
        } catch (DataFormatException ignored) {
        }

        // If uncompressed ASCII/text stream
        return new String(data, StandardCharsets.ISO_8859_1);
    }

    /**
     * Inflates {@code data}, refusing to produce more than {@code maxBytes} of output.
     * Throws IllegalArgumentException (not DataFormatException, so callers don't mistake it
     * for "not compressed" and retry) the moment the limit would be exceeded.
     */
    private static byte[] inflateBounded(byte[] data, boolean nowrap, int maxBytes) throws DataFormatException {
        Inflater inflater = new Inflater(nowrap);
        try {
            inflater.setInput(data);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count <= 0) break;
                if (baos.size() + count > maxBytes) {
                    throw new IllegalArgumentException("PDF contains too much data to process safely");
                }
                baos.write(buffer, 0, count);
            }
            return baos.toByteArray();
        } finally {
            inflater.end();
        }
    }

    private static void parseTextFromContentStream(String content, StringBuilder output) {
        int len = content.length();
        int i = 0;
        boolean inTextObject = false;
        StringBuilder currentLine = new StringBuilder();

        while (i < len) {
            char c = content.charAt(i);

            // Check for BT / ET markers
            if (c == 'B' && i + 1 < len && content.charAt(i + 1) == 'T' && isDelimiter(content, i + 2)) {
                inTextObject = true;
                i += 2;
                continue;
            }
            if (c == 'E' && i + 1 < len && content.charAt(i + 1) == 'T' && isDelimiter(content, i + 2)) {
                inTextObject = false;
                if (!currentLine.isEmpty()) {
                    output.append(currentLine).append('\n');
                    currentLine.setLength(0);
                }
                i += 2;
                continue;
            }

            if (!inTextObject) {
                i++;
                continue;
            }

            // String literal: ( ... )
            if (c == '(') {
                int depth = 1;
                i++;
                StringBuilder literal = new StringBuilder();
                while (i < len && depth > 0) {
                    char sc = content.charAt(i);
                    if (sc == '\\' && i + 1 < len) {
                        char next = content.charAt(i + 1);
                        if (next == 'n') literal.append('\n');
                        else if (next == 'r') literal.append('\r');
                        else if (next == 't') literal.append('\t');
                        else literal.append(next);
                        i += 2;
                        continue;
                    } else if (sc == '(') {
                        depth++;
                    } else if (sc == ')') {
                        depth--;
                        if (depth == 0) {
                            i++;
                            break;
                        }
                    }
                    literal.append(sc);
                    i++;
                }

                // Check operator following the literal
                String op = nextOperator(content, i);
                if (op.equals("Tj") || op.equals("TJ") || op.equals("'") || op.equals("\"")) {
                    if (!currentLine.isEmpty() && currentLine.charAt(currentLine.length() - 1) != ' ') {
                        currentLine.append(' ');
                    }
                    currentLine.append(literal);
                    if (op.equals("'") || op.equals("\"")) {
                        output.append(currentLine).append('\n');
                        currentLine.setLength(0);
                    }
                } else {
                    currentLine.append(literal);
                }
                continue;
            }

            // Array of strings / displacements: [ ... ] TJ
            if (c == '[') {
                i++;
                StringBuilder arrayText = new StringBuilder();
                while (i < len && content.charAt(i) != ']') {
                    if (content.charAt(i) == '(') {
                        i++;
                        while (i < len && content.charAt(i) != ')') {
                            if (content.charAt(i) == '\\' && i + 1 < len) {
                                arrayText.append(content.charAt(i + 1));
                                i += 2;
                            } else {
                                arrayText.append(content.charAt(i));
                                i++;
                            }
                        }
                        if (i < len && content.charAt(i) == ')') i++;
                    } else if (Character.isDigit(content.charAt(i)) || content.charAt(i) == '-') {
                        // Numeric displacement inside TJ array: large negative displacement indicates space
                        int numStart = i;
                        while (i < len && (Character.isDigit(content.charAt(i)) || content.charAt(i) == '-' || content.charAt(i) == '.')) {
                            i++;
                        }
                        try {
                            double disp = Double.parseDouble(content.substring(numStart, i));
                            if (disp < -120 && !arrayText.isEmpty() && arrayText.charAt(arrayText.length() - 1) != ' ') {
                                arrayText.append(' ');
                            }
                        } catch (Exception ignored) {
                        }
                    } else {
                        i++;
                    }
                }
                if (i < len && content.charAt(i) == ']') i++;

                if (!currentLine.isEmpty() && currentLine.charAt(currentLine.length() - 1) != ' ') {
                    currentLine.append(' ');
                }
                currentLine.append(arrayText);
                continue;
            }

            // Check for line positioning operators: T*, Td, TD, Tm
            if (c == 'T' && i + 1 < len) {
                char next = content.charAt(i + 1);
                if (next == '*' || next == 'd' || next == 'D' || next == 'm') {
                    if (!currentLine.isEmpty()) {
                        output.append(currentLine).append('\n');
                        currentLine.setLength(0);
                    }
                    i += 2;
                    continue;
                }
            }

            i++;
        }

        if (!currentLine.isEmpty()) {
            output.append(currentLine).append('\n');
        }
    }

    private static String nextOperator(String s, int pos) {
        int i = pos;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        int start = i;
        while (i < s.length() && !Character.isWhitespace(s.charAt(i)) && s.charAt(i) != '(' && s.charAt(i) != '[') {
            i++;
        }
        return s.substring(start, i);
    }

    private static boolean isDelimiter(String s, int pos) {
        if (pos >= s.length()) return true;
        char c = s.charAt(pos);
        return Character.isWhitespace(c) || c == '(' || c == '[' || c == '<' || c == '/';
    }

    private static int indexOf(byte[] source, byte[] target, int fromIndex) {
        if (fromIndex >= source.length || target.length == 0) return -1;
        outer:
        for (int i = fromIndex; i <= source.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
