# Nav Finance 💰

> **A Modern Personal Finance & Udhaar Management System for Windows**  
> Built with **Java 21**, **Modern Swing (FlatLaf)**, and an embedded **H2 Database** stored locally at `~user/nav-finance`.

---

## 🌟 Use Case & Problem Solved

Managing daily household finances, recurring bills, EMIs, credit cards, and informal loans (*Udhaar Given or Taken*) is often painful with generic spreadsheets or cloud apps that compromise privacy.

**Nav Finance** is designed specifically for personal and family finances in India and worldwide:
- **Private & Local-First**: No external cloud tracking. All financial data is securely kept in an embedded H2 database located in your user profile: `~user/nav-finance/navfinance_db.mv.db`.
- **Income & Expense Tracking**: Categorize daily transactions, recurring salaries, dividend returns, credit card payments, household utility bills, and personal EMIs.
- **Udhaar & Personal Loans Ledger**: Track informal lending and borrowing between friends, relatives, and colleagues with exact contact, relation, and net settlement tracking (*Who owes whom*).
- **Audit Details**: Every creation, modification, and deletion records full timestamps and change logs for complete financial integrity.
- **Executive Dashboard**: Visual trends of In vs Out (Green and Red) categorized by **Weekly**, **Monthly**, **Quarterly**, or **Annually**.
- **Multi-Format Reports**: Export filtered records with a single click to **PDF**, **Excel (.xlsx)**, or **CSV**.
- **Full Database CSV Backup**: Export all database tables as standard CSV files for offline backups or spreadsheet analysis.

---

## 🚀 Key Features

### 1. In & Out Income/Expense Tracking
- **Income Entry**:
  - **Date / Expected Date**: Dual input with interactive **`📅` Date Picker** popup and manual typing (`YYYY-MM-DD` / `DD-MM-YYYY`)
  - **Amount** formatted in INR (`₹`) or custom currency symbol
  - **Category**: `Salary`, `Relative`, `Return`, `Loan`, `Udhaar`, `Freelance`, `Other Income`
  - **Sub-Category**: `Person`, `Loan`, `Bonus`, `Others`
  - **Payment Mode**: Cash, Net Banking, UPI, Paytm, PhonePe, Cheque, etc.
- **Expense Entry**:
  - **Expense Date / Planned Date**: Dual input with interactive **`📅` Date Picker** popup and manual typing
  - **Amount**
  - **Via (Payment Mode)**: `Cash`, `Paytm`, `PhonePe`, `UPI`, `Net Banking`, `Credit Card`, `Debit Card`, `Other` *(supports custom modes on-the-fly)*
  - **Category**: `EMI Payment`, `Credit Card`, `Udhar Given`, `Bill`, `Special Expense`, `Groceries & Food`, `Shopping`, `Fuel & Travel`, `Health`
  - **Dynamic Sub-Category**:
    - When selecting **Bill** → Pick from registered bills (e.g., *Airtel Bill*, *Electricity (Khardoni)*, *Electricity (Noida)*)
    - When selecting **Credit Card** → Pick from registered credit cards (e.g., *HDFC Millennia*, *ICICI Amazon Pay*, *SBI SimplyCLICK*)
    - When selecting **EMI Payment** → Pick from registered loan EMIs (e.g., *Home Loan EMI*, *Car Loan EMI*, *Laptop EMI*)
    - When selecting **Udhar Given** → Pick from registered Persons (*Rahul*, *Amit*, *Suresh*, etc.)

### 2. Comprehensive Masters Catalog
- **Persons Master**: Maintains list of contacts with `Name`, `Contact`, `Relation` (Friend, Colleague, Relative, Family, Landlord), and `Notes`.
- **Bills Master**: Pre-configured utility bills (e.g., *Airtel Bill*, *Electricity (Khardoni)*, *Electricity (Noida)*, *IGL Gas*, *Water*) with default expected amounts.
- **EMI Master**: Pre-configured recurring loan installments (e.g., *Home Loan EMI*, *Car Loan EMI*, *Personal Loan*, *Laptop EMI*) with monthly dues and lender bank.
- **Credit Cards Master**: Store bank name, card name, and last 4 digits for easy expense attribution.
- **Payment Modes**: Manage payment modes used across transactions.

