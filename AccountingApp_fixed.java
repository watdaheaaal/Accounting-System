import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.text.NumberFormat;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/*
 Cleaned and fixed AccountingApp.java
 - Single file containing LoginForm, SignupForm and AccountingApp
 - Fixed missing refresh/update functions
 - Added Balance Sheet label fields for updating totals
 - DatabaseManager included (in-memory sqlite). Add sqlite-jdbc to classpath if you want DB features.
 Note: For production money use BigDecimal. This is for learning/demo.
*/

class LoginForm extends JFrame {
    private JTextField userField;
    private JPasswordField passField;

    public LoginForm() {
        setTitle("Login");
        setSize(350, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(new JLabel("Username:"));
        userField = new JTextField();
        panel.add(userField);

        panel.add(new JLabel("Password:"));
        passField = new JPasswordField();
        panel.add(passField);

        JButton loginBtn = new JButton("Login");
        JButton signupBtn = new JButton("Signup");

        panel.add(loginBtn);
        panel.add(signupBtn);

        add(panel);

        loginBtn.addActionListener(e -> login());
        signupBtn.addActionListener(e -> {
            new SignupForm();
            dispose();
        });

        setVisible(true);
    }

    private void login() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields.");
            return;
        }

        File f = new File("users.txt");
        if (!f.exists()) {
            JOptionPane.showMessageDialog(this, "No users found. Please signup first.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2 && parts[0].equals(user) && parts[1].equals(pass)) {
                    JOptionPane.showMessageDialog(this, "Login successful!");
                    new AccountingApp();
                    dispose();
                    return;
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error reading users file.");
        }

        JOptionPane.showMessageDialog(this, "Invalid username or password.");
    }
}

class SignupForm extends JFrame {
    private JTextField userField;
    private JPasswordField passField;

    public SignupForm() {
        setTitle("Signup");
        setSize(350, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(new JLabel("New Username:"));
        userField = new JTextField();
        panel.add(userField);

        panel.add(new JLabel("New Password:"));
        passField = new JPasswordField();
        panel.add(passField);

        JButton registerBtn = new JButton("Register");
        JButton backBtn = new JButton("Back");

        panel.add(registerBtn);
        panel.add(backBtn);

        add(panel);

        registerBtn.addActionListener(e -> signup());
        backBtn.addActionListener(e -> {
            new LoginForm();
            dispose();
        });

        setVisible(true);
    }

    private void signup() {
        String user = userField.getText().trim();
        String pass = new String(passField.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields.");
            return;
        }

        try (FileWriter fw = new FileWriter("users.txt", true)) {
            fw.write(user + ":" + pass + "\n");
            JOptionPane.showMessageDialog(this, "Signup successful!");
            new LoginForm();
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error writing user file.");
        }
    }
}

public class AccountingApp extends JFrame {
    // Core data
    private List<Account> accounts;
    private List<Transaction> transactions;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    // Table models for UI
    private DefaultTableModel transactionsTableModel;
    private DefaultTableModel accountsTableModel;
    private DefaultTableModel journalTableModel;
    private DefaultTableModel ledgerTableModel;
    private DefaultTableModel assetsTableModel;
    private DefaultTableModel liabilitiesTableModel;

    // UI controls that need cross-method access
    private JComboBox<String> ledgerAccountCombo;
    private JComboBox<String> debitComboGlobal;
    private JComboBox<String> creditComboGlobal;

    // Balance sheet labels (class fields so we can update them anywhere)
    private JLabel totalAssetsLabel;
    private JLabel totalLiabLabel;

    public AccountingApp() {
        setLayout(new BorderLayout());
        sdf.setLenient(false);

        accounts = new ArrayList<>();
        transactions = new ArrayList<>();
        addPredefinedAccounts();

        add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Add New Transaction", createAddTransactionPanel());
        tabbedPane.addTab("Transactions", createTransactionsPanel());
        tabbedPane.addTab("Accounts", createAccountsPanel());
        tabbedPane.addTab("General Journal", createGeneralJournalPanel());
        tabbedPane.addTab("General Ledger", createGeneralLedgerPanel());
        tabbedPane.addTab("Balance Sheet", createBalanceSheetPanel());

        add(tabbedPane, BorderLayout.CENTER);
        setTitle("Accounting App");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        refreshAllViews();
    }

