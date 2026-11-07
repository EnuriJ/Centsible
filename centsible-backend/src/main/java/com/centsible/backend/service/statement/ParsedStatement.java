package com.centsible.backend.service.statement;

import java.util.List;

/**
 * Result of reading a statement: the transactions found, plus plain-language notes about
 * anything the reader wasn't sure of (rows that didn't add up with the running balance,
 * amounts whose direction had to be guessed, rows that were skipped).
 */
public record ParsedStatement(List<ParsedTransaction> transactions, List<String> notes) {
}
