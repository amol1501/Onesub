package com.onesub.report.dto;

import java.math.BigDecimal;
import java.util.Map;

public record MerchantWiseReport(
        Map<String, Map<String, BigDecimal>> byCurrencyAndMerchant
) {
}
