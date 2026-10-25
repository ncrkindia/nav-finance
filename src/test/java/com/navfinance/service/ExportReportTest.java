package com.navfinance.service;

import com.lowagie.text.pdf.PdfReader;
import com.navfinance.config.AppConfig;
import com.navfinance.model.FilterCriteria;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests validating professional report generation across CSV, Excel, and PDF formats,
 * ensuring filter metadata, date ranges, and associated master details (Person, Bill, EMI, Card)
 * are cleanly and accurately captured.
 */
public class ExportReportTest {

    private static ExportService exportService;

    @BeforeAll
    static void setUp() {
        com.navfinance.i18n.I18n.setLanguage(com.navfinance.i18n.AppLanguage.ENGLISH);
        exportService = ExportService.getInstance();
    }

    private List<Transaction> createSampleTransactions() {
        List<Transaction> list = new ArrayList<>();

        // 1. Transaction linked to Person Master
        Transaction tx1 = new Transaction();
        tx1.setId(101L);
        tx1.setType(TransactionType.EXPENSE);
        tx1.setTxDate(LocalDate.of(2026, 10, 5));
        tx1.setAmount(5000.0);
        tx1.setCategory("Loan");
        tx1.setSubCategory("Personal Loan");
        tx1.setPersonId(1L);
        tx1.setPersonName("Amit Kumar");
        tx1.setPaymentVia("UPI");
        tx1.setNotes("Lent money for festival shopping");
        list.add(tx1);

        // 2. Transaction linked to Bill Master
        Transaction tx2 = new Transaction();
        tx2.setId(102L);
        tx2.setType(TransactionType.EXPENSE);
        tx2.setTxDate(LocalDate.of(2026, 10, 10));
        tx2.setAmount(2450.0);
        tx2.setCategory("Utilities");
        tx2.setSubCategory("Electricity");
        tx2.setBillId(2L);
        tx2.setBillName("Torrent Power Bill");
        tx2.setPaymentVia("Net Banking");
        tx2.setNotes("Electricity bill for Sep-Oct");
        list.add(tx2);

        // 3. Transaction linked to EMI Master
        Transaction tx3 = new Transaction();
        tx3.setId(103L);
        tx3.setType(TransactionType.EXPENSE);
        tx3.setTxDate(LocalDate.of(2026, 10, 15));
        tx3.setAmount(18500.0);
        tx3.setCategory("Loans & EMI");
        tx3.setSubCategory("Home Loan");
        tx3.setEmiId(3L);
        tx3.setEmiName("HDFC Home Loan EMI");
        tx3.setPaymentVia("Auto-Debit");
        tx3.setNotes("Monthly home loan installment");
        list.add(tx3);

        // 4. Transaction linked to Credit Card Master
        Transaction tx4 = new Transaction();
        tx4.setId(104L);
        tx4.setType(TransactionType.EXPENSE);
        tx4.setTxDate(LocalDate.of(2026, 10, 20));
        tx4.setAmount(6200.0);
        tx4.setCategory("Credit Card Payment");
        tx4.setSubCategory("Monthly Bill");
        tx4.setCreditCardId(4L);
        tx4.setCardName("HDFC Regalia Gold");
        tx4.setPaymentVia("UPI");
        tx4.setNotes("Card statement clearance");
        list.add(tx4);

        // 5. Unlinked Realized Income Transaction
        Transaction tx5 = new Transaction();
        tx5.setId(105L);
        tx5.setType(TransactionType.INCOME);
        tx5.setTxDate(LocalDate.of(2026, 10, 1));
        tx5.setAmount(75000.0);
        tx5.setCategory("Salary");
        tx5.setSubCategory("Monthly Salary");
        tx5.setPaymentVia("NEFT");
        tx5.setNotes("Tech corp October payroll");
        list.add(tx5);

        return list;
    }

