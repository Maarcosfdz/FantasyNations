package com.fantasynations.marketvalue;

import com.fantasynations.domain.MarketValueChangeReason;

import java.math.BigDecimal;
import java.util.Map;

public record MarketValueResult(
        BigDecimal oldValue,
        BigDecimal newValue,
        BigDecimal delta,
        BigDecimal deltaPercent,
        int momentumScore,
        MarketValueChangeReason reason,
        Map<String, Object> breakdown
) {}
