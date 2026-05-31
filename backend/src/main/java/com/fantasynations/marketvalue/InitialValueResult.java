package com.fantasynations.marketvalue;

import java.math.BigDecimal;
import java.util.Map;

public record InitialValueResult(
        BigDecimal value,
        Map<String, BigDecimal> breakdown
) {}