    private FilterCriteria createSampleCriteria() {
        FilterCriteria criteria = new FilterCriteria();
        criteria.setStartDate(LocalDate.of(2026, 10, 1));
        criteria.setEndDate(LocalDate.of(2026, 10, 31));
        criteria.setDatePresetName("This Month");
        criteria.setType(TransactionType.EXPENSE);
        criteria.setCategory("Utilities");
        criteria.setSubCategory("Electricity");
        criteria.setPaymentVia("UPI");
        criteria.setPersonName("Amit Kumar");
        criteria.setMinAmount(500.0);
        criteria.setMaxAmount(50000.0);
        criteria.setSearchKeyword("festival");
        return criteria;
    }

    @Test
    void testAssociatedMasterHelperMethods() {
        Transaction txPerson = new Transaction();
        txPerson.setPersonName("Rahul Verma");
        assertEquals("Person", txPerson.getAssociatedMasterType());
        assertEquals("Rahul Verma", txPerson.getAssociatedMasterName());
        assertEquals("Rahul Verma", txPerson.getAssociatedMasterDisplay());

        Transaction txBill = new Transaction();
        txBill.setBillName("Water Supply");
        assertEquals("Bill", txBill.getAssociatedMasterType());
        assertEquals("Water Supply", txBill.getAssociatedMasterName());
        assertEquals("Bill: Water Supply", txBill.getAssociatedMasterDisplay());

        Transaction txEmi = new Transaction();
        txEmi.setEmiName("Car Loan SBI");
        assertEquals("EMI", txEmi.getAssociatedMasterType());
        assertEquals("Car Loan SBI", txEmi.getAssociatedMasterName());
        assertEquals("EMI: Car Loan SBI", txEmi.getAssociatedMasterDisplay());

        Transaction txCard = new Transaction();
        txCard.setCardName("ICICI Sapphiro");
        assertEquals("Card", txCard.getAssociatedMasterType());
        assertEquals("ICICI Sapphiro", txCard.getAssociatedMasterName());
        assertEquals("Card: ICICI Sapphiro", txCard.getAssociatedMasterDisplay());

        Transaction txUnlinked = new Transaction();
        assertEquals("-", txUnlinked.getAssociatedMasterType());
        assertEquals("-", txUnlinked.getAssociatedMasterName());
        assertEquals("-", txUnlinked.getAssociatedMasterDisplay());
    }

    @Test
    void testFilterCriteriaFormattingAndSummary() {
        FilterCriteria criteria = new FilterCriteria();
        assertEquals("All Time", criteria.getFormattedDateRange());
        assertFalse(criteria.hasActiveFilters());

        criteria.setStartDate(LocalDate.of(2026, 10, 1));
        criteria.setEndDate(LocalDate.of(2026, 10, 31));
        criteria.setDatePresetName("This Month");
        assertEquals("01-Oct-2026 to 31-Oct-2026 (This Month)", criteria.getFormattedDateRange());
        assertTrue(criteria.hasActiveFilters());

        criteria.setType(TransactionType.INCOME);
        criteria.setCategory("Salary");
        criteria.setPersonName("Boss");

        List<String[]> summary = criteria.getFilterSummary();
        assertNotNull(summary);
        assertTrue(summary.stream().anyMatch(pair -> pair[0].equals("Date Range") && pair[1].contains("This Month")));
        assertTrue(summary.stream().anyMatch(pair -> pair[0].equals("Type") && pair[1].equals("INCOME")));
        assertTrue(summary.stream().anyMatch(pair -> pair[0].equals("Category") && pair[1].equals("Salary")));
        assertTrue(summary.stream().anyMatch(pair -> pair[0].equals("Associated Person") && pair[1].equals("Boss")));
    }

    @Test
    void testExportToCsvContainsFilterMetadataAndMasterEntities(@TempDir Path tempDir) throws Exception {
        File csvFile = tempDir.resolve("test_export.csv").toFile();
        List<Transaction> transactions = createSampleTransactions();
        FilterCriteria criteria = createSampleCriteria();

        exportService.exportToCsv(transactions, criteria, csvFile);

        assertTrue(csvFile.exists());
        assertTrue(csvFile.length() > 0);

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }

