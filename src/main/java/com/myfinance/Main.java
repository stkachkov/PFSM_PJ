package com.myfinance;

import com.myfinance.model.Category;
import com.myfinance.model.Expense;
import com.myfinance.model.Income;
import com.myfinance.model.Transaction;
import com.myfinance.model.Wallet;
import com.myfinance.service.FinanceManagerImpl;
import com.myfinance.service.ReportGeneratorImpl;
import com.myfinance.service.api.FinanceManager;
import com.myfinance.service.api.ReportGenerator;
import com.myfinance.service.api.CsvExportImportService;
import com.myfinance.service.CsvExportImportServiceImpl;

import java.io.*;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    private static final FinanceManager financeManager = new FinanceManagerImpl();
    private static final ReportGenerator reportGenerator = new ReportGeneratorImpl();
    private static final CsvExportImportService csvService = new CsvExportImportServiceImpl(financeManager);
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(final String[] args) {
        System.out.println("Добро пожаловать в приложение для управления личными финансами!");

        //noinspection InfiniteLoopStatement
        while (true) {
            if (financeManager.getCurrentUser().isEmpty()) {
                showAuthMenu();
            }
            else {
                showMainMenu();
            }
        }
    }

    private static void showAuthMenu() {
        System.out.println("\n1. Регистрация");
        System.out.println("2. Авторизация");
        System.out.println("3. Выход");
        System.out.print("Выберите действие: ");

        final int choice = readInt();

        switch (choice) {
            case 1:
                registerUser();
                break;
            case 2:
                loginUser();
                break;
            case 3:
                System.out.println("До свидания!");
                System.exit(0);
                break;
            default:
                System.out.println("Неверный выбор. Попробуйте снова.");
        }
    }

    private static void showMainMenu() {
        financeManager.getCurrentUser().ifPresent(user -> {
            System.out.println("\nВы вошли как: " + user.getLogin());
            System.out.println("1. Добавить доход");
            System.out.println("2. Добавить расход");
            System.out.println("3. Установить бюджет");
            System.out.println("4. Показать отчет");
            System.out.println("5. Отчет по категориям");
            System.out.println("6. Экспорт отчета");
            System.out.println("7. Перевести средства");
            System.out.println("8. Редактировать бюджет");
            System.out.println("9. Помощь");
            System.out.println("10. Просмотр баланса");
            System.out.println("11. Экспорт в CSV");
            System.out.println("12. Импорт из CSV");
            System.out.println("13. Выйти из аккаунта");
            System.out.print("Выберите действие: ");

            final int choice = readInt();

            switch (choice) {
                case 1:
                    addIncome();
                    break;
                case 2:
                    addExpense();
                    break;
                case 3:
                    setBudget();
                    break;
                case 4:
                    showReport();
                    break;
                case 5:
                    showReportByCategories();
                    break;
                case 6:
                    exportReport();
                    break;
                case 7:
                    transferFunds();
                    break;
                case 8:
                    editBudget();
                    break;
                case 9:
                    showHelp();
                    break;
                case 10:
                    showBalance();
                    break;
                case 11:
                    exportToCsv();
                    break;
                case 12:
                    importFromCsv();
                    break;
                case 13:
                    logoutUser();
                    break;
                default:
                    System.out.println("Неверный выбор. Попробуйте снова.");
            }
        });
    }

    private static void showBalance() {
        System.out.printf("Текущий баланс: %.2f%n", financeManager.getWalletBalance());
    }

    private static void exportToCsv() {
        financeManager.getCurrentUser().ifPresent(user ->
            financeManager.getCurrentUserWallet().ifPresent(wallet ->
                csvService.exportToCsv(wallet, user.getLogin())
            )
        );
    }

    private static void importFromCsv() {
        System.out.println("ВНИМАНИЕ! Импорт заменит все текущие транзакции и бюджеты.");
        String answer = readString("Вы уверены, что хотите продолжить? (да/нет): ");
        if (!answer.equalsIgnoreCase("да")) {
            System.out.println("Импорт отменен.");
            return;
        }

        financeManager.getCurrentUser().ifPresent(user ->
            financeManager.getCurrentUserWallet().ifPresent(wallet ->
                csvService.importFromCsv(wallet, user.getLogin())
            )
        );
    }

    private static void registerUser() {
        final String login = readString("Введите логин: ");
        final String password = readString("Введите пароль: ");
        if (financeManager.register(login, password)) {
            System.out.println("Пользователь успешно зарегистрирован.");
            financeManager.login(login, password);
            System.out.println("Авторизация прошла успешно.");
        } else {
            System.out.println("Пользователь с таким логином уже существует.");
        }
    }

    private static void loginUser() {
        final String login = readString("Введите логин: ");
        final String password = readString("Введите пароль: ");

        if (financeManager.login(login, password)) {
            System.out.println("Авторизация прошла успешно.");
        } else {
            System.out.println("Неверный логин или пароль.");
        }
    }
    
    private static void logoutUser() {
        financeManager.logout();
        System.out.println("Вы вышли из аккаунта.");
    }

    private static void addIncome() {
        financeManager.getCurrentUserWallet().ifPresent(wallet -> {
            final double amount = readPositiveDouble("Введите сумму дохода: ");
            final String categoryName = readString("Введите категорию дохода: ");

            final Category category = financeManager.getOrCreateCategory(categoryName);
            final Income income = new Income(amount, category);
            wallet.addTransaction(income);
            System.out.println("Доход успешно добавлен.");
        });
    }

    private static void addExpense() {
        financeManager.getCurrentUserWallet().ifPresent(wallet -> {
            final double amount = readPositiveDouble("Введите сумму расхода: ");
            final String categoryName = readString("Введите категорию расхода: ");

            final Category category = financeManager.getOrCreateCategory(categoryName);
            final Expense expense = new Expense(amount, category);
            wallet.addTransaction(expense);
            System.out.println("Расход успешно добавлен.");
            checkBudget(category);
            checkOverallBalance(wallet);
        });
    }

    private static void checkOverallBalance(final Wallet wallet) {
        final double totalIncome = wallet.getTransactions().stream()
                .filter(t -> t instanceof Income)
                .mapToDouble(Transaction::getAmount)
                .sum();
        final double totalExpense = wallet.getTransactions().stream()
                .filter(t -> t instanceof Expense)
                .mapToDouble(Transaction::getAmount)
                .sum();

        if (totalExpense > totalIncome) {
            System.out.println("Внимание! Ваши расходы превышают доходы!");
        }
    }

    private static void setBudget() {
        financeManager.getCurrentUserWallet().ifPresent(wallet -> {
            final String categoryName = readString("Введите категорию для бюджета: ");
            final double amount = readPositiveDouble("Введите сумму бюджета: ");

            final Category category = financeManager.getOrCreateCategory(categoryName);
            wallet.setBudget(category, amount);
            System.out.println("Бюджет для категории '" + categoryName + "' установлен.");
        });
    }

    private static void editBudget() {
        financeManager.getCurrentUserWallet().ifPresent(wallet -> {
            final String categoryName = readString("Введите категорию для редактирования бюджета: ");
            final double amount = readPositiveDouble("Введите новую сумму бюджета: ");

            final Category category = financeManager.getOrCreateCategory(categoryName);
            wallet.setBudget(category, amount);
            System.out.println("Бюджет для категории '" + categoryName + "' обновлен.");
        });
    }

    private static void showReport() {
        financeManager.getCurrentUserWallet().ifPresent(wallet -> {
            final String report = reportGenerator.generateFullReport(wallet);
            System.out.println("\n--- Финансовый отчет ---");
            System.out.println(report);
        });
    }

    private static void showReportByCategories() {
        financeManager.getCurrentUserWallet().ifPresent(wallet -> {
            System.out.println("Введите категории для отчета через запятую (например: Еда, Такси):");
            final String[] categoryNames = scanner.nextLine().split(",");
            final java.util.List<Category> categories = new java.util.ArrayList<>();
            for (final String name : categoryNames) {
                final String trimmedName = name.trim();
                final Optional<Category> categoryOpt = financeManager.getCategoryByName(trimmedName);
                if (categoryOpt.isPresent()) {
                    categories.add(categoryOpt.get());
                } else {
                    System.out.println("Категория '" + trimmedName + "' не найдена и будет проигнорирована.");
                }
            }
            if (categories.isEmpty()) {
                System.out.println("Не выбрано ни одной существующей категории для отчета.");
                return;
            }
            final String report = reportGenerator.generateReportByCategories(wallet, categories);
            System.out.println("\n--- Отчет по категориям ---");
            System.out.println(report);
        });
    }

    private static void exportReport() {
        financeManager.getCurrentUser().ifPresent(user -> financeManager.getCurrentUserWallet().ifPresent(wallet -> {
            final String report = reportGenerator.generateFullReport(wallet);
            final String fileName = user.getLogin() + "_report.txt";
            try (final java.io.FileWriter writer = new java.io.FileWriter(fileName)) {
                writer.write(report);
                System.out.println("Отчет успешно экспортирован в файл " + fileName);
            } catch (final java.io.IOException e) {
                System.out.println("Ошибка при экспорте отчета: " + e.getMessage());
            }
        }));
    }

    private static void showHelp() {
        System.out.println("\n--- Помощь ---");
        System.out.println("1. Добавить доход - добавляет новую запись о доходе.");
        System.out.println("2. Добавить расход - добавляет новую запись о расходе.");
        System.out.println("3. Установить бюджет - устанавливает лимит расходов на категорию.");
        System.out.println("4. Показать отчет - выводит полный финансовый отчет.");
        System.out.println("5. Отчет по категориям - выводит отчет по выбранным категориям.");
        System.out.println("6. Экспорт отчета - сохраняет полный отчет в текстовый файл.");
        System.out.println("7. Перевести средства - переводит деньги другому пользователю.");
        System.out.println("8. Редактировать бюджет - изменяет существующий бюджет.");
        System.out.println("9. Помощь - показывает это сообщение.");
        System.out.println("10. Просмотр баланса - показывает текущий баланс кошелька.");
        System.out.println("11. Экспорт в CSV - сохраняет транзакции и бюджеты в CSV файлы.");
        System.out.println("12. Импорт из CSV - загружает транзакции и бюджеты из CSV файлов, заменяя текущие данные.");
        System.out.println("13. Выйти из аккаунта - выходит из текущего аккаунта.");
    }

    private static void transferFunds() {
        final String toLogin = readString("Введите логин получателя: ");
        final double amount = readPositiveDouble("Введите сумму перевода: ");
        final String categoryName = readString("Введите категорию перевода: ");

        if (financeManager.transfer(toLogin, amount, categoryName)) {
            System.out.println("Перевод выполнен успешно.");
        } else {
            System.out.println("Не удалось выполнить перевод. Проверьте данные и баланс.");
        }
    }

    private static void checkBudget(final Category category) {
        financeManager.getCurrentUserWallet().ifPresent(wallet -> {
            final Double budget = wallet.getBudgets().get(category);
            if (budget != null) {
                final double totalSpent = wallet.getTransactions().stream()
                        .filter(t -> t instanceof Expense && t.getCategory().getName().equals(category.getName()))
                        .mapToDouble(Transaction::getAmount)
                        .sum();
                if (totalSpent > budget) {
                    System.out.println("Внимание! Превышен бюджет по категории '" + category.getName() + "'.");
                } else if (totalSpent >= budget * 0.8) {
                    System.out.println("Внимание! Вы потратили более 80% бюджета по категории '" + category.getName() + "'.");
                }
            }
        });
    }

    private static String readString(final String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        while (input.isBlank()) {
            System.out.println("Ввод не может быть пустым.");
            System.out.print(prompt);
            input = scanner.nextLine();
        }
        return input;
    }

    private static int readInt() {
        while (!scanner.hasNextInt()) {
            System.out.println("Это не число. Пожалуйста, введите число.");
            scanner.next();
        }
        final int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }

    private static double readDouble() {
        while (!scanner.hasNextDouble()) {
            System.out.println("Это не число. Пожалуйста, введите число.");
            scanner.next();
        }
        final double value = scanner.nextDouble();
        scanner.nextLine();
        return value;
    }

    private static double readPositiveDouble(final String prompt) {
        System.out.print(prompt);
        double amount = readDouble();
        while (amount <= 0) {
            System.out.println("Сумма должна быть положительной.");
            System.out.print(prompt);
            amount = readDouble();
        }
        return amount;
    }
}