    // ===================== HEADER / ABOUT =====================
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0, 128, 0));

        // Left side: company name + quote/description
        JPanel leftPanel = new JPanel(new GridLayout(2, 1));
        leftPanel.setOpaque(false);

        JLabel companyName = new JLabel("Accounting System");
        companyName.setForeground(Color.WHITE);
        companyName.setFont(new Font("Arial", Font.BOLD, 18));
        companyName.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));

        JLabel quoteLabel = new JLabel("<html><i>\"Organize your finances with clarity and efficiency.\"</i></html>");
        quoteLabel.setForeground(Color.WHITE);
        quoteLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        quoteLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        leftPanel.add(companyName);
        leftPanel.add(quoteLabel);

        // Right side: buttons and menu
        JPanel menuPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 10));
        menuPanel.setOpaque(false);

        JButton homeBtn = new JButton("Home");
        JButton aboutBtn = new JButton("About");

        JButton openFolderBtn = new JButton("Open New Folder");
        JButton saveBtn = new JButton("Save CSV");

        JButton menuBtn = new JButton("▼");
        JPopupMenu popup = new JPopupMenu();
        JMenuItem logoutItem = new JMenuItem("Sign out");
        popup.add(logoutItem);

        menuBtn.addActionListener(e -> popup.show(menuBtn, 0, menuBtn.getHeight()));
        logoutItem.addActionListener(e -> {
            dispose();
            new LoginForm();
        });

        aboutBtn.addActionListener(e -> showAbout());
        openFolderBtn.addActionListener(e -> openFolderAction());
        saveBtn.addActionListener(e -> saveCSVAction());

        menuPanel.add(homeBtn);
        menuPanel.add(aboutBtn);
        menuPanel.add(openFolderBtn);
        menuPanel.add(saveBtn);
        menuPanel.add(menuBtn);

        header.add(leftPanel, BorderLayout.WEST);
        header.add(menuPanel, BorderLayout.EAST);

        return header;
    }
private void openFolderAction() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int result = chooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File selectedDir = chooser.getSelectedFile();
        JOptionPane.showMessageDialog(this, "Opened folder: " + selectedDir.getAbsolutePath());
        // Optional: add logic to load CSV files from this folder
    }
}

