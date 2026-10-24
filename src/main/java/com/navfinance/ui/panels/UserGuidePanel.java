package com.navfinance.ui.panels;

import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interactive documentation manual and offline User Guide panel for NavFinance.
 * <p>
 * Features a split-pane layout with an indexed, searchable topic directory on the left
 * and an HTML-rendered reader canvas on the right detailing:
 * <ul>
 *   <li>1. Getting Started &amp; Dashboard Overview (KPI cards, bar trends, donut charts).</li>
 *   <li>2. Recording Income Entries (categories, subcategories, counterparties).</li>
 *   <li>3. Recording Expenses &amp; Bills (dynamic subcategories for Bills, Cards, EMIs, Udhar).</li>
 *   <li>4. Udhaar &amp; Personal Loans Ledger (lending, borrowing, net positions, settlements).</li>
 *   <li>5. Catalog Masters (Persons, Bills, EMIs, Credit Cards, Payment Modes).</li>
 *   <li>6. Filtering, Searching &amp; Sorting (presets, multi-column sorting).</li>
 *   <li>7. PDF, Excel &amp; CSV Reports (OpenPDF landscape reports, Apache POI workbooks).</li>
 *   <li>8. Bulk CSV Import, Update &amp; Deletion (template generation, error reporting).</li>
 *   <li>9. Database Storage &amp; Full Backup (H2 embedded storage, directory export).</li>
 *   <li>10. UI Zooming &amp; Theme Customization (FlatLaf Light/Dark, font magnification).</li>
 *   <li>11. Recurring Schedules &amp; Due/Overdue Tracker (fixed vs variable bills, auto-pay).</li>
 * </ul>
 */
public class UserGuidePanel extends JPanel {

    /** Map storing topic chapter titles mapped to HTML-formatted tutorial content. */
    private final Map<String, String> topicContent = new LinkedHashMap<>();

    /** List model backing the searchable topic selection list. */
    private DefaultListModel<String> listModel;

    /** JList component rendering the interactive list of documentation topics. */
    private JList<String> topicList;

    /** HTML-capable editor pane for rendering rich documentation content. */
    private JEditorPane contentPane;

    /** Search text field for filtering available help topics in real-time. */
    private JTextField txtSearch;

