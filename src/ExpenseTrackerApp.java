import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


class BudgetExceededException extends Exception {
    public BudgetExceededException(String message) {
        super(message);
    }
}

class InvalidAmountException extends RuntimeException {
    public InvalidAmountException(String message) {
        super(message);
    }
}


interface Reportable {
    void generateReport();
}

abstract class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    protected String category;
    protected double amount;
    protected LocalDate date;

    public Transaction(String category, double amount, LocalDate date) {
        this.category = category;
        this.amount = amount;
        this.date = date;
    }

    public abstract String getTransactionType();

    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }

    @Override
    public String toString() {
        return String.format("[%s] %s: ₹%.2f on %s", getTransactionType(), category, amount, date);
    }
}


final class Expense extends Transaction {
    private String paymentMethod;

    public Expense(String category, double amount, LocalDate date, String paymentMethod) {
        super(category, amount, date);
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String getTransactionType() {
        return "EXPENSE";
    }

    public String getPaymentMethod() { return paymentMethod; }
}


class DataRepository<T> {
    private List<T> items = new ArrayList<>();

    public void add(T item) {
        items.add(item);
    }

    public List<T> getAll() {
        return items;
    }

    public void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(items);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
        File file = new File(filename);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
                items = (List<T>) ois.readObject();
            }
        }
    }
}


class BudgetMonitorThread extends Thread {
    private double currentSpending;
    private double monthlyBudget;
    private boolean active = true;

    public BudgetMonitorThread(double budget) {
        this.monthlyBudget = budget;
        setPriority(Thread.MIN_PRIORITY);
    }

    public synchronized void updateSpending(double spending) {
        this.currentSpending = spending;
        notify();
    }

    @Override
    public void run() {
        while (active) {
            synchronized (this) {
                try {
                    wait(5000); // check spending status every 5 seconds
                    if (currentSpending > monthlyBudget * 0.85) {
                        System.out.println("[ALERT] Heads up! You've crossed 85% of your monthly budget.");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void stopThread() {
        active = false;
    }
}


class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:expenses.db";

    public static void initializeDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "category TEXT, amount REAL, date TEXT)";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("DB Setup Error: " + e.getMessage());
        }
    }

    public static void saveExpenseToDB(Expense exp) {
        String sql = "INSERT INTO expenses(category, amount, date) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, exp.getCategory());
            pstmt.setDouble(2, exp.getAmount());
            pstmt.setString(3, exp.getDate().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}


public class ExpenseTrackerApp extends JFrame implements Reportable {
    private DataRepository<Transaction> repository = new DataRepository<>();
    private BudgetMonitorThread monitorThread;

    private JTextField categoryField, amountField;
    private JComboBox<String> paymentMethodBox;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;
    private final double monthlyBudget = 25000.0; // Monthly limit set in Rupees

    public static class AppConfig {
        public static final String TITLE = "Personal Expense & Daily Budget Tracker";
        public static final String DATA_FILE = "expenses.dat";
    }

    public ExpenseTrackerApp() {
        setTitle(AppConfig.TITLE);
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        DatabaseManager.initializeDB();
        monitorThread = new BudgetMonitorThread(monthlyBudget);
        monitorThread.start();

        try {
            repository.loadFromFile(AppConfig.DATA_FILE);
        } catch (Exception e) {
            System.out.println("Starting with a clean state.");
        }

        setupUI();
        refreshDisplay();
    }

    private void setupUI() {
        setLayout(new BorderLayout(10, 10));


        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.add(new JLabel("Category:"));
        categoryField = new JTextField(12);
        inputPanel.add(categoryField);

        inputPanel.add(new JLabel("Amount (₹):"));
        amountField = new JTextField(8);
        inputPanel.add(amountField);

        inputPanel.add(new JLabel("Payment Mode:"));
        paymentMethodBox = new JComboBox<>(new String[]{"UPI", "Credit Card", "Cash", "Bank Transfer"});
        inputPanel.add(paymentMethodBox);

        JButton addButton = new JButton("Add Expense");
        inputPanel.add(addButton);
        add(inputPanel, BorderLayout.NORTH);


        tableModel = new DefaultTableModel(new Object[]{"Type", "Category", "Amount", "Date", "Details"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);


        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        totalLabel = new JLabel("Total Spent: ₹0.00");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        bottomPanel.add(totalLabel);

        JButton reportButton = new JButton("Generate Report");
        bottomPanel.add(reportButton);
        add(bottomPanel, BorderLayout.SOUTH);


        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processNewExpense();
            }
        });

        reportButton.addActionListener(e -> generateReport());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                monitorThread.stopThread();
                try {
                    repository.saveToFile(AppConfig.DATA_FILE);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void addTransaction(String category, double amount) {
        addTransaction(category, amount, LocalDate.now(), "UPI");
    }

    public void addTransaction(String category, double amount, LocalDate date, String paymentMethod) {
        try {
            assert amount > 0 : "Amount must be positive";

            if (amount <= 0) {
                throw new InvalidAmountException("Expense amount must be greater than zero.");
            }

            Expense expense = new Expense(category, amount, date, paymentMethod);
            repository.add(expense);
            DatabaseManager.saveExpenseToDB(expense);


            double totalSpent = repository.getAll().stream()
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            if (totalSpent > monthlyBudget) {
                throw new BudgetExceededException("Warning: Monthly budget of ₹" + monthlyBudget + " has been exceeded!");
            }

            monitorThread.updateSpending(totalSpent);
            refreshDisplay();

            categoryField.setText("");
            amountField.setText("");

        } catch (InvalidAmountException | BudgetExceededException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Alert", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processNewExpense() {
        try {
            String cat = categoryField.getText().trim();
            double amt = Double.parseDouble(amountField.getText().trim());
            String mode = (String) paymentMethodBox.getSelectedItem();

            if (cat.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a valid category.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            addTransaction(cat, amt, LocalDate.now(), mode);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for the amount.", "Format Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshDisplay() {
        tableModel.setRowCount(0);
        List<Transaction> list = repository.getAll();

        for (Transaction t : list) {
            String details = "";

            if (t instanceof Expense) {
                details = "Via: " + ((Expense) t).getPaymentMethod();
            }
            tableModel.addRow(new Object[]{
                    t.getTransactionType(),
                    t.getCategory(),
                    String.format("₹%.2f", t.getAmount()),
                    t.getDate().format(DateTimeFormatter.ISO_DATE),
                    details
            });
        }

        double total = list.stream().mapToDouble(Transaction::getAmount).sum();
        totalLabel.setText(String.format("Total Spent: ₹%.2f / Limit: ₹%.2f", total, monthlyBudget));
    }

    @Override
    public void generateReport() {

        Map<String, Double> categoryMap = repository.getAll().stream()
                .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));

        StringBuilder summary = new StringBuilder("--- Category-wise Expense Report ---\n\n");
        categoryMap.forEach((cat, total) -> summary.append(String.format("%s -> ₹%.2f\n", cat, total)));

        JOptionPane.showMessageDialog(this, summary.toString(), "Summary Report", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ExpenseTrackerApp().setVisible(true);
        });
    }
}