private void saveCSVAction() {
    JFileChooser chooser = new JFileChooser();
    chooser.setSelectedFile(new File("goods.csv"));
    int result = chooser.showSaveDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(file)) {
            // Save all accounts as CSV
            pw.println("Account Name,Type,Balance");
            for (Account a : accounts) {
                pw.println(a.getName() + "," + a.getType() + "," + a.getBalance());
            }
            JOptionPane.showMessageDialog(this, "CSV saved: " + file.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage());
        }
    }
}

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "Accounting System\n\n" +
            "A complete accounting application with support for:\n" +
            "• Transaction management\n" +
            "• Account management\n" +
            "• General Journal and Ledger\n" +
            "• Balance Sheet\n" +
            "• Financial Reports\n\n" +
            "Made by Team Kahagbungon(Group 1)\n",
            "About",
            JOptionPane.INFORMATION_MESSAGE);
    }

    // ===================== Domain classes =====================
    private static class Account {
        private String name;
        private String type;
        private double balance;

        public Account(String name, String type, double initialBalance) {
            this.name = name;
            this.type = type;
            this.balance = initialBalance;
        }
        public String getName() { return name; }
        public String getType() { return type; }
        public double getBalance() { return balance; }

        public void applyDebit(double amount) {
            if (type.equals("Asset") || type.equals("Expense")) {
                balance += amount;
            } else {
                balance -= amount;
            }
        }
        public void applyCredit(double amount) {
            if (type.equals("Liability") || type.equals("Owner's Equity") || type.equals("Revenue")) {
                balance += amount;
            } else {
                balance -= amount;
            }
        }
    }

    private static class Transaction {
        private Date date;
        private String description;
        private String debitAccount;
        private String creditAccount;
        private double amount;

        public Transaction(Date date, String description, String debitAccount, String creditAccount, double amount) {
            this.date = date;
            this.description = description;
            this.debitAccount = debitAccount;
            this.creditAccount = creditAccount;
            this.amount = amount;
        }

        public Date getDate() { return date; }
        public String getDescription() { return description; }
        public String getDebitAccount() { return debitAccount; }
        public String getCreditAccount() { return creditAccount; }
        public double getAmount() { return amount; }
    }

    // ===================== Predefined accounts =====================
    private void addPredefinedAccounts() {
        String[][] predefined = {
            {"Cash", "Asset", "0"},
            {"Accounts Receivable", "Asset", "0"},
            {"Inventory", "Asset", "0"},
            {"Supplies", "Asset", "0"},
            {"Prepaid Expenses", "Asset", "0"},
            {"Equipment", "Asset", "0"},
            {"Furniture and Fixtures", "Asset", "0"},
            {"Land", "Asset", "0"},
            {"Buildings", "Asset", "0"},
            {"Accounts Payable", "Liability", "0"},
            {"Notes Payable", "Liability", "0"},
            {"Salaries Payable", "Liability", "0"},
            {"Rent Payable", "Liability", "0"},
            {"Interest Payable", "Liability", "0"},
            {"Unearned Revenue", "Liability", "0"},
            {"Owner's Capital", "Owner's Equity", "0"},
            {"Owner's Drawing", "Owner's Equity", "0"},
            {"Service Revenue", "Revenue", "0"},
            {"Sales Revenue", "Revenue", "0"},
            {"Interest Income", "Revenue", "0"},
            {"Salaries Expense", "Expense", "0"},
            {"Rent Expense", "Expense", "0"},
            {"Utilities Expense", "Expense", "0"},
            {"Supplies Expense", "Expense", "0"},
            {"Depreciation Expense", "Expense", "0"},
            {"Insurance Expense", "Expense", "0"},
            {"Advertising Expense", "Expense", "0"}
        };

        for (String[] acc : predefined) {
            accounts.add(new Account(acc[0], acc[1], Double.parseDouble(acc[2])));
        }
    }
    public void addAccount(String name, String type, double balance) {
    try {
        String sql = "INSERT INTO accounts (name, type, balance) VALUES (?, ?, ?)";
        PreparedStatement ps = db.getConnection().prepareStatement(sql);
        ps.setString(1, name);
        ps.setString(2, type);
        ps.setDouble(3, balance);
        ps.executeUpdate();
        ps.close();
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error adding account: " + e.getMessage());
    }
    }

    // ===================== Panels =====================
    private JPanel createAddTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField dateField = new JTextField(sdf.format(new Date()));
        JTextField descField = new JTextField();
        debitComboGlobal = new JComboBox<>();
        creditComboGlobal = new JComboBox<>();
        JTextField amountField = new JTextField();

        refreshAccountCombos();

        JButton postBtn = new JButton("Post Transaction");
        JButton clearBtn = new JButton("Clear Fields");

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1; form.add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; form.add(descField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Debit Account:"), gbc);
        gbc.gridx = 1; form.add(debitComboGlobal, gbc);

        gbc.gridx = 0; gbc.gridy = 3; form.add(new JLabel("Credit Account:"), gbc);
        gbc.gridx = 1; form.add(creditComboGlobal, gbc);

        gbc.gridx = 0; gbc.gridy = 4; form.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 1; form.add(amountField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; form.add(postBtn, gbc);
        gbc.gridx = 1; form.add(clearBtn, gbc);

        panel.add(form, BorderLayout.NORTH);

        postBtn.addActionListener(e -> {
            String dateStr = dateField.getText().trim();
            String desc = descField.getText().trim();
            String debitAccName = (String) debitComboGlobal.getSelectedItem();
            String creditAccName = (String) creditComboGlobal.getSelectedItem();
            String amtStr = amountField.getText().trim();

            Date date;
            try {
                date = sdf.parse(dateStr);
            } catch (ParseException ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amtStr);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Amount must be a number greater than zero.");
                return;
            }

            if (debitAccName == null || creditAccName == null) {
                JOptionPane.showMessageDialog(this, "Select both debit and credit accounts.");
                return;
            }
            if (debitAccName.equals(creditAccName)) {
                JOptionPane.showMessageDialog(this, "Debit and credit accounts cannot be the same.");
                return;
            }

            Account debitAcc = getAccountByName(debitAccName);
            Account creditAcc = getAccountByName(creditAccName);
            if (debitAcc == null || creditAcc == null) {
                JOptionPane.showMessageDialog(this, "Selected account not found.");
                return;
            }

            // Apply changes
            debitAcc.applyDebit(amount);
            creditAcc.applyCredit(amount);

            Transaction tx = new Transaction(date, desc, debitAccName, creditAccName, amount);
            transactions.add(tx);
            transactions.sort(Comparator.comparing(Transaction::getDate));

            refreshAllViews();

            JOptionPane.showMessageDialog(this, "Transaction posted.");
            dateField.setText(sdf.format(new Date()));
            descField.setText("");
            amountField.setText("");
            if (debitComboGlobal.getItemCount() > 0) debitComboGlobal.setSelectedIndex(0);
            if (creditComboGlobal.getItemCount() > 0) creditComboGlobal.setSelectedIndex(0);
        });

        clearBtn.addActionListener(e -> {
            dateField.setText(sdf.format(new Date()));
            descField.setText("");
            amountField.setText("");
            if (debitComboGlobal.getItemCount() > 0) debitComboGlobal.setSelectedIndex(0);
            if (creditComboGlobal.getItemCount() > 0) creditComboGlobal.setSelectedIndex(0);
        });

        return panel;
    }

    private JPanel createTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(30);
        JButton searchBtn = new JButton("Search");
        top.add(new JLabel("Search (date or description):"));
        top.add(searchField);
        top.add(searchBtn);

        String[] columns = {"Date", "Description", "Debit Account", "Credit Account", "Amount"};
        transactionsTableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(transactionsTableModel);
        table.setAutoCreateRowSorter(true);

        searchBtn.addActionListener(e -> {
            String query = searchField.getText().trim().toLowerCase();
            filterTransactions(query);
        });

        searchField.addActionListener(e -> filterTransactions(searchField.getText().trim().toLowerCase()));

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void filterTransactions(String query) {
        transactionsTableModel.setRowCount(0);
        for (int i = transactions.size()-1; i >= 0; i--) {
            Transaction tx = transactions.get(i);
            String dateStr = sdf.format(tx.getDate());
            if (query.isEmpty()
                    || dateStr.contains(query)
                    || tx.getDescription().toLowerCase().contains(query)
                    || tx.getDebitAccount().toLowerCase().contains(query)
                    || tx.getCreditAccount().toLowerCase().contains(query)) {
                transactionsTableModel.addRow(new Object[]{
                        dateStr,
                        tx.getDescription(),
                        tx.getDebitAccount(),
                        tx.getCreditAccount(),
                        formatAccounting(tx.getAmount())
                });
            }
        }
    }

    private JPanel createAccountsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"Account Name", "Type", "Current Balance"};
        accountsTableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = new JTable(accountsTableModel);
        table.setAutoCreateRowSorter(true);

        JPanel addPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField nameField = new JTextField();
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Asset", "Liability", "Owner's Equity", "Revenue", "Expense"});
        JTextField balanceField = new JTextField();
        JButton addBtn = new JButton("Add Account");

        gbc.gridx = 0; gbc.gridy = 0; addPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; addPanel.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; addPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1; addPanel.add(typeCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 2; addPanel.add(new JLabel("Initial Balance (optional):"), gbc);
        gbc.gridx = 1; addPanel.add(balanceField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; addPanel.add(new JLabel(""), gbc);
        gbc.gridx = 1; addPanel.add(addBtn, gbc);

        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter account name.");
                return;
            }
            if (getAccountByName(name) != null) {
                JOptionPane.showMessageDialog(this, "An account with this name already exists.");
                return;
            }
            String type = (String) typeCombo.getSelectedItem();
            double initBal = 0.0;
            if (!balanceField.getText().trim().isEmpty()) {
                try {
                    initBal = Double.parseDouble(balanceField.getText().trim());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid initial balance. Use a number.");
                    return;
                }
            }
            accounts.add(new Account(name, type, initBal));
            refreshAllViews();
            nameField.setText("");
            balanceField.setText("");
            typeCombo.setSelectedIndex(0);
            JOptionPane.showMessageDialog(this, "Account added.");
        });

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(addPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createGeneralJournalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] cols = {"Date", "Description", "Account", "Debit", "Credit"};
        journalTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = new JTable(journalTableModel);
        table.setAutoCreateRowSorter(true);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createGeneralLedgerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ledgerAccountCombo = new JComboBox<>();
        refreshLedgerAccountCombo();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Select Account:"));
        top.add(ledgerAccountCombo);

        String[] cols = {"Date", "Description", "Debit Account", "Credit Account", "Amount", "Running Balance"};
        ledgerTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = new JTable(ledgerTableModel);
        table.setAutoCreateRowSorter(true);

        ledgerAccountCombo.addActionListener(e -> {
            String acc = (String) ledgerAccountCombo.getSelectedItem();
            if (acc != null) updateGeneralLedgerTable(acc);
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBalanceSheetPanel() {
        JPanel panel = new JPanel(new GridLayout(1,2));

        JPanel assetsPanel = new JPanel(new BorderLayout());
        assetsPanel.add(new JLabel("Assets", SwingConstants.CENTER), BorderLayout.NORTH);
        String[] assetCols = {"Account Name", "Amount"};
        assetsTableModel = new DefaultTableModel(assetCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        }
        };
        JTable assetsTable = new JTable(assetsTableModel);
        assetsTable.setAutoCreateRowSorter(true);
        assetsPanel.add(new JScrollPane(assetsTable), BorderLayout.CENTER);

        totalAssetsLabel = new JLabel("", SwingConstants.RIGHT);
        assetsPanel.add(totalAssetsLabel, BorderLayout.SOUTH);

        JPanel liabilitiesPanel = new JPanel(new BorderLayout());
        liabilitiesPanel.add(new JLabel("Liabilities and Owner's Equity", SwingConstants.CENTER), BorderLayout.NORTH);
        String[] liabCols = {"Account Name", "Amount"};
        liabilitiesTableModel = new DefaultTableModel(liabCols, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable liabTable = new JTable(liabilitiesTableModel);
        liabTable.setAutoCreateRowSorter(true);
        liabilitiesPanel.add(new JScrollPane(liabTable), BorderLayout.CENTER);

        totalLiabLabel = new JLabel("", SwingConstants.RIGHT);
        liabilitiesPanel.add(totalLiabLabel, BorderLayout.SOUTH);

        panel.add(assetsPanel);
        panel.add(liabilitiesPanel);

        panel.putClientProperty("updateLabels", (Runnable) () -> {
            totalAssetsLabel.setText("Total Assets: " + formatAccounting(calculateTotalAssets()));
            totalLiabLabel.setText("Total Liabilities and Equity: " + formatAccounting(calculateTotalLiabilitiesAndEquity()));
        });

        return panel;
    }

    // ===================== Helpers =====================
    private String formatNumber(double value) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(value);
    }

    private String formatAccounting(double value) {
        if (value < 0) {
            return "(" + formatNumber(Math.abs(value)) + ")";
        }
        return formatNumber(value);
    }

    private Account getAccountByName(String name) {
        for (Account a : accounts) {
            if (a.getName().equals(name)) return a;
        }
        return null;
    }

    private List<String> getDebitAccountNames() {
        List<String> out = new ArrayList<>();
        for (Account a : accounts) out.add(a.getName());
        return out;
    }

    private List<String> getCreditAccountNames() {
        List<String> out = new ArrayList<>();
        for (Account a : accounts) out.add(a.getName());
        return out;
    }

    private List<String> getAllAccountNames() {
        List<String> out = new ArrayList<>();
        for (Account a : accounts) out.add(a.getName());
        return out;
    }

    private List<String> getAllAccountNamesSorted() {
        List<String> list = getAllAccountNames();
        Collections.sort(list);
        return list;
    }

    private void refreshAccountCombos() {
        List<String> debits = getDebitAccountNames();
        List<String> credits = getCreditAccountNames();

        if (debitComboGlobal != null) {
            debitComboGlobal.removeAllItems();
            for (String s : debits) debitComboGlobal.addItem(s);
        }
        if (creditComboGlobal != null) {
            creditComboGlobal.removeAllItems();
            for (String s : credits) creditComboGlobal.addItem(s);
        }
    }

    private void refreshLedgerAccountCombo() {
        if (ledgerAccountCombo == null) return;
        ledgerAccountCombo.removeAllItems();
        for (String s : getAllAccountNamesSorted()) ledgerAccountCombo.addItem(s);
        if (ledgerAccountCombo.getItemCount() > 0) ledgerAccountCombo.setSelectedIndex(0);
    }

    private void updateGeneralLedgerTable(String accountName) {
        ledgerTableModel.setRowCount(0);
        Account acc = getAccountByName(accountName);
        if (acc == null) return;

        double running = 0.0;
        for (Transaction tx : transactions) {
            double amount = tx.getAmount();
            String dateStr = sdf.format(tx.getDate());
            boolean added = false;
            if (tx.getDebitAccount().equals(accountName)) {
                if (acc.getType().equals("Asset") || acc.getType().equals("Expense")) {
                    running += amount;
                } else {
                    running -= amount;
                }
                ledgerTableModel.addRow(new Object[]{dateStr, tx.getDescription(), tx.getDebitAccount(), tx.getCreditAccount(),
                        formatAccounting(amount),
                        formatAccounting(running)});
                added = true;
            }
            if (tx.getCreditAccount().equals(accountName)) {
                if (acc.getType().equals("Liability") || acc.getType().equals("Owner's Equity") || acc.getType().equals("Revenue")) {
                    running += amount;
                } else {
                    running -= amount;
                }
                ledgerTableModel.addRow(new Object[]{dateStr, tx.getDescription(), tx.getDebitAccount(), tx.getCreditAccount(),
                        formatAccounting(amount),
                        formatAccounting(running)});
                added = true;
            }
            // If no direct debit/credit on this account, skip
        }
    }
    public void addTransaction(Date date, String desc, String debit, String credit, double amount) {
    try {
        String sql = "INSERT INTO transactions (tx_date, description, debit_account, credit_account, amount) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = db.getConnection().prepareStatement(sql);
        ps.setDate(1, new java.sql.Date(date.getTime()));
        ps.setString(2, desc);
        ps.setString(3, debit);
        ps.setString(4, credit);
        ps.setDouble(5, amount);
        ps.executeUpdate();
        ps.close();

        // Update balances
        updateAccountBalance(debit, amount, true);
        updateAccountBalance(credit, amount, false);

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(null, "Error adding transaction: " + e.getMessage());
    }
}

private void updateAccountBalance(String accName, double amount, boolean isDebit) throws SQLException {
    String getTypeSQL = "SELECT type, balance FROM accounts WHERE name=?";
    PreparedStatement ps = db.getConnection().prepareStatement(getTypeSQL);
    ps.setString(1, accName);
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
        String type = rs.getString("type");
        double balance = rs.getDouble("balance");

        if (isDebit) {
            if (type.equals("Asset") || type.equals("Expense")) balance += amount;
            else balance -= amount;
        } else {
            if (type.equals("Liability") || type.equals("Owner's Equity") || type.equals("Revenue")) balance += amount;
            else balance -= amount;
        }

        String updateSQL = "UPDATE accounts SET balance=? WHERE name=?";
        PreparedStatement ups = db.getConnection().prepareStatement(updateSQL);
        ups.setDouble(1, balance);
        ups.setString(2, accName);
        ups.executeUpdate();
        ups.close();
    }
    rs.close();
    ps.close();
    }

    private double calculateTotalAssets() {
        double sum = 0;
        for (Account a : accounts) if (a.getType().equals("Asset")) sum += a.getBalance();
        return sum;
    }

    private double calculateTotalLiabilitiesAndEquity() {
        double total = 0;
        double capital = 0;
        double drawing = 0;
        double revenue = 0;
        double expense = 0;

        for (Account a : accounts) {
            switch (a.getType()) {
                case "Liability":
                    total += a.getBalance();
                    break;
                case "Owner's Equity":
                    if (a.getName().equals("Owner's Capital"))
                        capital += a.getBalance();
                    else if (a.getName().equals("Owner's Drawing"))
                        drawing += a.getBalance();
                    break;
                case "Revenue":
                    revenue += a.getBalance();
                    break;
                case "Expense":
                    expense += a.getBalance();
                    break;
            }
        }

        double netIncome = revenue - expense;
        double equity = capital - drawing + netIncome;

        return total + equity;
    }

    private void refreshAllViews() {
        // Transactions tab
        if (transactionsTableModel != null) {
            filterTransactions("");
        }

        // Accounts tab
        if (accountsTableModel != null) {
            accountsTableModel.setRowCount(0);
            for (Account a : accounts) {
                accountsTableModel.addRow(new Object[]{a.getName(), a.getType(), formatAccounting(a.getBalance())});
            }
        }

        // Journal
        if (journalTableModel != null) {
            journalTableModel.setRowCount(0);
            for (Transaction tx : transactions) {
                String dateStr = sdf.format(tx.getDate());
                // debit row
                journalTableModel.addRow(new Object[]{
                        dateStr, tx.getDescription(), tx.getDebitAccount(),
                        formatAccounting(tx.getAmount()), ""
                });
                journalTableModel.addRow(new Object[]{
                        dateStr, tx.getDescription(), tx.getCreditAccount(),
                        "", formatAccounting(tx.getAmount())
                });
            }
        }

        // Ledger combos and table
        refreshLedgerAccountCombo();
        if (ledgerAccountCombo != null && ledgerAccountCombo.getItemCount() > 0) {
            String sel = (String) ledgerAccountCombo.getSelectedItem();
            if (sel == null) sel = ledgerAccountCombo.getItemAt(0);
            ledgerAccountCombo.setSelectedItem(sel);
            updateGeneralLedgerTable(sel);
        }

        // Balance sheet tables
        if (assetsTableModel != null && liabilitiesTableModel != null) {
            assetsTableModel.setRowCount(0);
            liabilitiesTableModel.setRowCount(0);
            for (Account a : accounts) {
                if (a.getType().equals("Asset")) {
                    assetsTableModel.addRow(new Object[]{a.getName(), formatAccounting(a.getBalance())});
                } else if (a.getType().equals("Liability") || a.getType().equals("Owner's Equity")) {
                    liabilitiesTableModel.addRow(new Object[]{a.getName(), formatAccounting(a.getBalance())});
                }
            }
        }

        // Account combos in add-transaction panel
        refreshAccountCombos();

        // Run any UI label updaters (balance sheet totals)
        Component center = getContentPane().getComponent(1); // tabbed pane
        if (center instanceof JTabbedPane) {
            JTabbedPane tp = (JTabbedPane) center;
            for (int i = 0; i < tp.getTabCount(); i++) {
                Component c = tp.getComponentAt(i);
                if (c instanceof JPanel) {
                    Object prop = ((JPanel) c).getClientProperty("updateLabels");
                    if (prop instanceof Runnable) ((Runnable) prop).run();
                }
            }
        }
    }

    // ===================== DatabaseManager (optional) =====================
    // Note: this uses in-memory SQLite. You must add sqlite-jdbc jar to classpath for DB features.
