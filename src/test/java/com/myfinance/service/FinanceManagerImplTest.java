package com.myfinance.service;

import com.myfinance.model.*;
import com.myfinance.service.api.FinanceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FinanceManagerImplTest {

    private FinanceManager financeManager;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("pfms_test_");
        financeManager = new FinanceManagerImpl(tempDir.toString());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }


    @Test
    void testRegisterUser() {
        assertTrue(financeManager.register("testuser", "password"));
        assertTrue(financeManager.getCurrentUser().isEmpty());
        assertTrue(financeManager.login("testuser", "password"));
        assertTrue(financeManager.getCurrentUser().isPresent());
        assertEquals("testuser", financeManager.getCurrentUser().get().getLogin());
    }

    @Test
    void testRegisterExistingUser() {
        financeManager.register("testuser", "password");

        assertFalse(financeManager.register("testuser", "password"));
    }

    @Test
    void testLoginUser() {
        financeManager.register("testuser", "password");
        financeManager.logout();

        assertTrue(financeManager.login("testuser", "password"));
        assertTrue(financeManager.getCurrentUser().isPresent());
        assertEquals("testuser", financeManager.getCurrentUser().get().getLogin());
    }

    @Test
    void testLoginNonExistingUser() {
        assertFalse(financeManager.login("nonexistent", "password"));
    }

    @Test
    void testLoginWrongPassword() {
        financeManager.register("testuser", "password");
        financeManager.logout();

        assertFalse(financeManager.login("testuser", "wrongpassword"));
    }

    @Test
    void testLogout() {
        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");
        financeManager.logout();

        assertTrue(financeManager.getCurrentUser().isEmpty());
    }

    @Test
    void testAddIncome() {
        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");

        final Optional<Wallet> walletOpt = financeManager.getCurrentUserWallet();
        assertTrue(walletOpt.isPresent());
        final Wallet wallet = walletOpt.get();
        final Category category = financeManager.getOrCreateCategory("Salary");
        final Income income = new Income(1000, category);

        wallet.addTransaction(income);

        assertEquals(1000, wallet.getBalance());
        assertEquals(1, wallet.getTransactions().size());
    }

    @Test
    void testAddExpense() {
        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");

        final Optional<Wallet> walletOpt = financeManager.getCurrentUserWallet();
        assertTrue(walletOpt.isPresent());
        final Wallet wallet = walletOpt.get();
        final Category category = financeManager.getOrCreateCategory("Groceries");

        final Expense expense = new Expense(100, category);
        wallet.addTransaction(expense);

        assertEquals(-100, wallet.getBalance());
        assertEquals(1, wallet.getTransactions().size());
    }

    @Test
    void testGetBalance() {
        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");
        final Optional<Wallet> walletOpt = financeManager.getCurrentUserWallet();
        assertTrue(walletOpt.isPresent());
        walletOpt.get().addTransaction(new Income(500, financeManager.getOrCreateCategory("Salary")));

        assertEquals(500, financeManager.getWalletBalance());
    }

    @Test
    void testGetCategoryByName() {
        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");

        final Category existingCategory = financeManager.getOrCreateCategory("Food");
        assertTrue(financeManager.getCategoryByName("Food").isPresent());
        assertEquals(existingCategory, financeManager.getCategoryByName("Food").get());

        assertTrue(financeManager.getCategoryByName("NonExistentCategory").isEmpty());
    }

    @Test
    void testSetBudget() {
        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");

        final Optional<Wallet> walletOpt = financeManager.getCurrentUserWallet();
        assertTrue(walletOpt.isPresent());
        final Wallet wallet = walletOpt.get();
        final Category category = financeManager.getOrCreateCategory("Groceries");

        wallet.setBudget(category, 500);

        assertEquals(500, wallet.getBudgets().get(category));
    }

    @Test
    void testTransfer() {
        financeManager.register("testuser", "password");
        financeManager.register("testuser2", "password");
        financeManager.login("testuser", "password");

        final Optional<Wallet> wallet1Opt = financeManager.getCurrentUserWallet();
        assertTrue(wallet1Opt.isPresent());
        final Wallet wallet1 = wallet1Opt.get();
        final Income income = new Income(1000, financeManager.getOrCreateCategory("Salary"));
        wallet1.addTransaction(income);

        assertEquals(1000, wallet1.getBalance());
        assertTrue(financeManager.transfer("testuser2", 300, "Gift"));
        assertEquals(700, wallet1.getBalance());
    }

    @Test
    void testTransferToNonExistentUser() {
        financeManager.register("sender", "password");
        financeManager.login("sender", "password");
        financeManager.getCurrentUserWallet().ifPresent(wallet -> wallet.addTransaction(new Income(1000, financeManager.getOrCreateCategory("Salary"))));

        assertFalse(financeManager.transfer("nonexistent", 500, "Gift"));
        assertEquals(1000, financeManager.getWalletBalance());
    }

    @Test
    void testTransferInsufficientFunds() {
        financeManager.register("sender", "password");
        financeManager.register("receiver", "password");
        financeManager.login("sender", "password");
        financeManager.getCurrentUserWallet().ifPresent(wallet -> wallet.addTransaction(new Income(300, financeManager.getOrCreateCategory("Salary"))));

        assertFalse(financeManager.transfer("receiver", 500, "Gift"));
        assertEquals(300, financeManager.getWalletBalance());
    }

    @Test
    void testTransferExactBalance() {
        financeManager.register("sender", "password");
        financeManager.register("receiver", "password");
        financeManager.login("sender", "password");
        var walletOpt = financeManager.getCurrentUserWallet();
        assertTrue(walletOpt.isPresent());
        walletOpt.get().addTransaction(new Income(100, financeManager.getOrCreateCategory("Salary")));

        assertTrue(financeManager.transfer("receiver", 100, "Gift"));
        assertEquals(0, financeManager.getWalletBalance());
    }

    @Test
    void testDataPersistence() {
        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");

        final Optional<Wallet> walletOpt = financeManager.getCurrentUserWallet();
        assertTrue(walletOpt.isPresent());
        final Wallet wallet = walletOpt.get();
        final Category category = financeManager.getOrCreateCategory("Salary");
        final Income income = new Income(1000, category);

        wallet.addTransaction(income);
        financeManager.logout();

        final FinanceManager newFinanceManager = new FinanceManagerImpl(tempDir.toString());
        assertTrue(newFinanceManager.login("testuser", "password"));
        final Optional<Wallet> newWalletOpt = newFinanceManager.getCurrentUserWallet();
        assertTrue(newWalletOpt.isPresent());
        final Wallet newWallet = newWalletOpt.get();

        assertEquals(1000, newWallet.getBalance());
        assertEquals(1, newWallet.getTransactions().size());
    }
}
