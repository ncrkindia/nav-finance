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
 *   <li>3. Recording Expenses &amp; Bills (dynamic subcategories for Bills, Cards, EMIs, Loans).</li>
 *   <li>4. Loans &amp; Personal Dues Ledger (lending, borrowing, net positions, settlements).</li>
 *   <li>5. Catalog Masters (Persons, Bills, EMIs, Credit Cards, Payment Modes).</li>
 *   <li>6. Filtering, Searching &amp; Sorting (presets, multi-column sorting).</li>
 *   <li>7. PDF, Excel &amp; CSV Reports (OpenPDF landscape reports, Apache POI workbooks).</li>
 *   <li>8. Bulk CSV Import, Update &amp; Deletion (template generation, error reporting).</li>
 *   <li>9. Database Storage &amp; Full Backup (H2 embedded storage, directory export).</li>
 *   <li>10. UI Zooming &amp; Theme Customization (FlatLaf Light/Dark, font magnification).</li>
 *   <li>11. Recurring Schedules &amp; Due/Overdue Tracker (fixed vs variable bills, auto-pay).</li>
 *   <li>12. Entity &amp; Category Tracker (Days, Week, Months, Quarter, Years trends &amp; graphs).</li>
 *   <li>13. Multi-Database Engines (MySQL, PostgreSQL, MariaDB, SQLite, embedded H2).</li>
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
                <li><b style='color:#d97706;'>Loans & Dues Ledger:</b> Net balance of all personal loans and money lent or borrowed.</li>
            </ul>
            <h3>Time Periods</h3>
            <p>Use the time selector to view your finances by <b>Weekly</b>, <b>Monthly</b>, <b>Quarterly</b>, or <b>Annually</b>. Click <b>◀ Prev</b> or <b>Next ▶</b> to jump across past and future periods.</p>
            <h3>Interactive Visual Charts</h3>
            <p>Nav Finance features pro-grade interactive visual charts with smooth anti-aliased rendering:</p>
            <ul>
                <li><b>Cash Flow Dual-Bar Trend Chart:</b>
                    <ul>
                        <li><b>Hover Focus & Badges:</b> Hovering any period highlights the slot with a translucent spotlight card and displays exact numerical currency badges right above the bars.</li>
                        <li><b>Floating Glassmorphic Tooltip:</b> Shows detailed income, expense, net balance/deficit, and % savings rate near your cursor.</li>
                        <li><b>Click Drilldown:</b> Click any bar or slot to instantly open the Transactions ledger filtered to that exact time period.</li>
                        <li><b>Series Isolation:</b> Click <i>Income (In)</i> or <i>Expense (Out)</i> pills in the legend to isolate that series.</li>
                    </ul>
                </li>
                <li><b>Category Donut Chart:</b>
                    <ul>
                        <li><b>Dataset Toggle:</b> Switch between <b>[Expenses 🔻]</b> and <b>[Income 🔺]</b> to view spending or inflow distributions.</li>
                        <li><b>Radial Slice Explosion:</b> Slices pop outward with interactive center hero readouts and itemized legends.</li>
                        <li><b>Click Drilldown:</b> Click any slice or legend item to open the deep analytical Entity Tracker for that category.</li>
                    </ul>
                </li>
            </ul>
        """);

        topicContent.put("2. Recording Income Entries", """
            <h2>2. Recording Income Entries</h2>
            <p>Click the <b>+ Add Income</b> button in the header or Transactions tab:</p>
            <ul>
                <li><b>Date / Expected Date:</b> Enter manually or click the calendar button to open the picker.</li>
                <li><b>Expected Checkbox:</b> Check if this income is anticipated in the future rather than already in hand.</li>
                <li><b>Amount:</b> Enter the numerical amount.</li>
                <li><b>Category:</b> Choose from <i>Salary, Relative, Return, Loan, Freelance, Other Income</i>.</li>
                <li><b>Sub-Category:</b> Specific employer, person name, or loan name.</li>
                <li><b>Payment Via:</b> Payment mode (Net Banking, UPI, Cash, Paytm, PhonePe, etc.).</li>
                <li><b>Associated Person:</b> Link to an individual from your Persons master for loan returns.</li>
            </ul>
        """);

        topicContent.put("3. Recording Expenses, Bills & Specialized Natures", """
            <h2>3. Recording Expenses, Bills & Specialized Transaction Natures</h2>
            <p>Click <b>- Add Expense</b> (or <b>+ Add Income</b>) to open the dynamic transaction entry dialog:</p>
            <h3>Transaction Nature Selector</h3>
            <ul>
                <li><b>One-Time Expense (Done vs Expected):</b> For regular day-to-day shopping, fuel, dining. Toggle between <i>Realized (Done)</i> and <i>Planned (Expected)</i>.</li>
                <li><b>One-Time Received (Done vs Expected):</b> For one-off receipts, bonus, or freelance income. Toggle between <i>Realized (Done)</i> and <i>Planned (Expected)</i>.</li>
                <li><b>EMI Payment (Monthly Installment):</b> Selects loan from EMI master, auto-fills monthly amount, and optionally enables <code>[x] Also manage in Due / Upcoming payments schedule</code>.</li>
                <li><b>Credit Card Bill (Variable Monthly):</b> Links to Credit Cards master, accommodating varying monthly statement amounts and due reminders.</li>
                <li><b>Utility / Other Bills (Outright Expenses):</b> Links to Bills master (Electricity, Broadband, Water, Rent).
                    <p style='background:#fef2f2; border-left:4px solid #ef4444; padding:6px 10px; margin:6px 0; color:#991b1b;'>
                    <b>Non-Returnable Bill Principle:</b> Bills, EMIs, and credit cards are outright expenses. <b>They are NEVER considered as loan balances expected back like Given Money</b>, and cannot be nullified or matched in personal loan settlement candidates.
                    </p>
                </li>
                <li><b>Personal Loan / Transfer:</b> Peer-to-peer lending and borrowing linked to contacts with bidirectional settlement tracking.</li>
            </ul>
        """);

        topicContent.put("4. Loans & Dues (Lending & Borrowing) Ledger", """
            <h2>4. Loans & Dues (Lending & Borrowing) Ledger</h2>
            <p>The <b>Loans & Dues</b> tab tracks informal lending and borrowing with zero confusion:</p>
            <ul>
                <li><b>Total Lent:</b> Money you lent out to others.</li>
                <li><b>Total Returned:</b> Money returned back to you.</li>
                <li><b>Total Borrowed:</b> Money you borrowed from friends or relatives.</li>
                <li><b>Total Repaid:</b> Money you paid back.</li>
                <li><b>Net Position:</b>
                    <ul>
                        <li><span style='color:#16a34a; font-weight:bold;'>+ ₹X.XX (Owes You):</span> Person has outstanding dues to pay you.</li>
                        <li><span style='color:#dc2626; font-weight:bold;'>- ₹X.XX (You Owe):</span> You need to pay the person back.</li>
                        <li><span style='color:#64748b; font-weight:bold;'>₹0.00 (Settled):</span> All accounts squared.</li>
                    </ul>
                </li>
            </ul>
            <p>Use <b>- Give Loan</b> and <b>+ Take Loan / Receive Return</b> for quick entries.</p>
            <p><b>Settlement Linking:</b> When you record a loan return or loan given, you can link it directly with counterpart transactions to mark them as partially or 100% fully settled (Nullified).</p>
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

        topicContent.put("6. Filtering, Searching, Sorting & Settlement Linking", """
            <h2>6. Filtering, Searching, Sorting & Settlement Linking</h2>
            <p>In the <b>Transactions</b> tab:</p>
            <ul>
                <li><b>Search Box:</b> Searches notes, categories, sub-categories, and person names simultaneously.</li>
                <li><b>Type Filter:</b> Filter by All, Income (In), or Expense (Out).</li>
                <li><b>Settlement Filter:</b> Filter by <i>All, Nullified Only, Partial Only, or Unlinked Only</i>.</li>
                <li><b>Bidirectional Linking:</b> Right-click any row and choose <b>Link Given / Received Transactions...</b> to allocate counterpart settlements.</li>
                <li><b>Nullified Status &amp; Distinct Color:</b> Fully settled transactions (100% paid/received) are highlighted in <span style='color:#6d28d9; font-weight:bold;'>Royal Purple (✓ Nullified)</span> to immediately distinguish them from active incomes and expenses. Partial settlements show in <span style='color:#d97706; font-weight:bold;'>Amber (Partial %)</span>.</li>
                <li><b>Rich Hover Tooltip:</b> Hover over the settlement badge to view the full list of linked counterpart transactions, dates, and amounts.</li>
                <li><b>Date Presets:</b> Quick presets for <i>Today, This Week, This Month, This Quarter, This Year, All Time</i>, or custom <b>From / To</b> date pickers.</li>
                <li><b>Sorting:</b> Sort by <i>Date, Amount, Type (In/Out), Category, or Sub-Category</i> in Ascending or Descending order.</li>
                <li><b>Summary Bar:</b> Bottom bar shows real-time count, Total In, Total Out, and Net Balance for the filtered results.</li>
            </ul>
        """);

        topicContent.put("7. PDF, Excel & CSV Reports", """
            <h2>7. Exporting Reports (PDF, Excel, CSV) with Master & Filter Metadata</h2>
            <p>In the Transactions tab, click <b>Export Report</b>:</p>
            <ul>
                <li><b>Active Filter Summary Metadata Banner:</b> Clearly records the active filter parameters (Date Range, Categories, Master Entities, Payment Modes, and Amount Limits) at the top of exported reports.</li>
                <li><b>Dedicated Associated Entity / Master Column:</b> Shows exactly which Person, Bill, EMI, or Credit Card is tagged for every transaction.</li>
                <li><b>Export Filtered to PDF (.pdf):</b> Generates an executive landscape document featuring title header, filter summary, 4 KPI summary cards (Total In, Total Out, Balance, Count), colored badges, and itemized data table via OpenPDF.</li>
                <li><b>Export Filtered to Excel (.xlsx):</b> Generates a Microsoft Excel workbook with colored headers, formatted currency values, filter summary block, and double-line totals via Apache POI.</li>
                <li><b>Export Filtered to CSV (.csv):</b> Standard RFC 4180 comma-separated file with full master entity attributes.</li>
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

        topicContent.put("9. Database Storage, Integrity & Full Backup", """
            <h2>9. Database Storage, Integrity Verification & Full Backup</h2>
            <p>All data is maintained inside an embedded H2 database (or your configured external SQL engine) located at:</p>
            <pre style='background:#f1f5f9; padding:8px; border-radius:6px;'>~user/.nav-finance/navfinance_db.mv.db</pre>
            <p>In the <b>Settings & Database</b> tab, powerful data integrity and backup tools are available:</p>
            <ul>
                <li><b>Verify Integrity & Counts:</b> Instantly audits all 9 core relational tables (<i>transactions, persons, emis, credit_cards, bills, recurring_schedules, transaction_links, audit_logs, budgets</i>) with real-time row counts and database health validation.</li>
                <li><b>Quick 1-Click Backup:</b> Instantly exports an automated timestamped CSV snapshot into <code>~/.nav-finance/backups/</code> with zero prompt friction.</li>
                <li><b>Custom Directory CSV Export:</b> Select any target folder or external drive to dump all tables as individual CSV spreadsheets.</li>
                <li><b>Open Database Folder:</b> Launches Windows File Explorer directly into the application data directory.</li>
            </ul>
        """);

        topicContent.put("10. Zoom In / Out & Appearance", """
            <h2>10. UI Zooming, Currency Presets & Appearance</h2>
            <ul>
                <li><b>UI Zoom Scaling:</b> Scale UI magnification smoothly from 80% to 175% with instant re-rendering of vector icons, fonts, and layout bounds.</li>
                <li><b>In-Panel & Header Zoom Controls:</b> Click <code>Zoom In (+)</code>, <code>Zoom Out (-)</code>, or <code>Reset (100%)</code> directly from the Settings panel or use the top-right zoom controls.</li>
                <li><b>Keyboard Shortcuts:</b>
                    <ul>
                        <li><code>Ctrl</code> + <code>Plus (+)</code>: Zoom In</li>
                        <li><code>Ctrl</code> + <code>Minus (-)</code>: Zoom Out</li>
                        <li><code>Ctrl</code> + <code>0</code>: Reset Zoom to 100%</li>
                    </ul>
                </li>
                <li><b>Quick Currency Presets:</b> Click preset buttons (<b>₹</b>, <b>$</b>, <b>€</b>, <b>£</b>, <b>¥</b>, <b>AED</b>) or enter a custom symbol.</li>
                <li><b>Live Currency Preview:</b> View instant live formatting for positive inflow, negative outflow, and balance amounts as you type or select presets.</li>
                <li><b>Light & Dark Mode:</b> Switch themes anytime under <b>Settings & Database</b>.</li>
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

        topicContent.put("12. Entity Tracker & Time Graphs (Days, Week, Months, Quarter, Years)", """
            <h2>12. Entity & Category Tracking Dashboard (Track & Analyze 📊)</h2>
            <p>Access the <b>Track & Analyze</b> tab inside the Transactions view to track financial patterns with timeline graphs:</p>
            <h3>Selecting Entities & Scope</h3>
            <ul>
                <li><b>Entity Type:</b> Track a <i>Person, Utility / Other Bill, EMI Installment, Credit Card, or Category</i>.</li>
                <li><b>Target Entity Dropdown:</b> Select the specific entity to evaluate (e.g., <i>Airtel Fiber, HDFC Millennia, Home Loan EMI, Rahul Sharma</i>).</li>
                <li><b>Time Granularity:</b> Switch instantly between <b>Days</b>, <b>Week</b>, <b>Months</b>, <b>Quarter</b>, and <b>Years</b>. Java-computed chronological aggregation ensures 100% database engine independence!</li>
                <li><b>Date Presets:</b> Quick presets for <i>All Time, This Month, Last 90 Days, This Year</i>, or custom date ranges.</li>
            </ul>
            <h3>Visual Trends & Statement Exports</h3>
            <ul>
                <li><b>4 Real-Time KPI Cards:</b> Total Inflow, Total Outflow, Net Balance, and Average volume per Period.</li>
                <li><b>Visual Dual-Bar Trend Chart:</b> Displays green (Inflow) and red (Outflow) comparison bars with hover spotlight cards and value badges. Clicking any bar instantly filters the itemized ledger table below to that exact time bucket!</li>
                <li><b>Itemized Ledger:</b> View all matching entries with quick export to Excel or PDF statements.</li>
                <li><b>One-Click Deep Links:</b> Click <b>Track & Graph 📊</b> from the Masters tables (Persons, Bills, EMIs, Cards) or Loans & Dues ledger to view that entity's graph immediately.</li>
            </ul>
        """);

        topicContent.put("13. Multi-Database Engines (MySQL, Postgres, MariaDB, SQLite, H2)", """
            <h2>13. Multi-Database Engines & Configuration</h2>
            <p>Nav Finance is built on a clean, engine-agnostic architecture. Switch between popular relational databases directly from the <b>Settings & Database</b> tab:</p>
            <ul>
                <li><b>Supported Engines:</b>
                    <ul>
                        <li><b>H2 (Embedded - Default):</b> Zero-config embedded database stored in your user profile.</li>
                        <li><b>MySQL 8.x:</b> Standard port <code>3306</code> for networked or local multi-device installations.</li>
                        <li><b>PostgreSQL 14+ / 16+:</b> Standard port <code>5432</code> for robust enterprise-grade environments.</li>
                        <li><b>MariaDB 10.x / 11.x:</b> Standard port <code>3306</code> for open-source high-throughput setups.</li>
                        <li><b>SQLite:</b> Standalone file-based relational storage.</li>
                    </ul>
                </li>
                <li><b>Connection Testing:</b> Click <b>Test Connection</b> to verify database host, port, database name, and credentials with latency feedback.</li>
                <li><b>Automated Schema DDL:</b> Clicking <b>Save & Connect</b> automatically verifies tables, creates primary/foreign keys and indexes, seeds default payment modes, and migrates runtime queries live.</li>
                <li><b>One-Click Reset:</b> Click <b>Reset to Default H2</b> anytime to return to local embedded storage safely.</li>
            </ul>
        """);

        topicContent.put("14. System Diagnostics & About Center", """
            <h2>14. System Diagnostics & Executive About Center</h2>
            <p>Access the <b>About Us</b> tab for real-time engine diagnostics, performance telemetry, and system actions:</p>
            <ul>
                <li><b>Live JVM Heap Meter:</b> Dynamic memory gauge showing real-time used heap, allocated RAM, and max heap limit with automatic color-coding (healthy green/blue, warning amber, critical red).</li>
                <li><b>Database Engine & Ping Latency:</b> Asynchronously tests connection round-trip latency to the active database engine without blocking the user interface.</li>
                <li><b>Local Storage Footprint:</b> Live inspection of the primary embedded <code>navfinance_db.mv.db</code> disk size and user profile directory.</li>
                <li><b>Quick Actions Bar:</b> Direct 1-click shortcuts to open the local application data directory in Windows File Explorer, switch to the User Manual, jump to Database Settings, or copy a full diagnostics snapshot to the clipboard for support.</li>
                <li><b>Architectural Pillars:</b> Summary of zero-telemetry local privacy, bi-directional linking, tri-script typography, and multi-engine portability.</li>
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