public class DatabaseManagerMySQL {
    private Connection conn;

    public DatabaseManagerMySQL() {
        String url = "jdbc:mysql://localhost:3306/accounting_db"; // database name
        String user = "root"; // imong MySQL user
        String pass = "your_password"; // imong password

        try {
            conn = DriverManager.getConnection(url, user, pass);
            createTables(); // auto-create kung wala pa
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        String createAccounts = """
            CREATE TABLE IF NOT EXISTS accounts (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) UNIQUE NOT NULL,
                type VARCHAR(50) NOT NULL,
                balance DOUBLE DEFAULT 0
            )
        """;

        String createTransactions = """
            CREATE TABLE IF NOT EXISTS transactions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                tx_date DATE NOT NULL,
                description VARCHAR(255),
                debit_account VARCHAR(100),
                credit_account VARCHAR(100),
                amount DOUBLE
            )
        """;

        conn.createStatement().execute(createAccounts);
        conn.createStatement().execute(createTransactions);
    }

    public Connection getConnection() { return conn; }
}


        public void cleanup() {
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    logger.info("Database connection closed - all data cleared");
                }
            } catch (SQLException e) {
                logger.severe("Error closing database: " + e.getMessage());
            }
        }
    }

    // ===================== Main =====================
    public static void main(String[] args) {
        // Use SwingUtilities to start GUI on EDT
        SwingUtilities.invokeLater(() -> new LoginForm());
    }
}