    /**
     * Constructs a new {@code UserGuidePanel}, populating documentation content
     * and initializing the split-pane user interface.
     */
    public UserGuidePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        initTopics();
        initComponents();
    }

    /**
     * Populates all documentation chapters and markdown-styled HTML guides into the topic cache.
     */
    private void initTopics() {
        topicContent.put("1. Getting Started & Dashboard", """
            <h2>1. Getting Started & Dashboard Overview</h2>
            <p>The <b>Dashboard</b> provides an executive summary of your cash inflows and outflows across time:</p>
            <ul>
                <li><b style='color:#16a34a;'>Total Income (In):</b> Displayed in vibrant green, representing all earnings in the selected period.</li>
                <li><b style='color:#dc2626;'>Total Expense (Out):</b> Displayed in vibrant red, representing all expenditures, bills, and loans given.</li>
                <li><b style='color:#2563eb;'>Net Savings / Balance:</b> Automatically computed as <code>Income - Expense</code>.</li>
                <li><b style='color:#d97706;'>Udhaar Ledger Position:</b> Net balance of all personal loans and Udhar.</li>
            </ul>
            <h3>Time Periods</h3>
            <p>Use the time selector to view your finances by <b>Weekly</b>, <b>Monthly</b>, <b>Quarterly</b>, or <b>Annually</b>. Click <b>◀ Prev</b> or <b>Next ▶</b> to jump across past and future periods.</p>
            <h3>Visual Charts</h3>
            <p>The anti-aliased <b>Trend Bar Chart</b> compares Income vs Expense over time. The <b>Category Donut Chart</b> shows where your money is going by percentage.</p>
        """);

        topicContent.put("2. Recording Income Entries", """
            <h2>2. Recording Income Entries</h2>
            <p>Click the <b>+ Add Income</b> button in the header or Transactions tab:</p>
            <ul>
                <li><b>Date / Expected Date:</b> Enter manually or click the calendar button to open the picker.</li>
                <li><b>Expected Checkbox:</b> Check if this income is anticipated in the future rather than already in hand.</li>
                <li><b>Amount:</b> Enter the numerical amount.</li>
                <li><b>Category:</b> Choose from <i>Salary, Relative, Return, Loan, Udhaar, Freelance, Other Income</i>.</li>
                <li><b>Sub-Category:</b> Specific employer, person name, or loan name.</li>
                <li><b>Payment Via:</b> Payment mode (Net Banking, UPI, Cash, Paytm, PhonePe, etc.).</li>
                <li><b>Associated Person:</b> Link to an individual from your Persons master for Udhaar returns.</li>
            </ul>
        """);

        topicContent.put("3. Recording Expenses & Bills", """
            <h2>3. Recording Expenses & Bills</h2>
            <p>Click the <b>- Add Expense</b> button:</p>
            <ul>
                <li><b>Date / Planned Date:</b> Type manually or click the calendar button for the picker.</li>
                <li><b>Planned Checkbox:</b> Check if this is an upcoming bill or planned expense not yet paid.</li>
                <li><b>Amount:</b> Enter the expense amount.</li>
                <li><b>Via:</b> Select from <i>Cash, Paytm, PhonePe, UPI, Credit Card, Debit Card, Net Banking</i>, or type a custom payment mode.</li>
                <li><b>Category & Smart Sub-Category:</b>
                    <ul>
                        <li><b>Bill:</b> Sub-category dynamically suggests your bills (<i>Airtel Bill, Electricity (Khardoni), Electricity (Noida)</i>).</li>
                        <li><b>Credit Card:</b> Sub-category dynamically suggests your cards (<i>HDFC Millennia, ICICI Amazon Pay</i>).</li>
                        <li><b>EMI Payment:</b> Sub-category dynamically suggests your EMIs (<i>Home Loan EMI, Car Loan EMI</i>).</li>
                        <li><b>Udhar Given:</b> Sub-category dynamically suggests registered persons.</li>
                    </ul>
                </li>
            </ul>
        """);

        topicContent.put("4. Udhaar & Personal Loans Ledger", """
            <h2>4. Udhaar & Personal Loans Ledger</h2>
            <p>The <b>Udhaar & Loans</b> tab tracks informal lending and borrowing with zero confusion:</p>
            <ul>
                <li><b>Total Given:</b> Money you lent out to others.</li>
                <li><b>Total Returned:</b> Money returned back to you.</li>
                <li><b>Total Taken:</b> Money you borrowed from friends or relatives.</li>
                <li><b>Total Repaid:</b> Money you paid back.</li>
                <li><b>Net Position:</b>
                    <ul>
                        <li><span style='color:#16a34a; font-weight:bold;'>+ ₹X.XX (Owes You):</span> Person has outstanding dues to pay you.</li>
                        <li><span style='color:#dc2626; font-weight:bold;'>- ₹X.XX (You Owe):</span> You need to pay the person back.</li>
                        <li><span style='color:#64748b; font-weight:bold;'>₹0.00 (Settled):</span> All accounts squared.</li>
                    </ul>
                </li>
            </ul>
            <p>Use <b>- Give Udhar</b> and <b>+ Take Udhar / Receive Return</b> for quick entries.</p>
        """);

        topicContent.put("5. Catalog Masters (Bills, EMIs, Persons)", """
            <h2>5. Managing Catalog Masters</h2>
            <p>Under the <b>Masters</b> tab, maintain reusable catalogs for instant auto-suggestions:</p>
            <ul>
                <li><b>Persons Sub-Tab:</b> Add contacts with Name, Contact, Relation (Friend, Relative, Colleague, Family, Landlord), and Notes.</li>
                <li><b>Bills Sub-Tab:</b> Maintain utility bills like <i>Airtel Bill, Electricity (Khardoni), Electricity (Noida), Gas, Water</i> with default monthly amounts.</li>
                <li><b>EMI Master Sub-Tab:</b> Maintain recurring bank installments like <i>Home Loan EMI, Car Loan EMI, Laptop EMI</i> with lender details.</li>
                <li><b>Credit Cards Sub-Tab:</b> Store bank, card name, and last 4 digits.</li>
                <li><b>Payment Modes:</b> Add custom payment channels.</li>
            </ul>
        """);

        topicContent.put("6. Filtering, Searching & Sorting", """
            <h2>6. Filtering, Searching & Sorting Transactions</h2>
            <p>In the <b>Transactions</b> tab:</p>
            <ul>
                <li><b>Search Box:</b> Searches notes, categories, sub-categories, and person names simultaneously.</li>
                <li><b>Type Filter:</b> Filter by All, Income (In), or Expense (Out).</li>
                <li><b>Date Presets:</b> Quick presets for <i>Today, This Week, This Month, This Quarter, This Year, All Time</i>, or custom <b>From / To</b> date pickers.</li>
                <li><b>Sorting:</b> Sort by <i>Date, Amount, Type (In/Out), Category, or Sub-Category</i> in Ascending or Descending order.</li>
                <li><b>Summary Bar:</b> Bottom bar shows real-time count, Total In, Total Out, and Net Balance for the filtered results.</li>
            </ul>
        """);

        topicContent.put("7. PDF, Excel & CSV Reports", """
            <h2>7. Exporting Reports (PDF, Excel, CSV)</h2>
            <p>In the Transactions tab, click <b>Export Report</b>:</p>
            <ul>
                <li><b>Export Filtered to PDF (.pdf):</b> Generates a landscape document featuring title header, KPI summary boxes (Total In, Total Out, Balance), colored badges, and itemized data table via OpenPDF.</li>
                <li><b>Export Filtered to Excel (.xlsx):</b> Generates a Microsoft Excel workbook with colored headers, formatted currency values, and double-line totals via Apache POI.</li>
                <li><b>Export Filtered to CSV (.csv):</b> Standard RFC 4180 comma-separated file for spreadsheets or data analysis.</li>
            </ul>
        """);

        topicContent.put("8. Bulk CSV Import, Update & Delete", """
            <h2>8. Bulk CSV Import, Update & Deletion</h2>
            <p>Import and modify bulk transactions using CSV files:</p>
            <ol>
                <li>Click <b>Download CSV Template</b> to get the pre-formatted structure with instructions and examples.</li>
                <li>Edit your CSV file in Excel or Notepad:
                    <ul>
                        <li><b>Action:</b> <code>CREATE</code>, <code>UPDATE</code>, or <code>DELETE</code>.</li>
                        <li><b>ID:</b> Leave blank for new entries (CREATE); specify the transaction ID for UPDATE or DELETE.</li>
                        <li><b>Type:</b> <code>INCOME</code> or <code>EXPENSE</code>.</li>
                        <li><b>Date:</b> <code>YYYY-MM-DD</code>.</li>
                        <li><b>Amount:</b> Numerical value (e.g. <code>25000.00</code>).</li>
                    </ul>
                </li>
                <li>Click <b>Select CSV File...</b> to preview and validate the file.</li>
                <li>Review the validation status (valid rows vs error rows).</li>
                <li>Click <b>Execute Import</b> to apply changes directly to the database with full audit logging!</li>
            </ol>
        """);

        topicContent.put("9. Database Storage & Full Backup", """
            <h2>9. Database Storage & Full Backup</h2>
            <p>All data is maintained inside an embedded H2 database located at:</p>
            <pre style='background:#f1f5f9; padding:8px; border-radius:6px;'>~user/nav-finance/navfinance_db.mv.db</pre>
            <p>In the <b>Settings & Database</b> tab:</p>
            <ul>
                <li><b>Export All Database as CSV Files:</b> Dumps all 7 tables (<i>transactions, persons, bills, emis, credit_cards, payment_modes, audit_logs</i>) into individual CSV files.</li>
                <li><b>Open Database Folder:</b> Opens the local storage folder in Windows File Explorer.</li>
            </ul>
        """);

        topicContent.put("10. Zoom In / Out & Appearance", """
            <h2>10. UI Zooming & Theme Customization</h2>
            <ul>
                <li><b>Zoom In & Zoom Out:</b> Scalable UI magnification from 80% to 175% with instant re-rendering of all fonts, cards, and tables.</li>
                <li><b>Reset Zoom:</b> Click the percentage badge (e.g. <code>100%</code>) to reset default scale.</li>
                <li><b>Keyboard Shortcuts:</b>
                    <ul>
                        <li><code>Ctrl</code> + <code>Plus (+)</code>: Zoom In</li>
                        <li><code>Ctrl</code> + <code>Minus (-)</code>: Zoom Out</li>
                        <li><code>Ctrl</code> + <code>0</code>: Reset Zoom to 100%</li>
                    </ul>
                </li>
                <li><b>Light & Dark Mode:</b> Switch themes anytime under <b>Settings & Database</b>.</li>
                <li><b>Custom Currency Symbol:</b> Change from <code>₹</code> to <code>$</code>, <code>€</code>, etc.</li>
            </ul>
        """);

        topicContent.put("11. Recurring Schedules & Due/Overdue Tracker", """
            <h2>11. Recurring Schedules & Due/Overdue Payment Management</h2>
            <p>Under the <b>Due & Recurring</b> tab, manage repeating obligations with zero forgotten payments:</p>
            <h3>Registering a Recurring Schedule</h3>
            <ol>
                <li>Click <b>+ Register Recurring Schedule</b>.</li>
                <li><b>Specific Date of Month:</b> Choose day <code>1 to 31</code> (e.g. 5th of every month).</li>
                <li><b>Date Range:</b> Select Start Month and End Month (e.g. <i>Jan 2026 to Jan 2027</i> for a 12-month loan EMI, or check <i>Ongoing / No End Date</i>).</li>
                <li><b>Amount Type (Fixed vs Variable):</b>
                    <ul>
                        <li><b>Fixed Amount:</b> Recurring payments with constant values (e.g., <code>₹20,000</code> monthly EMI or house rent).</li>
                        <li><b>Variable Amount:</b> Utility bills or credit cards whose amounts fluctuate each month (e.g. <i>Electricity Bill</i> or <i>Credit Card</i>). The app automatically prompts you to enter the actual bill amount when settling!</li>
                    </ul>
                </li>
                <li><b>Auto-Payment vs Manual Payment:</b>
                    <ul>
                        <li><b style='color:#0e7490;'>[Auto-Payment]:</b> Mandates like NACH, bank auto-debits, or standing instructions that execute automatically.</li>
                        <li><b style='color:#b45309;'>[Manual Payment]:</b> Payments that require you to manually open an app, write a cheque, or swipe a card.</li>
                    </ul>
                </li>
            </ol>
            <h3>Tracking Due & Overdue Payments</h3>
            <ul>
                <li><b style='color:#dc2626;'>[Overdue Payments]:</b> Highlighted in bold red whenever the due date has elapsed without being settled. Shows elapsed days (e.g., <code>+5d</code>).</li>
                <li><b style='color:#d97706;'>[Upcoming Due]:</b> Payments due today or within the coming days.</li>
                <li><b style='color:#16a34a;'>[Settled]:</b> Successfully paid entries with settlement date and transaction reference.</li>
            </ul>
            <h3>Settling & Reverting Payments</h3>
            <ul>
                <li><b>Settle / Pay Selected:</b> Select any row and click Settle (or double-click). For variable bills, enter the exact amount from your bill statement. Check <i>Record as transaction</i> to automatically insert the expense into your ledger!</li>
                <li><b>Mark Unsettled:</b> If marked settled by mistake, click Mark Unsettled to revert the status back to pending, with option to delete the linked transaction.</li>
            </ul>
        """);
    }

    /**
     * Initializes visual layout components, headers, split panes, list models, and selection listeners.
     */
    private void initComponents() {
        // TOP: Header & Search Bar
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 2));
        titleBlock.setOpaque(false);
        JLabel lblTitle = new JLabel("Interactive User Guide & Documentation");
        lblTitle.setFont(UiUtils.FONT_TITLE);
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("Comprehensive manual explaining every workflow, formula, shortcut, and feature.");
        lblSub.setFont(UiUtils.FONT_SUBTITLE);
        lblSub.setForeground(UiUtils.COLOR_TEXT_MUTED);

        titleBlock.add(lblTitle, BorderLayout.NORTH);
        titleBlock.add(lblSub, BorderLayout.SOUTH);
        topBar.add(titleBlock, BorderLayout.WEST);

        // Filter / Search topics control
        JPanel searchBox = new JPanel(new BorderLayout(6, 0));
        searchBox.setOpaque(false);
        searchBox.add(new JLabel("Search Topic:", NavIcons.filter(12, UiUtils.COLOR_TEXT_MUTED), JLabel.LEFT), BorderLayout.WEST);
        txtSearch = new JTextField(14);
        txtSearch.addActionListener(e -> filterTopics());
        searchBox.add(txtSearch, BorderLayout.CENTER);
        topBar.add(searchBox, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // CENTER: Split pane (Topic List on Left, HTML Reader on Right)
        listModel = new DefaultListModel<>();
        for (String topic : topicContent.keySet()) {
            listModel.addElement(topic);
        }

        topicList = new JList<>(listModel);
        topicList.setFont(UiUtils.FONT_BOLD);
        topicList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        topicList.setSelectedIndex(0);
        topicList.setFixedCellHeight(34);
        topicList.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        topicList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setIcon(NavIcons.userGuide(14, isSelected ? Color.WHITE : UiUtils.COLOR_PRIMARY));
                l.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                return l;
            }
        });

        JScrollPane spList = new JScrollPane(topicList);
        spList.setBorder(UiUtils.createCardBorder());
        spList.setPreferredSize(new Dimension(280, 500));

        contentPane = new JEditorPane();
        contentPane.setContentType("text/html");
        contentPane.setEditable(false);
        contentPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        contentPane.setFont(UiUtils.FONT_REGULAR);
        contentPane.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JScrollPane spContent = new JScrollPane(contentPane);
        spContent.setBorder(UiUtils.createCardBorder());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spList, spContent);
        splitPane.setDividerLocation(290);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        add(splitPane, BorderLayout.CENTER);

        // Display initial selected topic
        displayTopic(listModel.get(0));

        // Topic selection change listener
        topicList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = topicList.getSelectedValue();
                if (selected != null) {
                    displayTopic(selected);
                }
            }
        });
    }

    /**
     * Filters the visible topics list in response to search input query matching topic title or HTML body.
     */
    private void filterTopics() {
        String query = txtSearch.getText().trim().toLowerCase();
        listModel.clear();
        for (String topic : topicContent.keySet()) {
            if (query.isEmpty() || topic.toLowerCase().contains(query) || topicContent.get(topic).toLowerCase().contains(query)) {
                listModel.addElement(topic);
            }
        }
        if (!listModel.isEmpty()) {
            topicList.setSelectedIndex(0);
        }
    }

    /**
     * Renders the specified documentation topic in the HTML editor pane.
     *
     * @param topicName the title of the topic chapter to display
     */
    private void displayTopic(String topicName) {
        String body = topicContent.get(topicName);
        if (body == null) body = "<p>No topic content available.</p>";

        String html = "<html><body style='font-family:Segoe UI, sans-serif; font-size:12pt; color:#1e293b; padding:10px; line-height:1.5;'>"
                + body + "</body></html>";
        contentPane.setText(html);
        contentPane.setCaretPosition(0);
    }
}
