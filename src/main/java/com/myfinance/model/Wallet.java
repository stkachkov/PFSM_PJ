package com.myfinance.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Wallet implements Serializable {
    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<Category, Double> budgets = new HashMap<>();
    private double balance;

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        if (transaction instanceof Income) {
            balance += transaction.getAmount();
        } else if (transaction instanceof Expense) {
            balance -= transaction.getAmount();
        }
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public double getBalance() {
        return balance;
    }

    public void setBudget(Category category, double amount) {
        budgets.put(category, amount);
    }

    public Map<Category, Double> getBudgets() {
        return budgets;
    }
}
