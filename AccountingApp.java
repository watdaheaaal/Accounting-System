import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.text.NumberFormat;
import javax.tools.Tool;


// Main class sa accounting app
public class AccountingApp extends JFrame {

    // --- Private Fields ---
    private List<Account> accounts;
    private List<Transaction> transactions;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private NumberFormat currencyFormat; 

    // UI Components
    private DefaultTableModel transactionsTableModel;
    private DefaultTableModel accountsTableModel;
    private DefaultTableModel journalTableModel;
    private DefaultTableModel ledgerTableModel;
    private DefaultTableModel assetsTableModel;
    private DefaultTableModel liabilitiesTableModel;
    private JComboBox<String> ledgerAccountCombo;
    private JComboBox<String> debitComboGlobal;
    private JComboBox<String> creditComboGlobal;
    private JTabbedPane mainTabbedPane; // Key component for navigation

    // Design Colors
    private final Color PRIMARY_BLUE = new Color(20, 50, 80); // Dark Blue
    private final Color SECONDARY_MINT = new Color(0, 191, 165); // Mint Green/Teal
    private final Color BACKGROUND_LIGHT = new Color(245, 248, 250);

    public AccountingApp() {
        try {
            Image icon = Toolkit.getDefaultToolkit().getImage(AccountingApp.class.getResource("/img/accountingIcon.png"));
            if (icon != null) {
                setIconImage(icon);
            } else {
                System.err.println("Icon file not found. Check path: /img/accountingIcon.png");
            }
        } catch (Exception e) {
            System.err.println("Error setting icon: " + e.getMessage());
        }


        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/img/accountingIcon.png")));
        // Set up formatting and data
        setLayout(new BorderLayout());
        sdf.setLenient(false);
        accounts = new ArrayList<>();
        transactions = new ArrayList<>();
        addPredefinedAccounts();
        
        currencyFormat = NumberFormat.getNumberInstance(Locale.US);
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);
        
