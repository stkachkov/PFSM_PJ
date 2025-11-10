package com.myfinance.model;

import java.io.Serializable;

public class Expense extends Transaction implements Serializable {
    public Expense(double amount, Category category) {
        super(amount, category);
    }
}
