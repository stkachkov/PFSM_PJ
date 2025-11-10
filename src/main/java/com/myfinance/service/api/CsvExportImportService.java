package com.myfinance.service.api;

import com.myfinance.model.Wallet;

public interface CsvExportImportService {
    void exportToCsv(Wallet wallet, String login);
    void importFromCsv(Wallet wallet, String login);
}
