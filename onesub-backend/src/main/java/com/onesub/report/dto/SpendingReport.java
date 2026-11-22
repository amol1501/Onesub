package com.onesub.report.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SpendingReport(
        String period,
        Map<String, BigDecimal> totals,
        Map<String, BigDecimal> byMerchant
) {
}