### 3. Udhaar & Personal Loan Ledger
- Dedicated ledger computing:
  - **Total Udhar Given** (money lent to others)
  - **Total Udhar Returned** (money returned by others)
  - **Total Udhar Taken** (money borrowed from others)
  - **Total Repaid** (money paid back by you)
  - **Net Balance** (clearly indicates *Owes You* in Green or *You Owe* in Red)

### 4. Interactive UI & Analytics Dashboard
- **Period Breakdown**: Switch seamlessly between **Weekly**, **Monthly**, **Quarterly**, and **Annually** with `< Prev` and `Next >` timeline navigation.
- **KPI Summary Cards**:
  - **Total Income (In)** in **Green**
  - **Total Expense (Out)** in **Red**
  - **Net Balance / Savings** in **Blue**
  - **Udhar Position** in **Amber**
- **Trend Chart**: Dual-colored Bar Chart comparing In (Green) vs Out (Red) over time.
- **Category Donut Chart**: Visual distribution of expenses by category with percentages and legends.
- **Recent Activity**: Quick view of latest 10 transactions.

### 5. Multi-Criteria Filtering & Sorting
- **Filter By**:
  - Type (`All`, `Income`, `Expense`)
  - Category & Dynamic Sub-Category
  - Payment Mode (`Via`)
  - Associated Person
  - Date Presets (`Today`, `This Week`, `This Month`, `This Quarter`, `This Year`, `Custom Range`)
  - Min / Max Amount range
  - Keyword Search (across notes, subcategory, and person names)
- **Sort By**:
  - Type (In/Out)
  - Category
  - Sub-Category
  - Amount
  - Date (Ascending / Descending)

### 6. Audit Trail & Change Logging
- Automatic logging of all **CREATE**, **UPDATE**, and **DELETE** actions.
- Captures precise timestamps, entity modified, and human-readable diff details (e.g., *Amount changed from ₹2,000.00 to ₹2,500.00; Via: Cash -> UPI*).

### 7. Exporting & Backup Options
- **Export Filtered Results to PDF**: Clean document with title, summary metric cards, colored badges, and itemized table.
- **Export Filtered Results to Excel (.xlsx)**: Formatted workbook with currency numbers, color-coded rows, and double-line totals.
- **Export Filtered Results to CSV**: Standard comma-separated file.
- **Export All Database as CSV Files**: One-click dump of all 7 database tables into individual CSV files.

### 8. Bulk CSV Import, Update & Deletion
- **Import Dialog**: Upload any standard CSV file to perform bulk transactions.
- **Download CSV Template**: One-click generation of `nav_finance_template.csv` containing instructions and sample rows for `CREATE`, `UPDATE`, and `DELETE` actions.
- **Auto Validation & Color-Coded Preview**: Table validation showing row numbers, badge-coded actions (`CREATE` in Green, `UPDATE` in Blue, `DELETE` in Red), and instant validation feedback.
- **Batch Execution**: Executes all valid lines in a single batch, updating person links, auto-registering novel payment modes, and logging audit events for each change.

### 9. Dynamic Zoom In / Out & UI Scaling
- **Header Controls**: Magnify the UI using `🔍+` (Zoom In) and `🔍-` (Zoom Out), or click the percentage badge (e.g., `100%`) to reset.
- **Scaling Range**: Scalable across `80%`, `90%`, `100%`, `115%`, `130%`, `150%`, and `175%`.
- **Keyboard Shortcuts**:
  - `Ctrl` + `+` / `Ctrl` + `=`: Zoom In
  - `Ctrl` + `-`: Zoom Out
  - `Ctrl` + `0`: Reset Zoom to 100%
- Fully scales UIManager fonts, cards, grids, buttons, and popups dynamically.

### 10. In-App User Guide & About Us Documentation
- **📖 In-App User Guide**: Searchable interactive documentation with a topic index on the left and HTML reader on the right explaining all operations, calculations, and shortcuts.
- **ℹ️ About Us**: Technical architecture specifications, Java 21 runtime metrics, H2 database file paths, and local-first privacy commitments.

### 11. Recurring Payment Schedules on Specific Dates of Month
- **Specific Day of Month**: Configure recurring obligations on any day from `1` to `31` (e.g., *EMI of ₹20,000 on 5th of every month from Jan 2026 to Jan 2027*).
- **Fixed vs Variable Amounts**:
  - **Fixed Amount**: Recurring obligations with constant dues (e.g. EMIs, rent, SIP).
  - **Variable Amount**: Repeating bills whose amounts vary each month (e.g. *Electricity Bill*, *Credit Card Bill*). The system alerts and prompts the user to enter the actual amount when due/settling.
