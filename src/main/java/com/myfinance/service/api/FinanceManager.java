package com.myfinance.service.api;

import com.myfinance.model.Category;
import com.myfinance.model.User;
import com.myfinance.model.Wallet;

import java.util.Optional;

public interface FinanceManager {
    Category getOrCreateCategory(final String name);
    boolean register(final String login, final String password);
    boolean login(final String login, final String password);
    void logout();
    Optional<User> getCurrentUser();
    Optional<Wallet> getCurrentUserWallet();
    boolean transfer(final String toLogin, final double amount, final String categoryName);
    double getWalletBalance();
    Optional<Category> getCategoryByName(String name);
}

