package com.centsible.backend.repository;

import java.math.BigDecimal;

public interface CategorySpendProjection {

    String getCategory();

    BigDecimal getTotalSpent();

}
