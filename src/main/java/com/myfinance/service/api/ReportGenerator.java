package com.myfinance.service.api;

import com.myfinance.model.Category;
import com.myfinance.model.Wallet;

import java.util.List;

public interface ReportGenerator {
    String generateFullReport(final Wallet wallet);
    String generateReportByCategories(final Wallet wallet, final List<Category> categories);
}
