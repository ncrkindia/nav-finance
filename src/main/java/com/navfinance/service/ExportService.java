package com.navfinance.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.navfinance.config.AppConfig;
import com.navfinance.database.DatabaseManager;
import com.navfinance.model.FilterCriteria;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for generating publication-ready financial exports in CSV, Microsoft Excel (.xlsx),
 * and vector PDF report formats.
 * <p>
 * Employs styled header bands, metric summary KPI tables, alternating rows,
 * currency formatting, and metadata watermarks.
 * </p>
 */
public class ExportService {

    private static ExportService instance;

    /**
     * Private constructor for singleton pattern.
     */
    private ExportService() {}

    /**
     * Returns the singleton instance of {@code ExportService}.
     *
     * @return singleton service instance
     */
    public static synchronized ExportService getInstance() {
        if (instance == null) {
            instance = new ExportService();
        }
        return instance;
    }

    /**
     * Exports a filtered list of ledger transactions to a RFC 4180 compliant CSV document.
     *
     * @param transactions List of transactions to export
     * @param file Output target file
     * @throws Exception on file I/O errors
     */
    public void exportToCsv(List<Transaction> transactions, File file) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, false))) {
            // Header
            writer.println("ID,Type,Date,Planned/Expected,Amount,Via,Category,Sub-Category,Person,Notes,Created_At");

            for (Transaction tx : transactions) {
                StringBuilder sb = new StringBuilder();
                sb.append(tx.getId()).append(",");
                sb.append(escapeCsv(tx.getType().name())).append(",");
                sb.append(escapeCsv(tx.getTxDate() != null ? tx.getTxDate().toString() : "")).append(",");
                sb.append(tx.isExpected() ? "Expected" : "Actual").append(",");
                sb.append(tx.getAmount()).append(",");
                sb.append(escapeCsv(tx.getPaymentVia())).append(",");
                sb.append(escapeCsv(tx.getCategory())).append(",");
                sb.append(escapeCsv(tx.getSubCategory())).append(",");
                sb.append(escapeCsv(tx.getPersonName())).append(",");
                sb.append(escapeCsv(tx.getNotes())).append(",");
                sb.append(escapeCsv(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : ""));
                writer.println(sb.toString());
            }
        }
    }

    /**
     * Exports a filtered transaction list to a professionally designed Excel workbook (.xlsx).
     * Includes color-coded headers, formatted currency cells, filter criteria metadata,
     * and double-underlined summary aggregate totals.
     *
     * @param transactions List of transactions to export
     * @param criteria Filter criteria applied to generate this report
     * @param file Target Excel workbook output file
     * @throws Exception on workbook generation or write error
     */
    public void exportToExcel(List<Transaction> transactions, FilterCriteria criteria, File file) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions Report");
            sheet.setDisplayGridlines(true);

            // Title Style
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);

            // Subtitle Style
            CellStyle subStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font subFont = workbook.createFont();
            subFont.setItalic(true);
            subFont.setFontHeightInPoints((short) 10);
            subStyle.setFont(subFont);

            // Header Row Style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);

            // Income cell style
            CellStyle incomeStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font inFont = workbook.createFont();
            inFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            inFont.setBold(true);
            incomeStyle.setFont(inFont);

            // Expense cell style
            CellStyle expenseStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font exFont = workbook.createFont();
            exFont.setColor(IndexedColors.RED.getIndex());
            exFont.setBold(true);
            expenseStyle.setFont(exFont);

            // Number/Currency style
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            // Total summary style
            CellStyle totalStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            totalStyle.setBorderTop(BorderStyle.DOUBLE);
            totalStyle.setBorderBottom(BorderStyle.DOUBLE);
            totalStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            int rowIdx = 0;

            // Title Row
            Row rTitle = sheet.createRow(rowIdx++);
            rTitle.createCell(0).setCellValue(AppConfig.APP_NAME + " - Financial Statement");
            rTitle.getCell(0).setCellStyle(titleStyle);

            // Subtitle Row
            Row rSub = sheet.createRow(rowIdx++);
            rSub.createCell(0).setCellValue("Generated on: " + LocalDateTime.now().format(AppConfig.DATE_TIME_FORMATTER));
            rSub.getCell(0).setCellStyle(subStyle);

            rowIdx++; // Empty spacer row

            // Summary metrics block
            double totalIn = 0;
            double totalOut = 0;
            for (Transaction t : transactions) {
                if (t.getType() == TransactionType.INCOME) totalIn += t.getAmount();
                else totalOut += t.getAmount();
            }

            Row rSum1 = sheet.createRow(rowIdx++);
            rSum1.createCell(0).setCellValue("Total Income (In):");
            rSum1.createCell(1).setCellValue(totalIn);
            rSum1.getCell(1).setCellStyle(incomeStyle);

            Row rSum2 = sheet.createRow(rowIdx++);
            rSum2.createCell(0).setCellValue("Total Expense (Out):");
            rSum2.createCell(1).setCellValue(totalOut);
            rSum2.getCell(1).setCellStyle(expenseStyle);

            Row rSum3 = sheet.createRow(rowIdx++);
            rSum3.createCell(0).setCellValue("Net Savings / Balance:");
            rSum3.createCell(1).setCellValue(totalIn - totalOut);
            rSum3.getCell(1).setCellStyle(totalStyle);

            rowIdx++; // Empty spacer row

            // Table Header Row
            String[] headers = {"#", "Type", "Date", "Status", "Amount (" + AppConfig.getCurrencySymbol() + ")", "Via", "Category", "Sub-Category", "Person", "Notes"};
            Row hRow = sheet.createRow(rowIdx++);
            hRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Data Rows
            int seq = 1;
            for (Transaction tx : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(seq++);

                org.apache.poi.ss.usermodel.Cell cType = row.createCell(1);
                cType.setCellValue(tx.getType().getDisplayName());
                cType.setCellStyle(tx.getType() == TransactionType.INCOME ? incomeStyle : expenseStyle);

                row.createCell(2).setCellValue(tx.getTxDate() != null ? tx.getTxDate().format(AppConfig.DATE_FORMATTER) : "");
                row.createCell(3).setCellValue(tx.isExpected() ? "Expected" : "Settled");

                org.apache.poi.ss.usermodel.Cell cAmt = row.createCell(4);
                cAmt.setCellValue(tx.getAmount());
                cAmt.setCellStyle(numberStyle);

                row.createCell(5).setCellValue(tx.getPaymentVia() != null ? tx.getPaymentVia() : "-");
                row.createCell(6).setCellValue(tx.getCategory() != null ? tx.getCategory() : "-");
                row.createCell(7).setCellValue(tx.getSubCategory() != null ? tx.getSubCategory() : "-");
                row.createCell(8).setCellValue(tx.getPersonName() != null ? tx.getPersonName() : "-");
                row.createCell(9).setCellValue(tx.getNotes() != null ? tx.getNotes() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(currentWidth + 1200, 15000));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Exports a filtered transaction list to an elegant, publication-quality PDF document.
     * Includes metric cards (Total Records, Income, Expense, Net Balance) and landscape table.
     *
     * @param transactions List of transactions to export
     * @param criteria Filter criteria applied
     * @param file Target PDF output file
     * @throws Exception on PDF generation error
     */
    public void exportToPdf(List<Transaction> transactions, FilterCriteria criteria, File file) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 25, 25, 30, 30);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter.getInstance(document, fos);
            document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(24, 43, 73));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
        Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font incomeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(16, 124, 65));
        Font expenseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(216, 59, 1));

        // Header Section
        Paragraph title = new Paragraph(AppConfig.APP_NAME + " - Personal Finance Report", titleFont);
        title.setAlignment(Element.ALIGN_LEFT);
        document.add(title);

        Paragraph subtitle = new Paragraph("Generated on: " + LocalDateTime.now().format(AppConfig.DATE_TIME_FORMATTER) +
                " | Target User Database: " + AppConfig.APP_DATA_DIR.getAbsolutePath(), subtitleFont);
        subtitle.setSpacingAfter(15);
        document.add(subtitle);

        // Metric Summary Table (In, Out, Balance)
        double totalIn = 0;
        double totalOut = 0;
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.INCOME) totalIn += t.getAmount();
            else totalOut += t.getAmount();
        }

        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(15);

        addSummaryCard(summaryTable, "Total Records", String.valueOf(transactions.size()), new Color(240, 243, 246), Color.BLACK);
        addSummaryCard(summaryTable, "Total Income (In)", AppConfig.formatCurrency(totalIn), new Color(230, 245, 235), new Color(16, 124, 65));
        addSummaryCard(summaryTable, "Total Expense (Out)", AppConfig.formatCurrency(totalOut), new Color(254, 237, 237), new Color(216, 59, 1));
        Color balBg = (totalIn >= totalOut) ? new Color(228, 242, 253) : new Color(255, 243, 205);
        Color balFg = (totalIn >= totalOut) ? new Color(15, 76, 129) : new Color(133, 100, 4);
        addSummaryCard(summaryTable, "Net Balance", AppConfig.formatCurrency(totalIn - totalOut), balBg, balFg);
        document.add(summaryTable);

        // Transactions Table
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.8f, 2.0f, 1.4f, 2.4f, 2.0f, 2.6f, 2.6f, 3.5f});

        String[] headers = {"#", "Type", "Date", "Status", "Amount", "Via", "Category", "Sub-Category", "Notes"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
            cell.setBackgroundColor(new Color(36, 53, 76));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6);
            table.addCell(cell);
        }

        int seq = 1;
        boolean alt = false;
        Color altColor = new Color(248, 249, 250);

        for (Transaction tx : transactions) {
            Color rowBg = alt ? altColor : Color.WHITE;
            alt = !alt;

            addCell(table, String.valueOf(seq++), regularFont, Element.ALIGN_CENTER, rowBg);

            boolean isIncome = tx.getType() == TransactionType.INCOME;
            addCell(table, tx.getType().getDisplayName(), isIncome ? incomeFont : expenseFont, Element.ALIGN_CENTER, rowBg);

            addCell(table, tx.getTxDate() != null ? tx.getTxDate().format(AppConfig.DATE_FORMATTER) : "-", regularFont, Element.ALIGN_CENTER, rowBg);
            addCell(table, tx.isExpected() ? "Expected" : "Settled", regularFont, Element.ALIGN_CENTER, rowBg);
            addCell(table, AppConfig.formatCurrency(tx.getAmount()), isIncome ? incomeFont : expenseFont, Element.ALIGN_RIGHT, rowBg);
            addCell(table, tx.getPaymentVia() != null ? tx.getPaymentVia() : "-", regularFont, Element.ALIGN_CENTER, rowBg);
            addCell(table, tx.getCategory() != null ? tx.getCategory() : "-", regularFont, Element.ALIGN_LEFT, rowBg);
            addCell(table, tx.getSubCategory() != null ? tx.getSubCategory() : "-", regularFont, Element.ALIGN_LEFT, rowBg);
            addCell(table, tx.getNotes() != null ? tx.getNotes() : "", regularFont, Element.ALIGN_LEFT, rowBg);
        }

        document.add(table);

        // Document Footer
        Paragraph footer = new Paragraph("Nav Finance Personal Finance Manager | Offline Local H2 Database Secure Storage", subtitleFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(12);
        document.add(footer);

        document.close();
        }
    }

    private void addSummaryCard(PdfPTable table, String label, String value, Color bg, Color textCol) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setPadding(8);
        cell.setBorder(Rectangle.NO_BORDER);

        Paragraph pLabel = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
        pLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLabel);

        Paragraph pValue = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, textCol));
        pValue.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pValue);

        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBackgroundColor(bg);
        table.addCell(cell);
    }

    /**
     * Exports all database tables (transactions, persons, bills, emis, credit cards, payment modes,
     * audit logs, recurring schedules, due payments) into individual timestamped CSV files.
     *
     * @param targetDir Destination directory where CSV table dumps are saved
     * @throws Exception on database or file I/O error
     */
    public void exportAllDatabaseToCsv(File targetDir) throws Exception {
        DatabaseManager.getInstance().exportAllTablesToCsv(targetDir);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
