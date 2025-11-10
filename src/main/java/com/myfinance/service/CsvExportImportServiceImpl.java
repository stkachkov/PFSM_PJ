package com.myfinance.service;

import com.myfinance.model.*;
import com.myfinance.service.api.CsvExportImportService;
import com.myfinance.service.api.FinanceManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvExportImportServiceImpl implements CsvExportImportService {

    private static final String TRANSACTIONS_CSV_HEADER = "Type,Amount,Category";
    private static final String BUDGETS_CSV_HEADER = "Category,Amount";
    private static final String DELIMITER = ",";
    private static final String NEW_LINE = "\n";

    private final FinanceManager financeManager;

    public CsvExportImportServiceImpl(FinanceManager financeManager) {
        this.financeManager = financeManager;
    }

    @Override
    public void exportToCsv(Wallet wallet, String login) {
        exportTransactions(wallet, login);
        exportBudgets(wallet, login);
    }

    private void exportTransactions(Wallet wallet, String login) {
        final String fileName = login + "_transactions.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.append(TRANSACTIONS_CSV_HEADER);
            writer.append(NEW_LINE);

            for (Transaction transaction : wallet.getTransactions()) {
                String type = transaction instanceof Income ? "INCOME" : "EXPENSE";
                writer.append(type);
                writer.append(DELIMITER);
                writer.append(String.valueOf(transaction.getAmount()));
                writer.append(DELIMITER);
                writer.append(transaction.getCategory().getName());
                writer.append(NEW_LINE);
            }
            System.out.println("Транзакции успешно экспортированы в " + fileName);
        } catch (IOException e) {
            System.out.println("Ошибка при экспорте транзакций: " + e.getMessage());
        }
    }

    private void exportBudgets(Wallet wallet, String login) {
        final String fileName = login + "_budgets.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.append(BUDGETS_CSV_HEADER);
            writer.append(NEW_LINE);

            wallet.getBudgets().forEach((category, amount) -> {
                try {
                    writer.append(category.getName());
                    writer.append(DELIMITER);
                    writer.append(String.valueOf(amount));
                    writer.append(NEW_LINE);
                } catch (IOException e) {
                    System.out.println("Ошибка при записи бюджета для категории " + category.getName() + ": " + e.getMessage());
                }
            });
            System.out.println("Бюджеты успешно экспортированы в " + fileName);
        } catch (IOException e) {
            System.out.println("Ошибка при экспорте бюджетов: " + e.getMessage());
        }
    }

    @Override
    public void importFromCsv(Wallet wallet, String login) {
        ImportValidationResult validationResult = validateFiles(login);

        if (!validationResult.isValid()) {
            System.out.println("Импорт отменен. Ошибка валидации: " + validationResult.getErrorMessage());
            return;
        }

        
        wallet.getTransactions().clear();
        wallet.getBudgets().clear();

        for (Transaction transaction : validationResult.getTransactions()) {
            wallet.addTransaction(transaction);
        }
        for (Map.Entry<Category, Double> budgetEntry : validationResult.getBudgets().entrySet()) {
            wallet.setBudget(budgetEntry.getKey(), budgetEntry.getValue());
        }

        System.out.println("Импорт данных успешно завершен.");
    }

    private ImportValidationResult validateFiles(String login) {
        List<Transaction> transactions = new ArrayList<>();
        Map<Category, Double> budgets = new HashMap<>();

        
        try (BufferedReader reader = new BufferedReader(new FileReader(login + "_transactions.csv"))) {
            String line = reader.readLine(); 
            if (line == null || !line.equals(TRANSACTIONS_CSV_HEADER)) {
                return new ImportValidationResult(false, "Неверный заголовок в файле транзакций.");
            }
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(DELIMITER);
                if (parts.length != 3) {
                    return new ImportValidationResult(false, "Неверное количество колонок в файле транзакций на строке " + lineNumber);
                }
                String type = parts[0].toUpperCase();
                if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
                    return new ImportValidationResult(false, "Неверный тип транзакции на строке " + lineNumber);
                }
                double amount;
                try {
                    amount = Double.parseDouble(parts[1]);
                    if (amount <= 0) {
                        return new ImportValidationResult(false, "Сумма должна быть положительной на строке " + lineNumber + " в файле транзакций.");
                    }
                } catch (NumberFormatException e) {
                    return new ImportValidationResult(false, "Неверный формат суммы на строке " + lineNumber + " в файле транзакций.");
                }
                Category category = financeManager.getOrCreateCategory(parts[2]);
                if (type.equals("INCOME")) {
                    transactions.add(new Income(amount, category));
                } else {
                    transactions.add(new Expense(amount, category));
                }
            }
        } catch (IOException e) {
            return new ImportValidationResult(false, "Не удалось прочитать файл транзакций: " + e.getMessage());
        }

        
        try (BufferedReader reader = new BufferedReader(new FileReader(login + "_budgets.csv"))) {
            String line = reader.readLine(); 
            if (line == null || !line.equals(BUDGETS_CSV_HEADER)) {
                return new ImportValidationResult(false, "Неверный заголовок в файле бюджетов.");
            }
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] parts = line.split(DELIMITER);
                if (parts.length != 2) {
                    return new ImportValidationResult(false, "Неверное количество колонок в файле бюджетов на строке " + lineNumber);
                }
                double amount;
                try {
                    amount = Double.parseDouble(parts[1]);
                    if (amount <= 0) {
                        return new ImportValidationResult(false, "Сумма должна быть положительной на строке " + lineNumber + " в файле бюджетов.");
                    }
                } catch (NumberFormatException e) {
                    return new ImportValidationResult(false, "Неверный формат суммы на строке " + lineNumber + " в файле бюджетов.");
                }
                Category category = financeManager.getOrCreateCategory(parts[0]);
                budgets.put(category, amount);
            }
        } catch (IOException e) {
            return new ImportValidationResult(false, "Не удалось прочитать файл бюджетов: " + e.getMessage());
        }

        return new ImportValidationResult(true, transactions, budgets);
    }

    private static class ImportValidationResult {
        private final boolean isValid;
        private final String errorMessage;
        private final List<Transaction> transactions;
        private final Map<Category, Double> budgets;

        public ImportValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.transactions = new ArrayList<>();
            this.budgets = new HashMap<>();
        }

        public ImportValidationResult(boolean isValid, List<Transaction> transactions, Map<Category, Double> budgets) {
            this.isValid = isValid;
            this.errorMessage = "";
            this.transactions = transactions;
            this.budgets = budgets;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public Map<Category, Double> getBudgets() {
            return budgets;
        }
    }
}