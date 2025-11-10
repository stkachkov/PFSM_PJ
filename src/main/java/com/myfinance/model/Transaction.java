package com.myfinance.model;

import java.io.Serializable;

public abstract class Transaction implements Serializable {
    private final double amount;
    private final Category category;

    public Transaction(final double amount, final Category category) {
        this.amount = amount;
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public Category getCategory() {
        return category;
    }
}
