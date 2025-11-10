package com.myfinance.service;

import com.myfinance.model.*;
import com.myfinance.service.api.CsvExportImportService;
import com.myfinance.service.api.FinanceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class CsvExportImportServiceImplTest {

    private FinanceManager financeManager;
    private CsvExportImportService csvService;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDir = Files.createTempDirectory("pfms_csv_test_");
        financeManager = new FinanceManagerImpl(tempDir.toString());
        csvService = new CsvExportImportServiceImpl(financeManager);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void tearDown() {
        
        new File("testuser_transactions.csv").delete();
        new File("testuser_budgets.csv").delete();
    }

    @Test
    void testExportAndImportCsv_Success() {
        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");

        var walletOpt = financeManager.getCurrentUserWallet();
        assertTrue(walletOpt.isPresent());
        Wallet wallet = walletOpt.get();
        wallet.addTransaction(new Income(1000, financeManager.getOrCreateCategory("Salary")));
        wallet.addTransaction(new Expense(150, financeManager.getOrCreateCategory("Food")));
        wallet.setBudget(financeManager.getOrCreateCategory("Food"), 500.0);

        
        csvService.exportToCsv(wallet, "testuser");

        
        Wallet newWallet = new Wallet();
        csvService.importFromCsv(newWallet, "testuser");

        assertEquals(2, newWallet.getTransactions().size());
        assertEquals(850.0, newWallet.getBalance());
        assertEquals(500.0, newWallet.getBudgets().get(financeManager.getOrCreateCategory("Food")));
    }

    @Test
    void testImportCsv_WrongColumnCount() throws IOException {
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("testuser_transactions.csv"))) {
            writer.write("Type,Amount,Category\n");
            writer.write("INCOME,1000\n"); 
        }
        Files.writeString(Paths.get("testuser_budgets.csv"), "");


        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");
        var walletOpt = financeManager.getCurrentUserWallet();
        assertTrue(walletOpt.isPresent());
        Wallet wallet = walletOpt.get();
        csvService.importFromCsv(wallet, "testuser");

        
        assertTrue(wallet.getTransactions().isEmpty());
    }

    @Test
    void testImportCsv_InvalidAmount() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("testuser_transactions.csv"))) {
            writer.write("Type,Amount,Category\n");
            writer.write("INCOME,abc,Salary\n"); 
        }
        Files.writeString(Paths.get("testuser_budgets.csv"), "");

        financeManager.register("testuser", "password");
        financeManager.login("testuser", "password");
        var walletOpt = financeManager.getCurrentUserWallet();
        assertTrue(walletOpt.isPresent());
        Wallet wallet = walletOpt.get();
        csvService.importFromCsv(wallet, "testuser");

        assertTrue(wallet.getTransactions().isEmpty());
    }
}
