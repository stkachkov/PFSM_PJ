package com.myfinance.service;

import com.myfinance.model.*;
import com.myfinance.service.api.ReportGenerator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportGeneratorImpl implements ReportGenerator {

    @Override
    public String generateFullReport(final Wallet wallet) {
        final StringBuilder report = new StringBuilder();

        final double totalIncome = wallet.getTransactions().stream()
                .filter(t -> t instanceof Income)
                .mapToDouble(Transaction::getAmount)
                .sum();

        final double totalExpense = wallet.getTransactions().stream()
                .filter(t -> t instanceof Expense)
                .mapToDouble(Transaction::getAmount)
                .sum();

        report.append("Общий доход: ").append(String.format("%,.1f", totalIncome)).append("\n");

        report.append("Доходы по категориям:\n");
        final Map<Category, Double> incomeByCategory = wallet.getTransactions().stream()
                .filter(t -> t instanceof Income)
                .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));

        incomeByCategory.forEach((category, sum) -> report.append(category.getName()).append(": ").append(String.format("%,.1f", sum)).append("\n"));

        report.append("Общие расходы: ").append(String.format("%,.1f", totalExpense)).append("\n");

        final Map<Category, Double> expenseByCategory = wallet.getTransactions().stream()
                .filter(t -> t instanceof Expense)
                .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));

        report.append("Бюджет по категориям:\n");
        wallet.getBudgets().forEach((category, budget) -> {
            final double spent = expenseByCategory.getOrDefault(category, 0.0);
            report.append(category.getName()).append(": ").append(String.format("%,.1f", budget)).append(", Оставшийся бюджет: ").append(String.format("%,.1f", budget - spent)).append("\n");
        });

        return report.toString();
    }

    @Override
    public String generateReportByCategories(final Wallet wallet, final List<Category> categories) {
        final StringBuilder report = new StringBuilder();
        if (categories == null || categories.isEmpty()) {
            return "Категории не выбраны.";
        }

        final List<String> categoryNames = categories.stream().map(Category::getName).collect(Collectors.toList());

        final double totalIncome = wallet.getTransactions().stream()
                .filter(t -> t instanceof Income && categoryNames.contains(t.getCategory().getName()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        final double totalExpense = wallet.getTransactions().stream()
                .filter(t -> t instanceof Expense && categoryNames.contains(t.getCategory().getName()))
                .mapToDouble(Transaction::getAmount)
                .sum();

        report.append("Общий доход по выбранным категориям: ").append(String.format("%,.1f", totalIncome)).append("\n");
        report.append("Общие расходы по выбранным категориям: ").append(String.format("%,.1f", totalExpense)).append("\n");

        return report.toString();
    }
}