        // Verify filter metadata in CSV comments
        assertTrue(lines.stream().anyMatch(l -> l.contains("# Date Range: 01-Oct-2026 to 31-Oct-2026 (This Month)")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("# Filter - Type: EXPENSE")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("# Filter - Category: Utilities")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("# Filter - Sub-Category: Electricity")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("# Filter - Associated Person: Amit Kumar")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("# Summary: Records=5")));

        // Verify header includes Associated_Master and Master_Type
        assertTrue(lines.stream().anyMatch(l -> l.contains("Associated_Master,Master_Type")));

        // Verify data rows contain respective master entities
        assertTrue(lines.stream().anyMatch(l -> l.contains("Amit Kumar,Person")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Bill: Torrent Power Bill,Bill")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("EMI: HDFC Home Loan EMI,EMI")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("Card: HDFC Regalia Gold,Card")));
    }

    @Test
    void testExportToExcelContainsFilterCriteriaAndMasterColumns(@TempDir Path tempDir) throws Exception {
        File xlsxFile = tempDir.resolve("test_export.xlsx").toFile();
        List<Transaction> transactions = createSampleTransactions();
        FilterCriteria criteria = createSampleCriteria();

        exportService.exportToExcel(transactions, criteria, xlsxFile);

        assertTrue(xlsxFile.exists());
        assertTrue(xlsxFile.length() > 0);

        try (FileInputStream fis = new FileInputStream(xlsxFile);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheet("Ledger Report");
            assertNotNull(sheet);

            // Verify title
            Row titleRow = sheet.getRow(0);
            assertNotNull(titleRow);
            assertTrue(titleRow.getCell(0).getStringCellValue().contains("Nav Finance"));

            // Verify subtitle with database engine
            Row subRow = sheet.getRow(1);
            assertNotNull(subRow);
            assertTrue(subRow.getCell(0).getStringCellValue().contains("Database Engine:"));

            // Check content of entire sheet for filter values and master entities
            boolean foundDateFilter = false;
            boolean foundMasterHeader = false;
            boolean foundPersonMaster = false;
            boolean foundBillMaster = false;
            boolean foundEmiMaster = false;
            boolean foundCardMaster = false;

            for (Row row : sheet) {
                for (org.apache.poi.ss.usermodel.Cell cell : row) {
                    try {
                        String val = cell.getStringCellValue();
                        if (val.contains("01-Oct-2026 to 31-Oct-2026 (This Month)")) foundDateFilter = true;
                        if (val.contains("Associated Person / Master") || val.contains("Master")) foundMasterHeader = true;
                        if (val.equals("Amit Kumar")) foundPersonMaster = true;
                        if (val.contains("Torrent Power Bill")) foundBillMaster = true;
                        if (val.contains("HDFC Home Loan EMI")) foundEmiMaster = true;
                        if (val.contains("HDFC Regalia Gold")) foundCardMaster = true;
                    } catch (Exception ignored) {}
                }
            }

            assertTrue(foundDateFilter, "Excel report should contain formatted date range");
            assertTrue(foundMasterHeader, "Excel report should contain Associated Master header");
            assertTrue(foundPersonMaster, "Excel report should contain Person master entity");
            assertTrue(foundBillMaster, "Excel report should contain Bill master entity");
            assertTrue(foundEmiMaster, "Excel report should contain EMI master entity");
            assertTrue(foundCardMaster, "Excel report should contain Card master entity");
        }
    }

    @Test
    void testExportToPdfGeneratesValidDocumentWithPages(@TempDir Path tempDir) throws Exception {
        File pdfFile = tempDir.resolve("test_export.pdf").toFile();
        List<Transaction> transactions = createSampleTransactions();
        FilterCriteria criteria = createSampleCriteria();

        exportService.exportToPdf(transactions, criteria, pdfFile);

        assertTrue(pdfFile.exists());
        assertTrue(pdfFile.length() > 2000, "PDF should be a substantial binary document");

        // Validate PDF structure and page count using OpenPDF PdfReader
        PdfReader reader = new PdfReader(pdfFile.getAbsolutePath());
        assertNotNull(reader);
        assertTrue(reader.getNumberOfPages() >= 1, "PDF should have at least 1 page");
        reader.close();
    }
}
