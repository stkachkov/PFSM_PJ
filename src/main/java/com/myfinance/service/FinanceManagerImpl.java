package com.myfinance.service;

import com.myfinance.model.*;
import com.myfinance.service.api.FinanceManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FinanceManagerImpl implements FinanceManager {
    private final String dataDirectory;
    private final String usersFilePath;
    private Map<String, User> users = new HashMap<>();
    private final Map<String, Wallet> userWallets = new HashMap<>();
    private final Map<String, Category> categories = new HashMap<>();
    private User currentUser;

    public FinanceManagerImpl() {
        this("data");
    }

    public FinanceManagerImpl(final String dataDirectory) {
        this.dataDirectory = dataDirectory;
        try {
            Files.createDirectories(Paths.get(dataDirectory));
        } catch (IOException e) {
            System.out.println("Не удалось создать директорию для данных: " + e.getMessage());
        }
        this.usersFilePath = Paths.get(dataDirectory, "users.dat").toString();
        loadUsers();
    }

    @Override
    public Category getOrCreateCategory(final String name) {
        return categories.computeIfAbsent(name, Category::new);
    }

    @Override
    public Optional<Category> getCategoryByName(String name) {
        return Optional.ofNullable(categories.get(name));
    }

    @Override
    public boolean register(final String login, final String password) {
        if (users.containsKey(login)) {
            return false;
        }
        final User newUser = new User(login, password);
        users.put(login, newUser);
        userWallets.put(login, new Wallet());
        saveUsers();
        saveWallet(login);
        return true;
    }

    @Override
    public boolean login(final String login, final String password) {
        final User user = users.get(login);
        if (user != null && user.getPassword().equals(password)) {
            currentUser = user;
            loadWallet(login);
            return true;
        }
        return false;
    }

    @Override
    public void logout() {
        if (currentUser != null) {
            saveWallet(currentUser.getLogin());
            currentUser = null;
        }
    }

    @Override
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    @Override
    public Optional<Wallet> getCurrentUserWallet() {
        return Optional.ofNullable(currentUser).map(user -> userWallets.get(user.getLogin()));
    }

    @Override
    public double getWalletBalance() {
        return getCurrentUserWallet().map(Wallet::getBalance).orElse(0.0);
    }

    @Override
    public boolean transfer(final String toLogin, final double amount, final String categoryName) {
        if (currentUser == null) {
            return false;
        }
        if (!users.containsKey(toLogin)) {
            return false;
        }
        if (currentUser.getLogin().equals(toLogin)) {
            return false;
        }

        final Optional<Wallet> senderWalletOpt = getCurrentUserWallet();
        if (senderWalletOpt.isEmpty() || senderWalletOpt.get().getBalance() < amount) {
            return false;
        }
        final Wallet senderWallet = senderWalletOpt.get();

        loadWallet(toLogin);
        final Wallet recipientWallet = userWallets.get(toLogin);

        final Category category = getOrCreateCategory(categoryName);
        final Expense expense = new Expense(amount, category);
        final Income income = new Income(amount, category);

        senderWallet.addTransaction(expense);
        recipientWallet.addTransaction(income);

        saveWallet(toLogin);

        return true;
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        final File file = new File(usersFilePath);
        if (file.exists()) {
            try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                users = (Map<String, User>) ois.readObject();
            } catch (final IOException | ClassNotFoundException e) {
                System.out.println("Ошибка при загрузке пользователей: " + e.getMessage());
            }
        }
    }

    private void saveUsers() {
        try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(usersFilePath))) {
            oos.writeObject(users);
        } catch (final IOException e) {
            System.out.println("Ошибка при сохранении пользователей: " + e.getMessage());
        }
    }

    private void loadWallet(final String login) {
        final Path walletPath = Paths.get(dataDirectory, login + "_wallet.dat");
        final File file = walletPath.toFile();
        if (file.exists()) {
            try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                final Wallet wallet = (Wallet) ois.readObject();
                userWallets.put(login, wallet);
            } catch (final IOException | ClassNotFoundException e) {
                System.out.println("Ошибка при загрузке кошелька: " + e.getMessage());
                userWallets.put(login, new Wallet());
            }
        } else {
            userWallets.put(login, new Wallet());
        }
    }

    private void saveWallet(final String login) {
        final Wallet wallet = userWallets.get(login);
        if (wallet != null) {
            final Path walletPath = Paths.get(dataDirectory, login + "_wallet.dat");
            final File file = walletPath.toFile();
            try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(wallet);
            } catch (final IOException e) {
                System.out.println("Ошибка при сохранении кошелька: " + e.getMessage());
            }
        }
    }
}
