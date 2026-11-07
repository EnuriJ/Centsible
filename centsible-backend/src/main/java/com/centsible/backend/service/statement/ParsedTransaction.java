package com.centsible.backend.service.statement;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One transaction read from a statement. {@code amount} is signed: negative for money
 * out, positive for money in. {@code balance} is the running balance printed on the
 * statement, or null when the statement doesn't print one.
 */
public record ParsedTransaction(LocalDate date, String description, BigDecimal amount, BigDecimal balance) {
}