- **Auto-Payment vs Manual Execution**:
  - `⚡ Auto-Payment`: Bank auto-debits, NACH mandates, and standing instructions.
  - `🖐 Manual Payment`: Directly executed payments by the user.

### 12. Due & Overdue Payments Tracker
- **Dedicated Tab**: `⏰ Due & Recurring` featuring real-time KPI cards for **Overdue Amount/Count**, **Upcoming Due**, **Settled Amount**, and **Auto vs Manual Ratio**.
- **Visual Status Badges**:
  - `🔴 OVERDUE (+Xd)`: Highlights past due date unpaid bills in bright red.
  - `🟡 DUE (in Xd)` / `DUE TODAY`: Upcoming bills due soon.
  - `🟢 SETTLED`: Completely paid entries.
- **Interactive Settlement (`✔ Settle / Pay`)**: Prompts for the exact bill amount (for variable bills), settlement date, payment mode, and automatically logs the transaction into your Expense ledger.
- **Mark Unsettled (`↩ Mark Unsettled`)**: Reverts settled bills to pending and safely removes the linked ledger transaction if desired.

---

## 📁 Database Architecture

The embedded database is created automatically upon first run at:
```text
C:\Users\<username>\nav-finance\navfinance_db.mv.db
```
- **Engine**: H2 Database 2.2
- **Mode**: Embedded Multi-Process (`AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1`)
- **Tables**:
  1. `transactions`
  2. `persons`
  3. `bills`
  4. `emis`
  5. `credit_cards`
  6. `payment_modes`
  7. `recurring_schedules`
  8. `due_payments`
  9. `audit_logs`

---

## 🛠️ Project Structure

```text
nav-finance/
├── pom.xml                               # Maven build descriptor (no test/junit dependencies)
├── .gitignore                            # Windows & Java Git exclusions
├── run.bat                               # Windows Batch double-click launcher
├── run.ps1                               # PowerShell launcher script
├── README.md                             # Project overview & technical specification
├── USER_GUIDE.md                         # End-user operational manual
└── src/main/java/com/navfinance/
    ├── Main.java                         # Application entry point & FlatLaf launcher
    ├── config/
    │   └── AppConfig.java                # Global constants, DB paths, currency & date formatters
    ├── database/
    │   └── DatabaseManager.java          # H2 initialization, schema migration & CSV backups
    ├── model/
    │   ├── AmountType.java               # FIXED vs VARIABLE amount enum
    │   ├── DuePayment.java               # Individual due date instance entity
    │   ├── DueStatus.java                # PENDING, OVERDUE, SETTLED status enum
    │   ├── RecurringSchedule.java        # Recurring rule entity (day of month, auto-pay)
    │   ├── Transaction.java              # Transaction entity
    │   ├── TransactionType.java          # INCOME / EXPENSE enum
    │   ├── Person.java                   # Person master entity
    │   ├── Bill.java                     # Utility bill master entity
    │   ├── Emi.java                      # EMI master entity
    │   ├── CreditCard.java               # Credit card master entity
    │   ├── AuditLog.java                 # Audit trail entity
    │   ├── FilterCriteria.java           # Multi-criteria filter & sort model
    │   └── DashboardSummary.java         # Metric aggregation & trend points
    ├── dao/
    │   ├── DuePaymentDao.java            # Due payments CRUD & settlement tracking
    │   ├── RecurringScheduleDao.java     # Recurring rules repository
    │   ├── TransactionDao.java           # Transaction queries, filters & aggregations
    │   ├── PersonDao.java                # Person CRUD
    │   ├── BillDao.java                  # Bill CRUD
    │   ├── EmiDao.java                   # EMI CRUD
    │   ├── CreditCardDao.java            # Credit Card CRUD
    │   ├── PaymentModeDao.java           # Payment mode repository
    │   └── AuditDao.java                 # Audit log repository
    ├── service/
    │   ├── FinanceService.java           # Central business logic & audit coordinator
    │   ├── AnalyticsService.java         # Periodic metrics & chart calculations
    │   ├── ExportService.java            # PDF, Excel, and CSV export engines
    │   ├── CsvImportService.java         # Bulk CSV validation, template & batch executor
    │   └── RecurringService.java         # Recurring schedule generation & due settlement
    └── ui/
        ├── MainFrame.java                # Main GUI window with tabbed layout & zoom shortcuts
        ├── util/
        │   ├── UiUtils.java              # Palette colors, button factories & dialog helpers
        │   └── ZoomManager.java          # Font & FlatLaf scaling engine (80% - 175%)
        ├── components/
        │   ├── DatePickerField.java      # Dual manual entry + calendar popup picker
        │   ├── KpiCard.java              # Gradient KPI summary card
        │   ├── TrendChartPanel.java      # Anti-aliased dual bar chart (In vs Out)
        │   └── CategoryPieChartPanel.java# Anti-aliased category donut chart
        ├── dialogs/
        │   ├── TransactionDialog.java    # Dynamic Income/Expense modal dialog
        │   ├── CsvImportDialog.java      # CSV file preview, template download & executor
        │   ├── RecurringScheduleDialog.java # Register & edit monthly recurring schedules
        │   ├── SettlePaymentDialog.java  # Settle due payments with variable amount prompts
        │   ├── PersonDialog.java         # Add/Edit Person modal dialog
        │   ├── BillDialog.java           # Add/Edit Bill modal dialog
        │   ├── EmiDialog.java            # Add/Edit EMI modal dialog
        │   └── CreditCardDialog.java     # Add/Edit Credit Card modal dialog
        └── panels/
            ├── DashboardPanel.java       # Visual executive dashboard
            ├── TransactionsPanel.java    # Search, filter, sort & export transactions
            ├── DuePaymentsPanel.java     # Due & overdue tracker & recurring rules manager
            ├── UdharTrackerPanel.java    # Person-wise loan & Udhaar ledger
            ├── MastersPanel.java         # Masters management tabs
            ├── AuditLogPanel.java        # Audit trail viewer
            ├── UserGuidePanel.java       # Interactive in-app user guide & documentation
            ├── AboutUsPanel.java         # Architecture, runtime specs & privacy philosophy
            └── SettingsPanel.java        # Database details, CSV backup & themes
```