        // Apply look and feel (Modernize the UI)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignore if L&F can't be set
        }

        // Header/navbar
        add(createHeader(), BorderLayout.NORTH);

        // Tabbed Pane for main content
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setBackground(BACKGROUND_LIGHT);
        mainTabbedPane.setForeground(PRIMARY_BLUE);
        mainTabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        mainTabbedPane.addTab("Add New Transaction", createAddTransactionPanel());
        mainTabbedPane.addTab("Transactions", createTransactionsPanel());
        mainTabbedPane.addTab("Accounts", createAccountsPanel()); 
        mainTabbedPane.addTab("General Journal", createGeneralJournalPanel());
        mainTabbedPane.addTab("General Ledger", createGeneralLedgerPanel());
        mainTabbedPane.addTab("Balance Sheet", createBalanceSheetPanel());

        add(mainTabbedPane, BorderLayout.CENTER);
        
        setTitle("Accounting System");
        setSize(1100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setVisible(true);

        refreshAllViews();
    }

    // --- Currency Formatting ---
    private String formatCurrency(double value) {
        return currencyFormat.format(value);
    }

    private String formatAccounting(double value) {
        if (value < 0) {
            return "(" + formatCurrency(Math.abs(value)) + ")";
        }
        return currencyFormat.format(value);
    }

    // --- Data Classes (Serializable for Save/Open) ---
    // Ensure all internal types are also Serializable
    private static class Account implements Serializable {
        private static final long serialVersionUID = 1L; 
        private String accountNumber; 
        private String name;
        private String type;
        private double balance;

        public Account(String accountNumber, String name, String type, double initialBalance) {
            this.accountNumber = accountNumber;
            this.name = name;
            this.type = type;
            this.balance = initialBalance;
        }

        public String getAccountNumber() { return accountNumber; }
        public String getName() { return name; }
        public String getType() { return type; }
        public double getBalance() { return balance; }

        public void applyDebit(double amount) {
            // Asset, Expense, DRAWING increase with Debit
            if (type.equals("Asset") || type.equals("Expense") || name.equals("Owner's Drawing")) {
                balance += amount;
            } else {
                // Liability, Revenue, CAPITAL decrease with Debit
                balance -= amount;
            }
        }

        public void applyCredit(double amount) {
            // Liability, Revenue, CAPITAL increase with Credit
            if (type.equals("Liability") || type.equals("Revenue") || name.equals("Owner's Capital")) {
                balance += amount;
            } else {
                // Asset, Expense, DRAWING decrease with Credit
                balance -= amount;
            }
        }
    }
    
    private static class Transaction implements Serializable {
        private static final long serialVersionUID = 1L;
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

    // --- Setup and Helper Methods ---

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_BLUE); 
        header.setBorder(new EmptyBorder(10, 15, 10, 15));

        // Left Panel (Title and Quote)
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        
        JLabel companyName = new JLabel("Accounting System");
        companyName.setForeground(Color.WHITE);
        companyName.setFont(new Font("Segoe UI", Font.BOLD, 28));
        
        JLabel systemQuote = new JLabel("Transactions • Accounts • General Journal • General Ledger • Balance Sheet");
        systemQuote.setForeground(SECONDARY_MINT);
        systemQuote.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        titlePanel.add(companyName);
        titlePanel.add(systemQuote);

        // Right Panel (Buttons)
        JPanel menuPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        menuPanel.setOpaque(false);
        
        // --- NEW BUTTONS ---
        JButton homeBtn = createStyledButton("Home");
        JButton aboutBtn = createStyledButton("About");
        // --- END NEW BUTTONS ---
        
        JButton openBtn = createStyledButton("Open File");
        JButton saveBtn = createStyledButton("Save");
        
        // --- ACTION LISTENERS FOR NEW BUTTONS ---
        homeBtn.addActionListener(e -> mainTabbedPane.setSelectedIndex(0)); // Index 0 is "Add New Transaction"
        aboutBtn.addActionListener(e -> createAboutDialog().setVisible(true));
        // --- END ACTION LISTENERS ---

        openBtn.addActionListener(e -> openFile());
        saveBtn.addActionListener(e -> saveFile());

        // Add new buttons to the menu panel
        menuPanel.add(homeBtn);
        menuPanel.add(aboutBtn);
        menuPanel.add(openBtn);
        menuPanel.add(saveBtn);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(menuPanel, BorderLayout.EAST);

        return header;
    }
    
    /**
     * Creates and returns the About dialog window.
     */
    private JDialog createAboutDialog() {
        JDialog dialog = new JDialog(this, "About this Accounting System", true);
        dialog.setLayout(new BorderLayout(15, 15));
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setBackground(BACKGROUND_LIGHT);
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.setBackground(Color.WHITE);

        JLabel title = new JLabel("Accounting System Overview", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(PRIMARY_BLUE.darker());

        JTextArea info = new JTextArea();
        info.setText(
            "This application simulates a simple proprietorship accounting system based on the double-entry method.\n\n" +
            "Key functionalities:\n" +
            "1. **Add New Transaction (Home):** Records debits and credits that must always balance.\n" +
            "2. **Chart of Accounts:** Displays all existing accounts and their running balances.\n" +
            "3. **General Journal:** Shows the chronological list of all posted transactions.\n" +
            "4. **General Ledger:** Allows viewing the detailed activity (T-account) and running balance for any selected account.\n" +
            "5. **Balance Sheet:** Automatically generates the fundamental financial statement (Assets = Liabilities + Equity).\n\n" +
            "Data can be saved and loaded using the 'Save' and 'Open File' buttons."+
            "This system is designed for educational purposes to illustrate basic accounting principles."+
            "It is not intended for actual financial reporting or compliance with accounting standards."+
            "Always consult a professional accountant for real-world accounting needs."+
            "\n\nDeveloped by Group 1(MCNF) for CC-ACCTNG21"+
            "\nMembers: \n- Turceno, James Gabriel\n- Abad, Alexandra\n- Taburnal, Phoebe Fatima\n- Arong, Dara San\n- Cañoneo, Mark\n- Ranes, Andrea"+

            "\n\n© 2024 Accounting System by MCNF. All rights reserved."
        );
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setWrapStyleWord(true);
        info.setLineWrap(true);
        info.setEditable(false);
        info.setBackground(Color.WHITE);
        info.setCaretPosition(0); // Scroll to top

        content.add(title, BorderLayout.NORTH);
        content.add(new JScrollPane(info), BorderLayout.CENTER);
        
        JButton closeBtn = createStyledButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.add(closeBtn);
        southPanel.setBackground(Color.WHITE);

        dialog.add(content, BorderLayout.CENTER);
        dialog.add(southPanel, BorderLayout.SOUTH);
        
        return dialog;
    }
    
    // Utility to create styled buttons
    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(SECONDARY_MINT);
        btn.setForeground(PRIMARY_BLUE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void addPredefinedAccounts() {
        String[][] predefined = {
            {"1001","Cash","Asset"},
            {"1010","Accounts Receivable","Asset"},
            {"1020","Prepaid Expenses","Asset"},
            {"1030","Inventory","Asset"},
            {"1040","Fixed Assets","Asset"},
            {"1050","Accumulated Depreciation","Asset"},
            {"1060","Other Assets","Asset"},
            {"2001","Accounts Payable","Liability"},
            {"2010","Accrued Liabilities","Liability"},
            {"2020","Taxes Payable","Liability"},
            {"2030","Payroll Payable","Liability"},
            {"2040","Notes Payable","Liability"},
            {"3001","Owner's Capital","Equity"}, 
            {"3002","Owner's Drawing","Equity"}, 
            {"4001","Revenue","Revenue"},
            {"4010","Sales returns and allowances","Revenue"},
            {"5001","Cost of Goods Sold","Expense"},
            {"5010","Advertising Expense","Expense"},
            {"5020","Bank Fees","Expense"},
            {"5030","Depreciation Expense","Expense"},
            {"5040","Payroll Tax Expense","Expense"},
            {"5050","Rent Expense","Expense"},
            {"5060","Supplies Expense","Expense"},
            {"5070","Utilities Expense","Expense"},
            {"5080","Wages Expense","Expense"},
            {"6001","Other Expenses","Expense"}
        };

        for (String[] acc : predefined) {
            accounts.add(new Account(acc[0], acc[1], acc[2], 0.0));
        }
    }
    
    private String extractAccountName(String comboItem) {
        return comboItem; 
    }

    private Account getAccountByName(String name) {
        for (Account a : accounts) {
            if (a.getName().equals(name)) return a;
        }
        return null;
    }

    // --- Data Persistence Methods (New) ---

    // Data structure to hold data for serialization
    private static class AccountingData implements Serializable {
        private static final long serialVersionUID = 2L;
        List<Account> accounts;
        List<Transaction> transactions;

        public AccountingData(List<Account> accounts, List<Transaction> transactions) {
            this.accounts = accounts;
            this.transactions = transactions;
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Accounting File");
        
        int userSelection = fileChooser.showSaveDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".dat")) {
                fileToSave = new File(fileToSave.toString() + ".dat");
            }
            
            try (FileOutputStream fileOut = new FileOutputStream(fileToSave);
                 ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {
                
                AccountingData data = new AccountingData(this.accounts, this.transactions);
                objectOut.writeObject(data);
                JOptionPane.showMessageDialog(this, "File saved successfully to:\n" + fileToSave.getAbsolutePath(), "Save Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Accounting File");
        
        int userSelection = fileChooser.showOpenDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            
            try (FileInputStream fileIn = new FileInputStream(fileToOpen);
                 ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {
                
                AccountingData data = (AccountingData) objectIn.readObject();
                
                this.accounts = data.accounts;
                this.transactions = data.transactions;
                
                // Ensure accounts are re-initialized if file was empty or corrupted (safety check)
                if (this.accounts.isEmpty()) addPredefinedAccounts();
                
                refreshAllViews();
                
                JOptionPane.showMessageDialog(this, "File loaded successfully from:\n" + fileToOpen.getAbsolutePath(), "Open Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "File not found.", "Open Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException | ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "Error opening file. Check the file format. Details: " + ex.getMessage(), "Open Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // Helper method for creating styled labels
    private JLabel createFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(PRIMARY_BLUE);
        return lbl;
    }


    // --- Panel Creation Methods (Redesigned UI) ---

    private JPanel createAddTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BACKGROUND_LIGHT);
        panel.setBorder(new EmptyBorder(30, 100, 30, 100)); // Added padding
        
        JLabel title = new JLabel("Post New Journal Entry");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(PRIMARY_BLUE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createLineBorder(PRIMARY_BLUE.darker(), 1));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField dateField = new JTextField(sdf.format(new Date()));
        JTextField descField = new JTextField();
        debitComboGlobal = new JComboBox<>();
        creditComboGlobal = new JComboBox<>();
        JTextField amountField = new JTextField();

        // Style Form Components
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        amountField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        debitComboGlobal.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        creditComboGlobal.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        refreshAccountCombos();
        

        gbc.gridx = 0; gbc.gridy = 0; form.add(createFormLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1; form.add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; form.add(createFormLabel("Description:"), gbc);
        gbc.gridx = 1; form.add(descField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(createFormLabel("Debit Account:"), gbc);
        gbc.gridx = 1; form.add(debitComboGlobal, gbc);

        gbc.gridx = 0; gbc.gridy = 3; form.add(createFormLabel("Credit Account:"), gbc);
        gbc.gridx = 1; form.add(creditComboGlobal, gbc);

        gbc.gridx = 0; gbc.gridy = 4; form.add(createFormLabel("Amount:"), gbc);
        gbc.gridx = 1; form.add(amountField, gbc);
        
        panel.add(form, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        JButton postBtn = createStyledButton("Post Transaction (F5)");
        JButton clearBtn = createStyledButton("Clear Fields (Esc)");
        
        postBtn.setBackground(SECONDARY_MINT.darker());
        postBtn.setForeground(Color.WHITE);
        clearBtn.setBackground(Color.LIGHT_GRAY);
        clearBtn.setForeground(PRIMARY_BLUE);

        buttonPanel.add(postBtn);
        buttonPanel.add(clearBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Action listeners remain the same
        postBtn.addActionListener(e -> postTransaction(dateField, descField, amountField));
        clearBtn.addActionListener(e -> clearTransactionFields(dateField, descField, amountField));
        
        // Add Key Bindings (F5 to post, Esc to clear)
        InputMap inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = panel.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "post");
        actionMap.put("post", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                postTransaction(dateField, descField, amountField);
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
        actionMap.put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearTransactionFields(dateField, descField, amountField);
            }
        });


        return panel;
    }
    
    // Extracted transaction logic
    private void postTransaction(JTextField dateField, JTextField descField, JTextField amountField) {
        String dateStr = dateField.getText().trim();
        String desc = descField.getText().trim();
        
        String debitAccComboItem = (String) debitComboGlobal.getSelectedItem();
        String creditAccComboItem = (String) creditComboGlobal.getSelectedItem();
        String debitAccName = extractAccountName(debitAccComboItem);
        String creditAccName = extractAccountName(creditAccComboItem);
        
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

        debitAcc.applyDebit(amount);
        creditAcc.applyCredit(amount);

        Transaction tx = new Transaction(date, desc, debitAccName, creditAccName, amount);
        transactions.add(tx);

        transactions.sort(Comparator.comparing(Transaction::getDate));

        refreshAllViews();

        JOptionPane.showMessageDialog(this, "Transaction posted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        clearTransactionFields(dateField, descField, amountField);
    }
    
    private void clearTransactionFields(JTextField dateField, JTextField descField, JTextField amountField) {
        dateField.setText(sdf.format(new Date()));
        descField.setText("");
        amountField.setText("");
        if (debitComboGlobal.getItemCount() > 0) debitComboGlobal.setSelectedIndex(0);
        if (creditComboGlobal.getItemCount() > 0) creditComboGlobal.setSelectedIndex(0);
    }


    private JPanel createTransactionsPanel() {
        JPanel panel = createStyledPanel();
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(10, 10, 10, 10));
        JTextField searchField = new JTextField(30);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JButton searchBtn = createStyledButton("Search");
        
        JLabel searchLabel = new JLabel("Search (date/description/account):");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchLabel.setForeground(PRIMARY_BLUE);
        
        top.add(searchLabel);
        top.add(searchField);
        top.add(searchBtn);

        String[] columns = {"Date", "Description", "Debit Account", "Credit Account", "Amount"};
        transactionsTableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = createStyledTable(transactionsTableModel);
        
        searchBtn.addActionListener(e -> {
            String query = searchField.getText().trim().toLowerCase();
            filterTransactions(query);
        });

        searchField.addActionListener(e -> filterTransactions(searchField.getText().trim().toLowerCase()));

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createAccountsPanel() {
        JPanel panel = createStyledPanel();
        
        String[] columns = {"Account no.", "Account Name", "Type", "Current Balance"}; 
        accountsTableModel = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = createStyledTable(accountsTableModel);
        
        refreshAccountsTable();

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createGeneralJournalPanel() {
        JPanel panel = createStyledPanel();
        String[] cols = {"Date", "Description", "Account", "Debit", "Credit"};
        journalTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = createStyledTable(journalTableModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createGeneralLedgerPanel() {
        JPanel panel = createStyledPanel();
        ledgerAccountCombo = new JComboBox<>();
        refreshLedgerAccountCombo();
        
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JLabel selectLabel = new JLabel("Select Account:");
        selectLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        selectLabel.setForeground(PRIMARY_BLUE);
        
        ledgerAccountCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        top.add(selectLabel);
        top.add(ledgerAccountCombo);

        String[] cols = {"Date", "Description", "Debit Account", "Credit Account", "Amount", "Running Balance"};
        ledgerTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable table = createStyledTable(ledgerTableModel);

        ledgerAccountCombo.addActionListener(e -> {
            String acc = (String) ledgerAccountCombo.getSelectedItem();
            if (acc != null) updateGeneralLedgerTable(acc);
        });

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBalanceSheetPanel() {
        JPanel panel = createStyledPanel();
        panel.setLayout(new GridLayout(1,2, 10, 10)); // Added spacing between panels
        
        // ASSETS PANEL
        JPanel assetsPanel = createStyledSubPanel("Assets");
        String[] assetCols = {"Account Name", "Amount"};
        assetsTableModel = new DefaultTableModel(assetCols, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable assetsTable = createStyledTable(assetsTableModel);
        assetsPanel.add(new JScrollPane(assetsTable), BorderLayout.CENTER);
        JLabel totalAssetsLabel = createTotalLabel();
        assetsPanel.add(totalAssetsLabel, BorderLayout.SOUTH);

        // LIABILITIES AND EQUITY PANEL
        JPanel liabilitiesPanel = createStyledSubPanel("Liabilities and Equity");
        String[] liabCols = {"Account Name", "Amount"};
        liabilitiesTableModel = new DefaultTableModel(liabCols, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        JTable liabTable = createStyledTable(liabilitiesTableModel);
        liabilitiesPanel.add(new JScrollPane(liabTable), BorderLayout.CENTER);
        JLabel totalLiabLabel = createTotalLabel();
        liabilitiesPanel.add(totalLiabLabel, BorderLayout.SOUTH);

        panel.add(assetsPanel);
        panel.add(liabilitiesPanel);

        Runnable updateLabels = () -> {
            totalAssetsLabel.setText("Total Assets: " + formatAccounting(calculateTotalAssets()));
            totalLiabLabel.setText("Total Liabilities & Equity: " + formatAccounting(calculateTotalLiabilitiesAndEquity()));
        };

        updateLabels.run();
        panel.putClientProperty("updateLabels", updateLabels);
        return panel;
    }
    
    // Styled UI helpers
    private JPanel createStyledPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_LIGHT);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        return panel;
    }
    
    private JPanel createStyledSubPanel(String titleText) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY_BLUE, 1),
            titleText,
            javax.swing.border.TitledBorder.LEADING,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 16),
            SECONDARY_MINT.darker()
        ));
        return panel;
    }
    
    /**
     * Creates a styled JTable, setting the header to dark blue 
     * and minimizing selection/hover effects.
     */
    private JTable createStyledTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(25);
        table.setFillsViewportHeight(true);
        
        // --- Header Styling Fixes (Row 1) ---
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        final Color HEADER_BG = PRIMARY_BLUE.darker();
        final Color HEADER_FG = Color.WHITE;

        // 1. Set the fixed background and foreground colors
        header.setBackground(HEADER_BG);
        header.setForeground(HEADER_FG);
        
        // 2. Override the default header renderer to guarantee consistent colors 
        //    and ignore selection/focus states (which cause the unwanted 'hover' effect).
        header.setDefaultRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value.toString(), SwingConstants.CENTER);
                label.setOpaque(true);
                label.setBackground(HEADER_BG);
                label.setForeground(HEADER_FG);
                label.setFont(header.getFont());
                label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, Color.LIGHT_GRAY));
                return label;
            }
        });

        // --- Table Row Styling Fixes (Removing Hover on Data Rows) ---
        // Minimize/remove visual hover/selection effect for data rows
        Color defaultTableBackground = table.getBackground();
        Color defaultTableForeground = table.getForeground();
        table.setSelectionBackground(defaultTableBackground);
        table.setSelectionForeground(defaultTableForeground);
        
        return table;
    }
    
    private JLabel createTotalLabel() {
        JLabel label = new JLabel("", SwingConstants.RIGHT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(PRIMARY_BLUE);
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        label.setBackground(new Color(230, 230, 230)); // Light gray background for total
        label.setOpaque(true);
        return label;
    }
    // End Styled UI helpers

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
                        formatCurrency(tx.getAmount())
                });
            }
        }
    }

    private void refreshAccountsTable() {
        if (accountsTableModel == null) return;
        accountsTableModel.setRowCount(0);
        for (Account a : accounts) {
            accountsTableModel.addRow(new Object[]{a.getAccountNumber(), a.getName(), a.getType(), formatAccounting(a.getBalance())});
        }
    }

    private void updateGeneralLedgerTable(String accountName) {
        ledgerTableModel.setRowCount(0);
        Account acc = getAccountByName(accountName);
        if (acc == null) return;

        double running = 0.0;
        
        boolean normalBalanceIsDebit = acc.getType().equals("Asset") || acc.getType().equals("Expense") || acc.getName().equals("Owner's Drawing");

        for (Transaction tx : transactions) {
            double amount = tx.getAmount();
            String dateStr = sdf.format(tx.getDate());
            
            // Debit Effect
            if (tx.getDebitAccount().equals(accountName)) {
                if (normalBalanceIsDebit) {
                    running += amount; 
                } else {
                    running -= amount; 
                }
                ledgerTableModel.addRow(new Object[]{dateStr, tx.getDescription(), tx.getDebitAccount(), tx.getCreditAccount(),
                        formatCurrency(amount), 
                        formatAccounting(running)});
            }
            
            // Credit Effect
            if (tx.getCreditAccount().equals(accountName)) {
                if (normalBalanceIsDebit) {
                    running -= amount; 
                } else {
                    running += amount; 
                }
                ledgerTableModel.addRow(new Object[]{dateStr, tx.getDescription(), tx.getDebitAccount(), tx.getCreditAccount(),
                        formatCurrency(amount), 
                        formatAccounting(running)});
            }
        }
    }

    private List<String> getAllAccountNames() {
        List<String> out = new ArrayList<>();
        for (Account a : accounts) out.add(a.getName());
        return out;
    }

    private void refreshAccountCombos() {
        List<String> allAccountNames = getAllAccountNames();

        if (debitComboGlobal != null) {
            debitComboGlobal.removeAllItems();
            for (String s : allAccountNames) debitComboGlobal.addItem(s);
        }
        if (creditComboGlobal != null) {
            creditComboGlobal.removeAllItems();
            for (String s : allAccountNames) creditComboGlobal.addItem(s);
        }
    }

    private void refreshLedgerAccountCombo() {
        if (ledgerAccountCombo == null) return;
        ledgerAccountCombo.removeAllItems();
        for (String s : getAllAccountNames()) ledgerAccountCombo.addItem(s);
    }

    // --- Refresh All Views ---

    private void refreshAllViews() {
        if (transactionsTableModel != null) {
            filterTransactions("");
        }
        
        refreshAccountsTable(); 

        if (journalTableModel != null) {
            journalTableModel.setRowCount(0);
            for (Transaction tx : transactions) {
                String dateStr = sdf.format(tx.getDate());
                // debit row
                journalTableModel.addRow(new Object[]{dateStr, tx.getDescription(), tx.getDebitAccount(),
                        formatCurrency(tx.getAmount()), ""});
                // credit row
                journalTableModel.addRow(new Object[]{dateStr, tx.getDescription(), tx.getCreditAccount(),
                        "", formatCurrency(tx.getAmount())});
            }
        }

        refreshLedgerAccountCombo();
        if (ledgerAccountCombo != null && ledgerAccountCombo.getItemCount() > 0) {
            String sel = (String) ledgerAccountCombo.getSelectedItem();
            if (sel == null) sel = ledgerAccountCombo.getItemAt(0);
            ledgerAccountCombo.setSelectedItem(sel);
            updateGeneralLedgerTable(sel);
        }

        if (assetsTableModel != null && liabilitiesTableModel != null) {
            assetsTableModel.setRowCount(0);
            liabilitiesTableModel.setRowCount(0);
            
            for (Account a : accounts) {
                if (a.getType().equals("Asset")) {
                    assetsTableModel.addRow(new Object[]{a.getName(), formatAccounting(a.getBalance())});
                } else if (a.getType().equals("Liability")) {
                    liabilitiesTableModel.addRow(new Object[]{a.getName(), formatAccounting(a.getBalance())});
                }
            }
            
            double totalEquity = calculateProprietorshipEquity();
            liabilitiesTableModel.addRow(new Object[]{"", ""}); // Separator
            liabilitiesTableModel.addRow(new Object[]{"Owner's Equity (Ending Balance)", formatAccounting(totalEquity)});
        }

        refreshAccountCombos();

        // run any UI label updaters (balance sheet totals)
        if (mainTabbedPane != null) {
            for (int i = 0; i < mainTabbedPane.getTabCount(); i++) {
                Component c = mainTabbedPane.getComponentAt(i);
                if (c instanceof JPanel) {
                    Object prop = ((JPanel) c).getClientProperty("updateLabels");
                    if (prop instanceof Runnable) ((Runnable) prop).run();
                }
            }
        }
    }
    // --- End Refresh All Views ---
    
    // --- Financial Calculations ---
    
    private double calculateProprietorshipEquity() {
        double capital = 0;
        double drawing = 0;
        double revenue = 0;
        double expense = 0;

        for (Account a : accounts) {
            if (a.getName().equals("Owner's Capital")) {
                capital = a.getBalance();
            } else if (a.getName().equals("Owner's Drawing")) {
                drawing = a.getBalance();
            } else if (a.getType().equals("Revenue")) {
                revenue += a.getBalance();
            } else if (a.getType().equals("Expense")) {
                expense += a.getBalance();
            }
        }

        double netIncome = revenue - expense;
        return capital + netIncome - drawing; 
    }

    private double calculateTotalAssets() {
        double sum = 0;
        for (Account a : accounts) if (a.getType().equals("Asset")) sum += a.getBalance();
        return sum;
    }

    private double calculateTotalLiabilitiesAndEquity() {
        double totalLiabilities = 0;
        
        for (Account a : accounts) {
            if (a.getType().equals("Liability")) {
                totalLiabilities += a.getBalance();
            }
        }
        
        double totalEquity = calculateProprietorshipEquity();
        
        return totalLiabilities + totalEquity; 
    }

    // --- Main Method ---
    public static void main(String[] args) {
        // Jframe_Icon fic = new Jframe_Icon();
        // fic.setVisible(true);

        SwingUtilities.invokeLater(() -> new AccountingApp());
    }
}
