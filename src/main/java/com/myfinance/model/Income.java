package com.myfinance.model;

import java.io.Serializable;

public class Income extends Transaction implements Serializable {
    public Income(double amount, Category category) {
        super(amount, category);
    }
}
