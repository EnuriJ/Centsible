package com.centsible.backend.service.statement;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the transaction table out of a bank statement, whatever the bank, from the text
 * runs of a PDF and where each one sits on the page. No bank is hard-coded; instead:
 *
 * <ol>
 *   <li>Find the table's heading row (it mentions a date, a description and an amount, or
 *       debit/credit columns) and work out what each column is. Headings that wrap over
 *       two or three lines are handled.</li>
 *   <li>Walk the lines below it. A line with a date in the date column starts a new
 *       transaction; a text-only line just below one continues its description; anything
 *       else (page footers, totals) is ignored.</li>
 *   <li>Work out whether each amount is money in or out, from (in order of trust) separate
 *       debit/credit columns, a DR/CR column or suffix, a printed minus sign, or the change
 *       in the running balance.</li>
 *   <li>Check every row against the running balance and report any that don't add up
 *       rather than importing wrong numbers silently.</li>
 * </ol>
 */
public final class StatementTableParser {

    private StatementTableParser() {
    }

    private enum Role { DATE, DESCRIPTION, TYPE, DEBIT, CREDIT, DRCR, AMOUNT, BALANCE, OTHER }

    private enum Kind { DATE, AMOUNT, FLAG, TEXT }

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    /** How far (in PDF points) a value may sit from its column heading and still belong to it. */
    private static final double MAX_DATE_COLUMN_DISTANCE = 30.0;
    private static final double MAX_FLAG_COLUMN_DISTANCE = 40.0;

    private static final String[] OPENING_MARKERS = {
            "BALANCE AS OF", "OPENING BALANCE", "BALANCE B/F", "BROUGHT FORWARD", "BALANCE BF"
    };

    /** Dated rows that are really balance markers, not transactions. */
    private static final String[] BALANCE_ROW_MARKERS = {
            "BALANCE AS OF", "OPENING BALANCE", "CLOSING BALANCE", "BALANCE B/F", "BALANCE C/F",
            "BROUGHT FORWARD", "CARRIED FORWARD"
    };

    /** Undated lines that are page furniture and so never continue a description. */
    private static final String[] FURNITURE_MARKERS = {
            "BALANCE AS OF", "OPENING BALANCE", "CLOSING BALANCE", "BALANCE B/F", "BALANCE C/F",
            "BROUGHT FORWARD", "CARRIED FORWARD", "END OF STATEMENT", "TOTAL", "PAGE ", "STATEMENT OF"
    };

