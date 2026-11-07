package com.centsible.backend.service.statement;

/**
 * A run of text on a PDF page together with where it sits.
 * Coordinates are in PDF points measured from the top-left of the page, so a larger
 * {@code y} is lower on the page.
 */
public record PositionedText(int page, double x0, double x1, double y, double height, String text) {

    public double centre() {
        return (x0 + x1) / 2.0;
    }
}
