package com.centsible.backend.repository;

import java.math.BigDecimal;

public interface CategoryIncomeProjection {

    String getCategory();

    BigDecimal getTotalIncome();

}