    private static final Pattern FLAG = Pattern.compile("^(?:D|C|DR|CR|DB|DEBIT|CREDIT)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CORE_NUMBER = Pattern.compile("^(\\d{1,3}(?:,\\d{3})+|\\d+)\\.(\\d{2})$");
    private static final Pattern CURRENCY_PREFIX = Pattern.compile("^(?:[A-Za-z]{2,4}\\.?|[$€£])\\s*");
    private static final Pattern TIME_SUFFIX = Pattern.compile("\\s+\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:AM|PM)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE_NUMERIC = Pattern.compile("^(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{4}|\\d{2})$");
    private static final Pattern DATE_DAY_MONTHNAME = Pattern.compile("^(\\d{1,2})[\\s\\-/]+([A-Za-z]{3,9})\\.?[\\s\\-/,]+(\\d{4}|\\d{2})$");
    private static final Pattern DATE_ISO = Pattern.compile("^(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})$");
    private static final Pattern DATE_YEAR_MONTHNAME = Pattern.compile("^(\\d{4})[\\s\\-/]+([A-Za-z]{3,9})[\\s\\-/]+(\\d{1,2})$");
    private static final List<String> MONTHS = List.of(
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec");

    // ------------------------------------------------------------------ public API

    public static ParsedStatement parse(List<PositionedText> chunks) {
        List<String> notes = new ArrayList<>();
        List<Line> lines = buildLines(chunks);
        BigDecimal opening = findOpeningBalance(lines);

        List<RawRow> rows = new ArrayList<>();
        Layout layout = null;
        RawRow current = null;
        Line previousLine = null;

        int i = 0;
        while (i < lines.size()) {
            Layout header = detectHeader(lines, i);
            if (header != null) {
                layout = header;
                i += header.lineCount;
                current = null;
                previousLine = null;
                continue;
            }

            Line line = lines.get(i);
            i++;
            if (layout == null) {
                continue;
            }

            Cells cells = layout.assign(line);
            if (cells.primaryDate != null) {
                current = new RawRow(cells);
                rows.add(current);
                previousLine = line;
            } else if (current != null && isContinuation(line, previousLine, cells)) {
                current.description = join(current.description, joinTexts(cells.description, cells.type));
                previousLine = line;
            } else {
                current = null;
                previousLine = null;
            }
        }

        return resolve(rows, opening, notes);
    }

    /** Parses a single date cell; day-first for numeric dates. Returns null if it isn't a date. */
    public static LocalDate parseDate(String raw) {
        if (raw == null) {
            return null;
        }
        String t = TIME_SUFFIX.matcher(raw.trim()).replaceFirst("").trim();
        try {
            Matcher m = DATE_NUMERIC.matcher(t);
            if (m.matches()) {
                int first = Integer.parseInt(m.group(1));
                int second = Integer.parseInt(m.group(2));
                int year = fullYear(m.group(3));
                // Day-first (Sri Lanka); only flip when the second number can't be a month
                if (second > 12 && first <= 12) {
                    return checkedDate(year, first, second);
                }
                return checkedDate(year, second, first);
            }
            m = DATE_DAY_MONTHNAME.matcher(t);
            if (m.matches()) {
                int month = monthNumber(m.group(2));
                return month == 0 ? null : checkedDate(fullYear(m.group(3)), month, Integer.parseInt(m.group(1)));
            }
            m = DATE_ISO.matcher(t);
            if (m.matches()) {
                return checkedDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
            }
            m = DATE_YEAR_MONTHNAME.matcher(t);
            if (m.matches()) {
                int month = monthNumber(m.group(2));
                return month == 0 ? null : checkedDate(Integer.parseInt(m.group(1)), month, Integer.parseInt(m.group(3)));
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    // ------------------------------------------------------------------ lines

    private static final class Line {
        final int page;
        final double y;
        double height;
        List<PositionedText> chunks = new ArrayList<>();

        Line(PositionedText first) {
            this.page = first.page();
            this.y = first.y();
            this.height = first.height();
            this.chunks.add(first);
        }
    }

    private static List<Line> buildLines(List<PositionedText> chunks) {
        List<PositionedText> sorted = new ArrayList<>();
        for (PositionedText c : chunks) {
            if (c.text() != null && !c.text().isBlank()) {
                sorted.add(c);
            }
        }
        sorted.sort(Comparator.comparingInt(PositionedText::page)
                .thenComparingDouble(PositionedText::y)
                .thenComparingDouble(PositionedText::x0));

        List<Line> lines = new ArrayList<>();
        Line current = null;
        for (PositionedText c : sorted) {
            double tolerance = Math.max(1.5, 0.4 * c.height());
            if (current != null && current.page == c.page() && Math.abs(c.y() - current.y) <= tolerance) {
                current.chunks.add(c);
                current.height = Math.max(current.height, c.height());
            } else {
                current = new Line(c);
                lines.add(current);
            }
        }
        for (Line line : lines) {
            line.chunks.sort(Comparator.comparingDouble(PositionedText::x0));
            line.chunks = mergeWordGaps(line.chunks);
        }
        return lines;
    }

    /**
     * Some PDFs position every word separately. Runs separated by no more than a normal
     * word space are put back together; real column gaps are much wider and stay apart.
     */
    private static List<PositionedText> mergeWordGaps(List<PositionedText> chunks) {
        List<PositionedText> merged = new ArrayList<>();
        PositionedText previous = null;
        for (PositionedText c : chunks) {
            if (previous != null) {
                double gap = c.x0() - previous.x1();
                if (gap >= -1.0 && gap <= 0.3 * Math.max(previous.height(), c.height())) {
                    previous = new PositionedText(previous.page(), previous.x0(), Math.max(previous.x1(), c.x1()),
                            previous.y(), Math.max(previous.height(), c.height()),
                            previous.text().trim() + " " + c.text().trim());
                    continue;
                }
                merged.add(previous);
            }
            previous = c;
        }
        if (previous != null) {
            merged.add(previous);
        }
        return merged;
    }

    // ------------------------------------------------------------------ header detection

    private static final class Column {
        final double x0;
        final double x1;
        final Role role;
        final boolean effective;
        final boolean valueDate;

        Column(double x0, double x1, String heading) {
            this.x0 = x0;
            this.x1 = x1;
            String norm = " " + heading.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", " ").trim() + " ";
            this.role = classify(norm);
            this.effective = norm.contains(" EFFECTIVE ");
            this.valueDate = norm.contains(" VALUE ");
        }

        double centre() {
            return (x0 + x1) / 2.0;
        }

        double distanceTo(PositionedText c) {
            return Math.min(Math.abs(c.x0() - x0), Math.min(Math.abs(c.x1() - x1), Math.abs(c.centre() - centre())));
        }

        private static Role classify(String n) {
            if (n.contains(" BALANCE ")) return Role.BALANCE;
            if (n.contains(" DATE ")) return Role.DATE;
            boolean debit = containsAny(n, " DEBIT ", " DEBITS ", " WITHDRAWAL ", " WITHDRAWALS ", " WITHDRAWN ", " PAYMENT ", " PAYMENTS ");
            boolean credit = containsAny(n, " CREDIT ", " CREDITS ", " DEPOSIT ", " DEPOSITS ", " RECEIPT ", " RECEIPTS ");
            if (debit && credit) return Role.DRCR;
            if (debit) return Role.DEBIT;
            if (credit) return Role.CREDIT;
            boolean dr = n.contains(" DR ");
            boolean cr = n.contains(" CR ");
            if (dr && cr) return Role.DRCR;
            if (dr) return Role.DEBIT;
            if (cr) return Role.CREDIT;
            if (n.contains(" AMOUNT ")) return Role.AMOUNT;
            if (containsAny(n, " DESCRIPTION ", " PARTICULARS ", " DETAILS ", " NARRATION ", " NARRATIVE ", " REMARKS ")) {
                return Role.DESCRIPTION;
            }
            if (n.contains(" TYPE ")) return Role.TYPE;
            return Role.OTHER;
        }

        private static boolean containsAny(String n, String... words) {
            for (String w : words) {
                if (n.contains(w)) return true;
            }
            return false;
        }
    }

    private static final class Layout {
        final List<Column> columns;
        final Column primaryDate;
        int lineCount;

        Layout(List<Column> columns, Column primaryDate) {
            this.columns = columns;
            this.primaryDate = primaryDate;
        }

        Cells assign(Line line) {
            Cells cells = new Cells();
            Map<Column, LocalDate> dates = new HashMap<>();
            for (PositionedText chunk : line.chunks) {
                String text = chunk.text().trim();
                Kind kind = kindOf(text);

                if (kind == Kind.DATE) {
                    Column best = nearest(chunk, Role.DATE);
                    if (best != null && best.distanceTo(chunk) <= MAX_DATE_COLUMN_DISTANCE) {
                        dates.putIfAbsent(best, parseDate(text));
                        continue;
                    }
                } else if (kind == Kind.AMOUNT) {
                    Column best = nearest(chunk, Role.AMOUNT, Role.DEBIT, Role.CREDIT, Role.BALANCE);
                    if (best != null) {
                        cells.putNumber(best.role, text);
                        cells.numericChunks++;
                        continue;
                    }
                } else if (kind == Kind.FLAG) {
                    Column best = nearest(chunk, Role.DRCR);
                    if (best != null && best.distanceTo(chunk) <= MAX_FLAG_COLUMN_DISTANCE) {
                        if (cells.flag == null) {
                            cells.flag = text;
                        }
                        cells.numericChunks++;
                        continue;
                    }
                }

                // Everything else is description-like text
                Column best = nearest(chunk, Role.DESCRIPTION, Role.TYPE);
                if (best != null && best.role == Role.TYPE) {
                    cells.type.add(text);
                } else {
                    cells.description.add(text);
                }
                cells.textChunks++;
            }

            Column chosen = null;
            for (Column c : columns) {
                if (c.role == Role.DATE && dates.get(c) != null) {
                    boolean better = chosen == null
                            || (c.effective && !chosen.effective)
                            || (!c.effective && !chosen.effective && chosen.valueDate && !c.valueDate);
                    if (better) {
                        chosen = c;
                    }
                }
            }
            cells.primaryDate = chosen == null ? null : dates.get(chosen);
            return cells;
        }

        private Column nearest(PositionedText chunk, Role... roles) {
            Column best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Column c : columns) {
                boolean wanted = false;
                for (Role r : roles) {
                    if (c.role == r) {
                        wanted = true;
                        break;
                    }
                }
                if (!wanted) continue;
                double d = c.distanceTo(chunk);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = c;
                }
            }
            return best;
        }
    }

    private static final class Cells {
        LocalDate primaryDate;
        final List<String> description = new ArrayList<>();
        final List<String> type = new ArrayList<>();
        String debit;
        String credit;
        String amount;
        String balance;
        String flag;
        int textChunks;
        int numericChunks;

        void putNumber(Role role, String text) {
            switch (role) {
                case DEBIT -> { if (debit == null) debit = text; }
                case CREDIT -> { if (credit == null) credit = text; }
                case BALANCE -> { if (balance == null) balance = text; }
                default -> { if (amount == null) amount = text; }
            }
        }
    }

    private static Layout detectHeader(List<Line> lines, int start) {
        Line first = lines.get(start);
        for (int size = 1; size <= 3 && start + size <= lines.size(); size++) {
            List<Line> stack = lines.subList(start, start + size);
            if (size > 1) {
                Line last = stack.get(size - 1);
                if (last.page != first.page || last.y - first.y > 12.0) {
                    break;
                }
            }
            Layout layout = tryHeader(stack);
            if (layout != null) {
                layout.lineCount = size;
                return layout;
            }
        }
        return null;
    }

    private static Layout tryHeader(List<Line> stack) {
        // A heading never contains dates or amounts; if it does this is a data row
        for (Line line : stack) {
            for (PositionedText c : line.chunks) {
                Kind k = kindOf(c.text().trim());
                if (k == Kind.DATE || k == Kind.AMOUNT) {
                    return null;
                }
            }
        }

        List<Column> columns = buildColumns(stack);
        boolean hasDate = false;
        boolean hasMoney = false;
        boolean hasText = false;
        for (Column c : columns) {
            switch (c.role) {
                case DATE -> hasDate = true;
                case AMOUNT, DEBIT, CREDIT -> hasMoney = true;
                case DESCRIPTION, TYPE -> hasText = true;
                default -> { }
            }
        }
        if (!(hasDate && hasMoney && hasText)) {
            return null;
        }

        Column primary = null;
        for (Column c : columns) {
            if (c.role != Role.DATE) continue;
            if (primary == null
                    || (c.effective && !primary.effective)
                    || (!c.effective && !primary.effective && primary.valueDate && !c.valueDate)) {
                primary = c;
            }
        }
        return new Layout(columns, primary);
    }

    private record Item(int line, PositionedText chunk) {
    }

    private static List<Column> buildColumns(List<Line> stack) {
        List<Item> items = new ArrayList<>();
        for (int li = 0; li < stack.size(); li++) {
            for (PositionedText c : stack.get(li).chunks) {
                items.add(new Item(li, c));
            }
        }

        // Cluster heading pieces that sit above one another
        int n = items.size();
        int[] parent = new int[n];
        for (int a = 0; a < n; a++) parent[a] = a;
        for (int a = 0; a < n; a++) {
            for (int b = a + 1; b < n; b++) {
                if (items.get(a).line() == items.get(b).line()) continue;
                PositionedText p = items.get(a).chunk();
                PositionedText q = items.get(b).chunk();
                double overlap = Math.min(p.x1(), q.x1()) - Math.max(p.x0(), q.x0());
                if (overlap > 0.5) {
                    int ra = find(parent, a);
                    int rb = find(parent, b);
                    if (ra != rb) parent[rb] = ra;
                }
            }
        }
        Map<Integer, List<Item>> groups = new LinkedHashMap<>();
        for (int a = 0; a < n; a++) {
            groups.computeIfAbsent(find(parent, a), k -> new ArrayList<>()).add(items.get(a));
        }

        List<Column> columns = new ArrayList<>();
        for (List<Item> group : groups.values()) {
            List<Column> split = trySplit(group);
            if (split != null) {
                columns.addAll(split);
                continue;
            }
            group.sort(Comparator.comparingInt(Item::line).thenComparingDouble(it -> it.chunk().x0()));
            double x0 = Double.MAX_VALUE;
            double x1 = -Double.MAX_VALUE;
            StringBuilder text = new StringBuilder();
            for (Item it : group) {
                x0 = Math.min(x0, it.chunk().x0());
                x1 = Math.max(x1, it.chunk().x1());
                text.append(' ').append(it.chunk().text().trim());
            }
            columns.add(new Column(x0, x1, text.toString().trim()));
        }
        columns.sort(Comparator.comparingDouble(c -> c.x0));
        return columns;
    }

    /**
     * Handles a wide heading such as "Posting Effective" sitting above two narrower ones
     * ("Date", "Date"): its words are shared out between the narrow headings.
     */
    private static List<Column> trySplit(List<Item> group) {
        Map<Integer, List<Item>> byLine = new LinkedHashMap<>();
        for (Item it : group) {
            byLine.computeIfAbsent(it.line(), k -> new ArrayList<>()).add(it);
        }
        if (byLine.size() != 2) {
            return null;
        }
        List<Item> multi = null;
        Item wide = null;
        for (List<Item> lineItems : byLine.values()) {
            if (lineItems.size() >= 2) {
                if (multi != null) return null;
                multi = lineItems;
            } else {
                wide = lineItems.get(0);
            }
        }
        if (multi == null || wide == null) {
            return null;
        }
        String[] words = wide.chunk().text().trim().split("\\s+");
        if (words.length < multi.size()) {
            return null;
        }

        multi.sort(Comparator.comparingDouble(it -> it.chunk().x0()));
        List<List<String>> assigned = new ArrayList<>();
        for (int m = 0; m < multi.size(); m++) assigned.add(new ArrayList<>());

        int totalChars = 0;
        for (String w : words) totalChars += w.length() + 1;
        double span = wide.chunk().x1() - wide.chunk().x0();
        int consumed = 0;
        for (String w : words) {
            double centre = wide.chunk().x0() + span * (consumed + (w.length() + 1) / 2.0) / totalChars;
            consumed += w.length() + 1;
            int best = 0;
            double bestDistance = Double.MAX_VALUE;
            for (int m = 0; m < multi.size(); m++) {
                double d = Math.abs(centre - multi.get(m).chunk().centre());
                if (d < bestDistance) {
                    bestDistance = d;
                    best = m;
                }
            }
            assigned.get(best).add(w);
        }

        boolean wideIsAbove = wide.line() < multi.get(0).line();
        List<Column> columns = new ArrayList<>();
        for (int m = 0; m < multi.size(); m++) {
            String own = multi.get(m).chunk().text().trim();
            String shared = String.join(" ", assigned.get(m));
            String heading = wideIsAbove ? (shared + " " + own) : (own + " " + shared);
            columns.add(new Column(multi.get(m).chunk().x0(), multi.get(m).chunk().x1(), heading.trim()));
        }
        return columns;
    }

    private static int find(int[] parent, int a) {
        while (parent[a] != a) {
            parent[a] = parent[parent[a]];
            a = parent[a];
        }
        return a;
    }

    // ------------------------------------------------------------------ rows

    private static final class RawRow {
        final LocalDate date;
        String description;
        final String debit;
        final String credit;
        final String amount;
        final String balance;
        final String flag;

        // Filled in by resolve()
        BigDecimal magnitude;
        Integer sign;
        BigDecimal balanceValue;

        RawRow(Cells cells) {
            this.date = cells.primaryDate;
            String desc = joinTexts(cells.description, new ArrayList<>());
            this.description = desc.isBlank() ? joinTexts(cells.type, new ArrayList<>()) : desc;
            this.debit = cells.debit;
            this.credit = cells.credit;
            this.amount = cells.amount;
            this.balance = cells.balance;
            this.flag = cells.flag;
        }
    }

    private static boolean isContinuation(Line line, Line previous, Cells cells) {
        if (previous == null || line.page != previous.page) return false;
        if (cells.numericChunks > 0 || cells.textChunks == 0) return false;
        if (line.y - previous.y > Math.max(2.5 * line.height, 12.0)) return false;
        String upper = joinTexts(cells.description, cells.type).toUpperCase(Locale.ROOT);
        return !isFurniture(upper);
    }

    private static boolean isFurniture(String upper) {
        for (String marker : FURNITURE_MARKERS) {
            if (upper.contains(marker)) return true;
        }
        return false;
    }

    private static boolean isBalanceRow(String upper) {
        for (String marker : BALANCE_ROW_MARKERS) {
            if (upper.contains(marker)) return true;
        }
        return false;
    }

    private static String joinTexts(List<String> first, List<String> second) {
        List<String> all = new ArrayList<>(first);
        all.addAll(second);
        return String.join(" ", all).trim();
    }

    private static String join(String a, String b) {
        if (b == null || b.isBlank()) return a;
        if (a == null || a.isBlank()) return b;
        return a + " " + b;
    }

    // ------------------------------------------------------------------ opening balance

    private static BigDecimal findOpeningBalance(List<Line> lines) {
        for (Line line : lines) {
            StringBuilder text = new StringBuilder();
            for (PositionedText c : line.chunks) text.append(' ').append(c.text());
            String upper = text.toString().toUpperCase(Locale.ROOT);
            boolean marker = false;
            for (String m : OPENING_MARKERS) {
                if (upper.contains(m)) {
                    marker = true;
                    break;
                }
            }
            if (!marker) continue;
            for (int k = line.chunks.size() - 1; k >= 0; k--) {
                Num num = parseNumber(line.chunks.get(k).text());
                if (num != null) {
                    return num.sign < 0 ? num.abs.negate() : num.abs;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ amounts

    private record Num(BigDecimal abs, int sign, boolean creditSuffix, boolean debitSuffix) {
    }

    /** Parses "1,234.50", "-1,234.50", "(1,234.50)", "LKR 1,234.50", "1,234.50 CR"... or returns null. */
    static Num parseNumber(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;

        int sign = 0;
        boolean creditSuffix = false;
        boolean debitSuffix = false;

        if (t.startsWith("(")) {
            sign = -1;
            t = t.substring(1).trim();
        } else if (t.startsWith("-")) {
            sign = -1;
            t = t.substring(1).trim();
        } else if (t.startsWith("+")) {
            sign = 1;
            t = t.substring(1).trim();
        }

        t = CURRENCY_PREFIX.matcher(t).replaceFirst("").trim();

        if (t.startsWith("-")) {
            sign = -1;
            t = t.substring(1).trim();
        } else if (t.startsWith("+")) {
            sign = 1;
            t = t.substring(1).trim();
        }

        String upper = t.toUpperCase(Locale.ROOT);
        if (upper.endsWith(")") || upper.endsWith("-")) {
            sign = -1;
            t = t.substring(0, t.length() - 1).trim();
        } else if (upper.endsWith("CR")) {
            creditSuffix = true;
            t = t.substring(0, t.length() - 2).trim();
        } else if (upper.endsWith("DR")) {
            debitSuffix = true;
            t = t.substring(0, t.length() - 2).trim();
        }

        Matcher m = CORE_NUMBER.matcher(t);
        if (!m.matches()) return null;
        BigDecimal value = new BigDecimal(m.group(1).replace(",", "") + "." + m.group(2));
        if (creditSuffix) sign = 1;
        if (debitSuffix) sign = -1;
        return new Num(value, sign, creditSuffix, debitSuffix);
    }

    private static Kind kindOf(String text) {
        if (parseDate(text) != null) return Kind.DATE;
        if (parseNumber(text) != null) return Kind.AMOUNT;
        if (FLAG.matcher(text).matches()) return Kind.FLAG;
        return Kind.TEXT;
    }

    private static Integer flagSign(String flag) {
        if (flag == null) return null;
        String f = flag.trim().toUpperCase(Locale.ROOT);
        if (f.startsWith("D")) return -1;
        if (f.startsWith("C")) return 1;
        return null;
    }

    // ------------------------------------------------------------------ resolve

    private static ParsedStatement resolve(List<RawRow> rows, BigDecimal opening, List<String> notes) {
        boolean sawCreditSuffix = false;
        boolean sawDebitSuffix = false;
        boolean sawPrintedNegative = false;
        int skippedNoAmount = 0;

        List<RawRow> kept = new ArrayList<>();
        for (RawRow r : rows) {
            Num debit = parseNumber(r.debit);
            Num credit = parseNumber(r.credit);
            Num amount = parseNumber(r.amount);
            Num balance = parseNumber(r.balance);
            if (balance != null) {
                r.balanceValue = balance.sign < 0 ? balance.abs.negate() : balance.abs;
            }

            if (debit != null || credit != null) {
                BigDecimal out = debit == null ? BigDecimal.ZERO : debit.abs;
                BigDecimal in = credit == null ? BigDecimal.ZERO : credit.abs;
                BigDecimal net = in.subtract(out);
                r.magnitude = net.abs();
                r.sign = net.signum() >= 0 ? 1 : -1;
            } else if (amount != null) {
                r.magnitude = amount.abs;
                Integer fromFlag = flagSign(r.flag);
                if (fromFlag != null) {
                    r.sign = fromFlag;
                } else if (amount.sign != 0) {
                    r.sign = amount.sign;
                    if (amount.sign < 0) sawPrintedNegative = true;
                }
                if (amount.creditSuffix) sawCreditSuffix = true;
                if (amount.debitSuffix) sawDebitSuffix = true;
            }

            String upper = r.description == null ? "" : r.description.toUpperCase(Locale.ROOT);
            if (r.magnitude == null) {
                // A dated line with only a balance is an opening / brought-forward line
                if (r.balanceValue != null && isBalanceRow(upper)) {
                    if (opening == null) {
                        opening = r.balanceValue;
                    }
                    continue;
                }
                skippedNoAmount++;
                continue;
            }
            if (isBalanceRow(upper)) {
                continue;
            }
            kept.add(r);
        }
        return finish(kept, opening, notes, sawCreditSuffix, sawDebitSuffix, sawPrintedNegative, skippedNoAmount);
    }

    private static ParsedStatement finish(List<RawRow> kept, BigDecimal opening, List<String> notes,
                                          boolean sawCreditSuffix, boolean sawDebitSuffix,
                                          boolean sawPrintedNegative, int skippedNoAmount) {
        if (kept.isEmpty()) {
            return new ParsedStatement(new ArrayList<>(), notes);
        }

        // Statement-level evidence for rows that carry no marker of their own
        for (RawRow r : kept) {
            if (r.sign != null) continue;
            if (sawPrintedNegative) {
                r.sign = 1;
            } else if (sawCreditSuffix && !sawDebitSuffix) {
                r.sign = -1;
            } else if (sawDebitSuffix && !sawCreditSuffix) {
                r.sign = 1;
            }
        }

        // Newest-first statements: walk them oldest-first so the running balance reads forwards
        LocalDate firstDate = kept.get(0).date;
        LocalDate lastDate = kept.get(kept.size() - 1).date;
        boolean newestFirst = firstDate.isAfter(lastDate);
        List<RawRow> chronological = new ArrayList<>(kept);
        if (newestFirst) {
            Collections.reverse(chronological);
        }

        BigDecimal previousBalance = opening;
        int checked = 0;
        int mismatched = 0;
        int assumedSpending = 0;
        String firstMismatch = null;

        for (RawRow r : chronological) {
            if (r.sign == null && r.balanceValue != null && previousBalance != null) {
                BigDecimal diff = r.balanceValue.subtract(previousBalance);
                if (diff.abs().subtract(r.magnitude).abs().compareTo(TOLERANCE) <= 0) {
                    r.sign = diff.signum() >= 0 ? 1 : -1;
                }
            }
            if (r.sign == null) {
                r.sign = looksLikeIncome(r.description) ? 1 : -1;
                if (r.sign < 0) assumedSpending++;
            }

            BigDecimal signed = r.sign < 0 ? r.magnitude.negate() : r.magnitude;
            if (r.balanceValue != null && previousBalance != null) {
                checked++;
                if (previousBalance.add(signed).subtract(r.balanceValue).abs().compareTo(TOLERANCE) > 0) {
                    mismatched++;
                    if (firstMismatch == null) firstMismatch = r.date + " " + r.description;
                }
            }
            previousBalance = r.balanceValue;
        }

        List<ParsedTransaction> out = new ArrayList<>();
        for (RawRow r : kept) {
            BigDecimal signed = r.sign < 0 ? r.magnitude.negate() : r.magnitude;
            String description = r.description == null || r.description.isBlank() ? "Transaction" : r.description;
            out.add(new ParsedTransaction(r.date, description, signed, r.balanceValue));
        }

        if (mismatched > 0) {
            notes.add(mismatched + " of " + checked + " rows didn't add up with the running balance on the statement"
                    + " (first: " + firstMismatch + "). Please check the imported amounts.");
        }
        if (assumedSpending > 0) {
            notes.add(assumedSpending + " row" + (assumedSpending == 1 ? " has" : "s have")
                    + " no clear money-in or money-out marker and were treated as spending. Please check them.");
        }
        if (skippedNoAmount > 0) {
            notes.add(skippedNoAmount + " dated row" + (skippedNoAmount == 1 ? " was" : "s were")
                    + " skipped because no amount could be read.");
        }
        return new ParsedStatement(out, notes);
    }

    private static boolean looksLikeIncome(String description) {
        if (description == null) return false;
        String u = description.toUpperCase(Locale.ROOT);
        return (u.contains("INTEREST") && !u.contains("TAX"))
                || u.contains("DEPOSIT") || u.contains("SALARY") || u.contains("RECEIPT")
                || u.contains("REFUND") || u.contains("CREDIT");
    }

    // ------------------------------------------------------------------ dates

    private static int fullYear(String y) {
        int year = Integer.parseInt(y);
        return y.length() == 2 ? 2000 + year : year;
    }

    private static int monthNumber(String name) {
        int idx = MONTHS.indexOf(name.substring(0, 3).toLowerCase(Locale.ROOT));
        return idx < 0 ? 0 : idx + 1;
    }

    private static LocalDate checkedDate(int year, int month, int day) {
        if (year < 1990 || year > 2100) return null;
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }
}
