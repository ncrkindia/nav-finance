# Nav Finance - User Guide 📘

Welcome to **Nav Finance**, your personal finance companion for tracking household income, daily expenses, utility bills, loan EMIs, and informal lending/borrowing (*Udhaar*).

---

## Table of Contents
1. [Starting the Application](#1-starting-the-application)
2. [Navigating the Dashboard](#2-navigating-the-dashboard)
3. [Recording Income Entries](#3-recording-income-entries)
4. [Recording Expense Entries](#4-recording-expense-entries)
5. [Managing Udhaar & Personal Loans](#5-managing-udhaar--personal-loans)
6. [Managing Catalog Masters (Bills, EMIs, Persons)](#6-managing-catalog-masters)
7. [Filtering, Searching & Sorting Transactions](#7-filtering-searching--sorting-transactions)
8. [Exporting Reports (PDF, Excel, CSV)](#8-exporting-reports)
9. [Full Database Backup (Export All as CSV)](#9-full-database-backup)
10. [Reviewing Audit Logs](#10-reviewing-audit-logs)
11. [Bulk CSV Import, Update & Deletion](#11-bulk-csv-import-update--deletion)
12. [Zoom In, Zoom Out & UI Scaling](#12-zoom-in-zoom-out--ui-scaling)
13. [In-App User Guide & About Us](#13-in-app-user-guide--about-us)
14. [Recurring Schedules, Due & Overdue Payments](#14-recurring-schedules-due--overdue-payments)

---

## 1. Starting the Application

### Option A: One-Click Windows Launch
Double-click `run.bat` in the project directory.

### Option B: PowerShell
```powershell
.\run.ps1
```

### Option C: Command Line
```powershell
java -jar target/nav-finance-1.0.0.jar
```

Upon starting, the application automatically ensures the database directory exists at:
```text
C:\Users\<YourUser>\nav-finance\navfinance_db.mv.db
```
Initial master records (e.g., *Rahul Sharma*, *Airtel Bill*, *Electricity (Khardoni)*, *Electricity (Noida)*, *Home Loan EMI*, etc.) and sample transactions will be seeded automatically on first run.

---

## 2. Navigating the Dashboard

The **Dashboard** tab is your financial control center:

### KPI Summary Cards
- **Total Income (In)**: Displayed in **Green**. Represents total earnings and receipts in the active period.
- **Total Expense (Out)**: Displayed in **Red**. Represents total expenditures, bills, and money lent out.
- **Net Savings / Balance**: Displayed in **Blue** or **Green**. Represents `Income - Expense`.
- **Udhaar Ledger Position**: Displayed in **Amber**. Shows your net pending balance across all loans given vs borrowed.

### Changing the Time Period
In the top bar of the dashboard, choose from:
- **Weekly**: Breaks down transactions by Monday to Sunday week.
- **Monthly**: Focuses on the calendar month (e.g., October 2026).
- **Quarterly**: Focuses on calendar quarters (Q1, Q2, Q3, Q4).
- **Annually**: Focuses on the whole calendar year.

Use the `◀ Prev` and `Next ▶` buttons to navigate back and forward through historical periods, or click `Current Period` to jump back to today.

### Interactive Visual Charts
1. **Income vs Expense Trend (Dual Bar Chart)**:
   - Green bars represent **Income (In)**.
   - Red bars represent **Expense (Out)**.
   - Compares the last 6 months (or weeks/quarters) side-by-side.
2. **Expense Breakdown by Category (Donut Chart)**:
   - Shows where your money went (e.g., *EMI Payment*, *Bills*, *Credit Cards*, *Udhar Given*).
   - Displays percentage share and formatted amounts in the right legend.

---

## 3. Recording Income Entries

1. Click **`+ Add Income`** (in the top header or transactions panel).
2. The form opens pre-selected to **Income (In)**:
   - **Date / Expected Date (Date Picker + Manual Entry)**:
     - **Manual Entry**: Type directly in `YYYY-MM-DD` (or `DD-MM-YYYY`) format.
     - **Date Picker**: Click the **`📅`** button right next to the field to open the visual calendar popup. Navigate months/years, click any day, or click **Today** to select.
   - **Expected Checkbox**: Check if this income is anticipated in the future rather than already in hand.
   - **Amount**: Enter the numerical amount (e.g. `95000` or `15000`).
   - **Category**: Select from:
     - `Salary`
     - `Relative`
     - `Return` (e.g. return of previously given loan)
     - `Loan` (money borrowed from a bank or friend)
     - `Udhaar` (informal money taken)
     - `Freelance / Business`
     - `Other Income`
   - **Sub-Category**: Select or type the specific source (e.g. `Primary Employer`, `Tech Corp`, or select a person's name).
   - **Payment Via**: Select payment channel (`Net Banking`, `UPI`, `Cash`, `PhonePe`, `Paytm`, `Cheque`, etc.).
   - **Associated Person**: If the income comes from an individual (such as an Udhaar return from *Rahul Sharma*), pick the person from the dropdown.
   - **Notes / Remarks**: Add any descriptive detail.
3. Click **Save Entry**. An audit log will be created automatically.

---

## 4. Recording Expense Entries

1. Click **`- Add Expense`** (in the top header or transactions panel).
2. The form opens configured for **Expense (Out)**:
   - **Date / Planned Date (Date Picker + Manual Entry)**:
     - **Manual Entry**: Type directly in `YYYY-MM-DD` format.
     - **Date Picker**: Click **`📅`** to select from the interactive calendar.
   - **Planned Checkbox**: Check if this is an upcoming bill or planned expense not yet paid.
   - **Amount**: Enter the expense amount (e.g. `2450.00`).
   - **Via (Payment Mode)**: Select from `Cash`, `Paytm`, `PhonePe`, `UPI`, `Credit Card`, `Debit Card`, `Net Banking`, or type a custom payment mode directly.
   - **Category**: Choose the category:
     - `Bill`
     - `EMI Payment`
     - `Credit Card`
     - `Udhar Given`
     - `Special Expense`
     - `Groceries & Food`
     - `Shopping`
     - `Fuel & Travel`
   - **Sub-Category** *(Smart Auto-Populated)*:
     - If Category is **Bill**, the sub-category dropdown automatically loads your bill list:
       - *Airtel Bill*
       - *Electricity (Khardoni)*
       - *Electricity (Noida)*
       - *Piped Gas (IGL)*
       - *Water Bill*
     - If Category is **Credit Card**, it automatically loads your cards:
       - *HDFC Millennia (..4128)*
       - *ICICI Amazon Pay (..9051)*
       - *SBI SimplyCLICK (..6733)*
     - If Category is **EMI Payment**, it automatically loads your EMIs:
       - *Home Loan EMI*
       - *Car Loan EMI*
       - *Personal Loan*
       - *Laptop EMI*
     - If Category is **Udhar Given**, it automatically lists registered persons.
   - **Notes / Remarks**: Additional transaction comments.
3. Click **Save Entry**.

---

## 5. Managing Udhaar & Personal Loans

Click the **`🤝 Udhaar & Loans`** tab to access the dedicated personal loan and Udhaar ledger:

- **Total Udhar Given**: Total amount you have lent out to friends, relatives, and colleagues.
- **Total Udhar Taken**: Total amount you have borrowed from others.
- **Net Udhar Position**: Overall net position:
  - If positive: Others owe you money in total.
  - If negative: You owe others money in total.
- **Person-Wise Ledger Table**:
  - Displays every person, their contact and relation.
  - Shows **Total Given**, **Total Returned**, **Total Taken**, **Total Repaid**.
  - **Net Position**:
    - **`+ ₹5,000.00` (Owes You)** in **Green**
    - **`- ₹10,000.00` (You Owe)** in **Red**
    - **`₹ 0.00` (Settled)** in **Gray**
- **Quick Actions**:
  - Click **`- Give Udhar`** to quickly record money lent to someone.
  - Click **`+ Take Udhar / Receive Return`** to record money received back or borrowed.

---

## 6. Managing Catalog Masters

Click the **`🗂️ Masters`** tab to customize your entities:

1. **Persons Sub-Tab**:
   - Add new friends, relatives, landlords, or colleagues with **Name**, **Contact**, **Relation**, and **Notes**.
   - Edit or delete existing persons.
2. **Bills Sub-Tab**:
   - Add regular bills (e.g., *Airtel Bill*, *Electricity (Khardoni)*, *Electricity (Noida)*).
   - Specify the provider and typical monthly amount.
3. **EMI Master Sub-Tab**:
   - Add recurring bank loans (e.g., *Home Loan EMI*, *Car Loan EMI*, *Laptop EMI*).
   - Specify the lender bank and fixed monthly installment.
4. **Credit Cards Sub-Tab**:
   - Add credit cards with card name, bank, and last 4 digits.
5. **Payment Modes (Via) Sub-Tab**:
   - Add custom payment methods (e.g. *Amazon Pay*, *Mobikwik*, *Sodexo*).

---

## 7. Filtering, Searching & Sorting Transactions

Open the **`💳 Transactions`** tab:

### Multi-Criteria Filters
- **Search Keyword**: Type any word to search across notes, categories, subcategories, or person names.
- **Type**: Filter by `All`, `Income (In)`, or `Expense (Out)`.
- **Category & Sub-Category**: Filter by specific category (e.g. *Bill*) and specific bill name.
- **Date Range Preset**:
  - `Today`
  - `This Week`
  - `This Month`
  - `This Quarter`
  - `This Year`
  - `Custom` (choose via visual **`📅`** Date Picker or type manually in *From* and *To* boxes)
- **Via (Mode)**: Filter transactions paid through *Paytm*, *PhonePe*, *UPI*, *Cash*, etc.
- **Person**: Filter all transactions linked to a particular person.
- **Amount Range**: Enter Minimum and/or Maximum amount.

### Sorting Options
- **Sort By**: Choose between **Date**, **Amount**, **Type (In/Out)**, **Category**, or **Sub Category**.
- **Sort Direction**: Select **Desc ▼** (highest/newest first) or **Asc ▲** (lowest/oldest first).

### Editing & Deleting
- **Double-click** any transaction row to open the edit dialog.
- Alternatively, select a row and click **`✎ Edit`** or **`🗑 Delete`**.

---

## 8. Exporting Reports

To export the filtered transactions:
1. Apply your desired filters in the **Transactions** tab.
2. Click the blue **`⬇ Export Report ▼`** button in the top right.
3. Choose your format:
   - **Export Filtered to PDF (.pdf)**: Generates an elegant landscape report with header details, summary statistics cards (Total In, Total Out, Balance), colored status badges, and itemized table.
   - **Export Filtered to Excel (.xlsx)**: Generates a professionally styled Microsoft Excel spreadsheet with formatted numbers, colored rows, bold headers, and auto-adjusted column widths.
   - **Export Filtered to CSV (.csv)**: Generates a standard RFC 4180 comma-separated file compatible with Google Sheets, Excel, or Python scripts.
4. Choose the destination folder and filename, then click **Save**.

---

## 9. Full Database Backup

To export the entire database as standard CSV files:
1. Click the **`⚙️ Settings & Database`** tab.
2. In the **Full Database Export** section, click **`Export All Database as CSV Files`**.
3. Choose a destination folder (defaults to `~user/nav-finance/backups/backup_<timestamp>`).
4. All 7 tables will be exported as:
   - `transactions.csv`
   - `persons.csv`
   - `bills.csv`
   - `emis.csv`
   - `credit_cards.csv`
   - `payment_modes.csv`
   - `audit_logs.csv`

---

## 10. Reviewing Audit Logs

Click the **`📝 Audit Trail`** tab:
- Every action taken in the system is recorded with:
  - **Date & Time** (exact timestamp)
  - **Action** (`CREATE` in Green, `UPDATE` in Blue, `DELETE` in Red)
  - **Entity Type** (`TRANSACTION`, `PERSON`, `BILL`, `EMI`, `CREDIT_CARD`)
  - **Change Details / Diff**: Detailed description of what changed (e.g. *Amount changed from ₹2,000.00 to ₹2,500.00; Via: Cash -> UPI*).
- Use the **Search** box and **Action** dropdown to filter specific audit events.

---

## 11. Bulk CSV Import, Update & Deletion

Nav Finance allows bulk operations via standard CSV files for initial data migration, offline transaction bookkeeping, or batch deletions.

### Step 1: Downloading the CSV Template
1. In the **`💳 Transactions`** tab, click **`📤 Import CSV`** (or open the **`⬇ Export / Templates ▼`** menu and click **`Download CSV Template (Import/Update/Delete)`**).
2. Save the template (default name: `nav_finance_template.csv`).
3. The template comes pre-populated with documentation and sample rows for all three actions:
   ```csv
   Action,ID,Type,Date,Expected,Amount,Via,Category,SubCategory,Person,Notes
   CREATE,,INCOME,2026-10-01,false,95000.00,Net Banking,Salary,Tech Corp India,,Monthly salary credit
   CREATE,,EXPENSE,2026-10-04,false,1199.00,Paytm,Bill,Airtel Bill,,Home fiber bill
   CREATE,,EXPENSE,2026-10-05,false,28500.00,Net Banking,EMI Payment,Home Loan EMI,,Auto-debit for Home loan
   CREATE,,EXPENSE,2026-10-15,false,5000.00,UPI,Udhar Given,Rahul Sharma,Rahul Sharma,Urgent loan to friend
   UPDATE,1,INCOME,2026-10-01,false,98500.00,Net Banking,Salary,Tech Corp India,,Salary with festival bonus
   DELETE,999,,,,,,,,,,Deleting unwanted entry
   ```

### Step 2: Preparing Your CSV File
- **Column Explanations**:
  - **`Action`**: `CREATE` (for new transaction), `UPDATE` (edits an existing transaction by ID), or `DELETE` (deletes transaction by ID).
  - **`ID`**: Leave empty for `CREATE`. Required for `UPDATE` and `DELETE`.
  - **`Type`**: `INCOME` (or `IN`) / `EXPENSE` (or `OUT`).
  - **`Date`**: Flexible format support: `YYYY-MM-DD`, `DD-MM-YYYY`, or `DD/MM/YYYY`.
  - **`Expected`**: `true` or `false` (default: false).
  - **`Amount`**: Number without currency symbols (e.g. `15000.00`). Commas are automatically stripped.
  - **`Via`**: Payment mode (`Cash`, `Paytm`, `PhonePe`, `UPI`, `Net Banking`, etc.). Novel modes are automatically added to the master list.
  - **`Category`**: Primary category (e.g., `Salary`, `Bill`, `EMI Payment`, `Udhar Given`).
  - **`SubCategory`**: Sub-category (e.g., *Airtel Bill*, *Home Loan EMI*, *Rahul Sharma*).
  - **`Person`**: Person's name (auto-links to your registered Persons list).
  - **`Notes`**: Optional remarks.

### Step 3: Validating & Executing Import
1. In the **Bulk CSV Import & Update Manager** dialog, click **`📂 Select CSV File...`**.
2. The preview grid parses and checks every row:
   - **Action Badge**: Color-coded `CREATE` (Green), `UPDATE` (Blue), and `DELETE` (Red).
   - **Validation Status**: `Valid` (Green) or exact error description (Red) if fields are missing or invalid IDs are provided.
3. Once satisfied, click **`✔ Execute Import`**. All modifications are committed with corresponding audit log entries.

---

## 12. Zoom In, Zoom Out & UI Scaling

To ensure comfortable readability across 1080p, 2K, 4K, and high-DPI laptop screens, Nav Finance features dynamic UI zooming:

- **Zoom Controls Bar**:
  - Located at the top right of the application header.
  - **`🔍-` Button**: Zooms out, decreasing font and UI component sizing.
  - **Zoom Badge (e.g. `100%`)**: Clicking resets magnification to the standard 100% baseline.
  - **`🔍+` Button**: Zooms in, increasing font and UI component sizing.
- **Magnification Levels Supported**:
  - `80%`, `90%`, `100%` (Default), `115%`, `130%`, `150%`, `175%`
- **Global Keyboard Shortcuts**:
  - **Zoom In**: `Ctrl` + `+` (or `Ctrl` + `=`)
  - **Zoom Out**: `Ctrl` + `-`
  - **Reset Zoom**: `Ctrl` + `0`
- Scaling seamlessly re-renders all tables, buttons, cards, headers, and tabs across all open windows without requiring an application restart.

---

## 13. In-App User Guide & About Us

Nav Finance includes built-in reference documentation and system specifications directly within the interface:

### 📖 In-App User Guide Tab
- Accessible from the top tab bar: **`📖 User Guide`**.
- Features an interactive topic index on the left with live topic filtering and search.
- Clean formatted HTML reader explaining every feature, formula, shortcut, and data flow.

### ℹ️ About Us Tab
- Accessible from the top tab bar: **`ℹ️ About Us`**.
- Displays:
  - **Application Overview**: Purpose, version, and architecture.
  - **Runtime Specifications**: Current Java version & vendor, OS details, embedded H2 engine path, FlatLaf theme version, and PDF/Excel libraries.
  - **Privacy Guarantee**: Outlines our offline philosophy—100% local storage with zero cloud dependencies or telemetries.
  - **License**: Free and Open-Source under the MIT License.

---

## 14. Recurring Schedules, Due & Overdue Payments

Under the **`⏰ Due & Recurring`** tab, you can register repeating obligations and systematically manage pending or overdue bills.

### Step 1: Registering a Recurring Schedule
Click **`+ Register Recurring Schedule`**:
1. **Schedule Name**: Name the obligation (e.g. *Home Loan EMI*, *Electricity Bill (Noida)*, *Car EMI*).
2. **Type & Category**: Select Expense/Income and Category (`EMI Payment`, `Bill`, `Credit Card`, `Salary`, etc.).
3. **Specific Date of Month**: Select the recurring day from `1` to `31` (e.g. **5th of every month**).
4. **Amount Type**:
   - **Fixed Amount**: For constant values (e.g., ₹20,000 EMI). Enter the amount.
   - **Variable Amount**: For bills that change each month (e.g. *Electricity Bill*, *Credit Card Bill*). The system will prompt you for the exact amount when settling.
5. **Date Range**:
   - Set **Start Month** (e.g. `2026-01-01`).
   - Set **End Month** (e.g. `2027-01-31`), or check **Ongoing / No End Date**.
6. **Execution Mode**:
   - **`⚡ Auto-Payment`**: For bank auto-debits, NACH mandates, or recurring standing instructions.
   - **`🖐 Manual Payment`**: For bills where you manually open an app or write a cheque.
7. Click **`✔ Save Schedule`**. The system automatically generates due occurrences for every month in the active window.

### Step 2: Tracking Due & Overdue Statuses
In the **Due & Overdue Tracker** grid:
- **`🔴 OVERDUE (+Xd)`**: Bills past their due date that haven't been paid.
- **`🟡 DUE (in Xd)` / `DUE TODAY`**: Upcoming obligations due today or within the coming days.
- **`🟢 SETTLED`**: Paid entries showing the exact amount paid, date, and payment mode.

### Step 3: Settling Payments
1. Select any due entry and click **`✔ Settle / Pay Selected`** (or double-click the row).
2. If the bill was registered as **Variable Amount**, the modal prominently highlights:
   > *"💡 Variable Amount Bill: Please enter the exact billed amount for this period:"*
3. Enter or adjust the actual amount, confirm the settlement date, and choose the payment channel.
4. Keep the checkbox checked to automatically create a transaction in your Expense ledger.
5. Click **Confirm Settlement**.

### Step 4: Reverting Settled Payments (Mark Unsettled)
- If an entry was marked settled by mistake, select it and click **`↩ Mark Unsettled`**.
- The app asks if you would also like to remove the linked transaction from your ledger. Confirming returns the item to `PENDING` (or `OVERDUE`) and keeps your ledger balanced.

---

*Thank you for using Nav Finance! For suggestions or modifications, feel free to update the codebase.*