---

## ⚙️ Prerequisites

- **Java JDK**: 21 or higher (Oracle JDK, GraalVM JDK 21, Eclipse Temurin 21)
- **Maven**: 3.8+ or 3.9+

---

## 🚀 How to Build & Run

### Method 1: Using Windows Double-Click Launcher
Double-click `run.bat` in the project directory. It compiles the project (if not already compiled) and opens the application window.

### Method 2: Using PowerShell
```powershell
.\run.ps1
```

### Method 3: Using Maven Command Line
```powershell
# 1. Compile and package the executable JAR (no test/junit)
mvn clean package

# 2. Run the packaged Fat JAR
java -jar target/nav-finance-1.0.0.jar
```

---

## 📦 Building Native Installers & Artifacts

NavFinance includes automated tooling to generate standalone and production-ready distribution packages:

### Automated One-Click Build Script
Run the automated installer builder script:
```cmd
build.bat
```
*(or run `scripts\build-installers.bat`)*

This script automatically executes Maven shading, verifies artifacts, and runs JDK `jpackage` + WiX Toolset to build all three formats in `target/artifacts/`:

| Artifact | Location | Description |
| :--- | :--- | :--- |
| **Standalone Executable Fat JAR** | `target/artifacts/NavFinance-1.0.0.jar` | Portable shaded JAR with all dependencies included (requires Java 21). |
| **Portable App with Bundled JRE** | `target/artifacts/NavFinance-1.0.0-jre21-winX64.zip` | Standalone portable `.zip` containing `NavFinance.exe` and a bundled JRE 21 runtime (runs without needing Java installed on target Windows machine). |
| **Windows Native MSI Installer** | `target/artifacts/NavFinance-1.0.0.msi` | Windows installer with Desktop shortcut, Start Menu group (`NavFinance`), and install directory selection. |

> **Note**: Building `.msi` installers requires the **WiX Toolset v3.0+** (`candle.exe` and `light.exe`) installed and accessible in the system PATH.

---

## 📄 License
This project is licensed under the MIT License - see the `LICENSE` file for details.
