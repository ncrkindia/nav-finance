package com.navfinance.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.navfinance.config.AppConfig;
import com.navfinance.database.DatabaseManager;
import com.navfinance.i18n.I18n;
import com.navfinance.model.FilterCriteria;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
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
 * Employs styled header bands, metric summary KPI tables, active filter criteria scopes,
 * associated master details (Person, Bill, EMI, Card), alternating rows,
 * currency formatting, and dynamic multi-page footers.
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
     * Exports a filtered list of ledger transactions to a RFC 4180 compliant CSV document,
     * including active filter metadata comments and associated master details.
     *
     * @param transactions List of transactions to export
     * @param criteria Filter criteria applied to generate this report
     * @param file Output target file
     * @throws Exception on file I/O errors
     */
    public void exportToCsv(List<Transaction> transactions, FilterCriteria criteria, File file) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, false))) {
            // Write report metadata comment block
            writer.println("# ==============================================================================");
            writer.println("# " + AppConfig.APP_NAME + " - " + I18n.get("report.title"));
            writer.println("# Generated On: " + LocalDateTime.now().format(AppConfig.DATE_TIME_FORMATTER));
            writer.println("# Database Engine: " + AppConfig.getEffectiveDbType().getDisplayName());

            if (criteria != null) {
                writer.println("# Date Range: " + criteria.getFormattedDateRange());
                for (String[] f : criteria.getFilterSummary()) {
                    if (!f[0].equalsIgnoreCase("Date Range")) {
                        writer.println("# Filter - " + f[0] + ": " + f[1]);
                    }
                }
            } else {
                writer.println("# Scope: " + I18n.get("report.filter.allRecords"));
            }

            double totalIn = 0;
            double totalOut = 0;
            for (Transaction tx : transactions) {
                if (tx.getType() == TransactionType.INCOME) totalIn += tx.getAmount();
                else totalOut += tx.getAmount();
            }
            writer.println(String.format("# Summary: Records=%d | Total Inflow=%s | Total Outflow=%s | Net Balance=%s",
                    transactions.size(),
                    AppConfig.formatCurrency(totalIn),
                    AppConfig.formatCurrency(totalOut),
                    AppConfig.formatCurrency(totalIn - totalOut)));
            writer.println("# ==============================================================================");

            // Header columns
            writer.println("ID,Type,Date,Status,Amount,Payment_Via,Category,Sub_Category,Associated_Master,Master_Type,Notes,Created_At");

            for (Transaction tx : transactions) {
                StringBuilder sb = new StringBuilder();
                sb.append(tx.getId()).append(",");
                sb.append(escapeCsv(tx.getType().name())).append(",");
                sb.append(escapeCsv(tx.getTxDate() != null ? tx.getTxDate().toString() : "")).append(",");
                sb.append(tx.isExpected() ? "Expected" : "Settled").append(",");
                sb.append(tx.getAmount()).append(",");
                sb.append(escapeCsv(tx.getPaymentVia())).append(",");
                sb.append(escapeCsv(tx.getCategory())).append(",");
                sb.append(escapeCsv(tx.getSubCategory())).append(",");
                sb.append(escapeCsv(tx.getAssociatedMasterDisplay())).append(",");
                sb.append(escapeCsv(tx.getAssociatedMasterType())).append(",");
                sb.append(escapeCsv(tx.getNotes())).append(",");
                sb.append(escapeCsv(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : ""));
                writer.println(sb.toString());
            }
        }
    }

    /**
     * Backward-compatible overload for exporting transactions to CSV without explicit criteria.
     *
     * @param transactions List of transactions to export
     * @param file Output target file
     * @throws Exception on file I/O errors
     */
    public void exportToCsv(List<Transaction> transactions, File file) throws Exception {
        exportToCsv(transactions, null, file);
    }

    /**
     * Exports a filtered transaction list to a professionally designed Excel workbook (.xlsx).
     * Includes color-coded headers, formatted currency cells, filter criteria metadata card,
     * associated master column, auto-filtering, and aggregate totals.
     *
     * @param transactions List of transactions to export
     * @param criteria Filter criteria applied to generate this report
     * @param file Target Excel workbook output file
     * @throws Exception on workbook generation or write error
     */
    public void exportToExcel(List<Transaction> transactions, FilterCriteria criteria, File file) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ledger Report");
            sheet.setDisplayGridlines(true);

            // Title Style (16pt Bold Navy)
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);

            // Subtitle Style (9.5pt Italic Slate)
            CellStyle subStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font subFont = workbook.createFont();
            subFont.setItalic(true);
            subFont.setFontHeightInPoints((short) 9);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subStyle.setFont(subFont);

            // Filter Section Header Style
            CellStyle filterHeaderStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font filterHeaderFont = workbook.createFont();
            filterHeaderFont.setBold(true);
            filterHeaderFont.setFontHeightInPoints((short) 10);
            filterHeaderFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            filterHeaderStyle.setFont(filterHeaderFont);
            filterHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            filterHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            filterHeaderStyle.setBorderBottom(BorderStyle.THIN);
            filterHeaderStyle.setBorderTop(BorderStyle.THIN);

            // Filter Key Style
            CellStyle filterKeyStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font filterKeyFont = workbook.createFont();
            filterKeyFont.setBold(true);
            filterKeyFont.setFontHeightInPoints((short) 9);
            filterKeyStyle.setFont(filterKeyFont);
            filterKeyStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Filter Value Style
            CellStyle filterValStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font filterValFont = workbook.createFont();
            filterValFont.setFontHeightInPoints((short) 9);
            filterValStyle.setFont(filterValFont);

            // Table Header Row Style (Deep Navy, White text, centered)
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
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

            // Center aligned cell style
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Number/Currency style
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            // Total summary style
            CellStyle totalStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);
            totalStyle.setBorderTop(BorderStyle.THIN);
            totalStyle.setBorderBottom(BorderStyle.DOUBLE);
            totalStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            // Alternating Row Style (Zebra)
            CellStyle altStyle = workbook.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());

            int rowIdx = 0;

            // 1. Report Title Row
            Row rTitle = sheet.createRow(rowIdx++);
            rTitle.createCell(0).setCellValue(AppConfig.APP_NAME + " - " + I18n.get("report.title"));
            rTitle.getCell(0).setCellStyle(titleStyle);

            // 2. Subtitle Row with Timestamp and Database Engine
            Row rSub = sheet.createRow(rowIdx++);
            rSub.createCell(0).setCellValue(I18n.get("report.generatedOn", LocalDateTime.now().format(AppConfig.DATE_TIME_FORMATTER))
                    + "  |  Database Engine: " + AppConfig.getEffectiveDbType().getDisplayName());
            rSub.getCell(0).setCellStyle(subStyle);

            rowIdx++; // Spacer

            // 3. Filter Scope & Criteria Block
            Row rFiltHeader = sheet.createRow(rowIdx++);
            Cell cFiltTitle = rFiltHeader.createCell(0);
            cFiltTitle.setCellValue(I18n.get("report.filter.title").toUpperCase());
            cFiltTitle.setCellStyle(filterHeaderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 7));

            List<String[]> filterSummary = (criteria != null) ? criteria.getFilterSummary() : null;
            if (filterSummary == null || filterSummary.isEmpty()) {
                Row rF = sheet.createRow(rowIdx++);
                rF.createCell(0).setCellValue(I18n.get("report.filter.allRecords"));
                rF.getCell(0).setCellStyle(filterValStyle);
            } else {
                for (int i = 0; i < filterSummary.size(); i += 2) {
                    Row rF = sheet.createRow(rowIdx++);
                    String[] f1 = filterSummary.get(i);
                    Cell k1 = rF.createCell(0);
                    k1.setCellValue(f1[0] + ":");
                    k1.setCellStyle(filterKeyStyle);
                    Cell v1 = rF.createCell(1);
                    v1.setCellValue(f1[1]);
                    v1.setCellStyle(filterValStyle);

                    if (i + 1 < filterSummary.size()) {
                        String[] f2 = filterSummary.get(i + 1);
                        Cell k2 = rF.createCell(3);
                        k2.setCellValue(f2[0] + ":");
                        k2.setCellStyle(filterKeyStyle);
                        Cell v2 = rF.createCell(4);
                        v2.setCellValue(f2[1]);
                        v2.setCellStyle(filterValStyle);
                    }
                }
            }

            rowIdx++; // Spacer

            // 4. Summary KPI Aggregate Block
            double totalIn = 0;
            double totalOut = 0;
            for (Transaction t : transactions) {
                if (t.getType() == TransactionType.INCOME) totalIn += t.getAmount();
                else totalOut += t.getAmount();
            }

            Row rSum0 = sheet.createRow(rowIdx++);
            rSum0.createCell(0).setCellValue(I18n.get("report.totalRecords", transactions.size()) + ":");
            rSum0.getCell(0).setCellStyle(filterKeyStyle);
            rSum0.createCell(1).setCellValue(transactions.size());
            rSum0.getCell(1).setCellStyle(filterValStyle);

            Row rSum1 = sheet.createRow(rowIdx++);
            rSum1.createCell(0).setCellValue(I18n.get("dashboard.kpi.totalIncome") + ":");
            rSum1.getCell(0).setCellStyle(filterKeyStyle);
            Cell cIn = rSum1.createCell(1);
            cIn.setCellValue(totalIn);
            cIn.setCellStyle(incomeStyle);

            Row rSum2 = sheet.createRow(rowIdx++);
            rSum2.createCell(0).setCellValue(I18n.get("dashboard.kpi.totalExpense") + ":");
            rSum2.getCell(0).setCellStyle(filterKeyStyle);
            Cell cOut = rSum2.createCell(1);
            cOut.setCellValue(totalOut);
            cOut.setCellStyle(expenseStyle);

            Row rSum3 = sheet.createRow(rowIdx++);
            rSum3.createCell(0).setCellValue(I18n.get("dashboard.kpi.netBalance") + ":");
            rSum3.getCell(0).setCellStyle(filterKeyStyle);
            Cell cBal = rSum3.createCell(1);
            cBal.setCellValue(totalIn - totalOut);
            cBal.setCellStyle(totalStyle);

            rowIdx++; // Spacer

            // 5. Table Header Row (11 Columns)
            String[] headers = {
                    "#",
                    I18n.get("transactions.table.type"),
                    I18n.get("transactions.table.date"),
                    I18n.get("transactions.table.status"),
                    I18n.get("transactions.table.amount") + " (" + AppConfig.getCurrencySymbol() + ")",
                    I18n.get("transactions.table.via"),
                    I18n.get("transactions.table.category"),
                    I18n.get("transactions.table.subCategory"),
                    I18n.get("report.column.master"),
                    I18n.get("report.column.masterType"),
                    I18n.get("transactions.table.notes")
            };
            int tableHeaderRowIdx = rowIdx;
            Row hRow = sheet.createRow(rowIdx++);
            hRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // 6. Data Rows
            int firstDataRowIdx = rowIdx;
            int seq = 1;
            for (Transaction tx : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(seq++);
                row.getCell(0).setCellStyle(centerStyle);

                Cell cType = row.createCell(1);
                cType.setCellValue(tx.getType().getDisplayName());
                cType.setCellStyle(tx.getType() == TransactionType.INCOME ? incomeStyle : expenseStyle);

                Cell cDate = row.createCell(2);
                cDate.setCellValue(tx.getTxDate() != null ? tx.getTxDate().format(AppConfig.DATE_FORMATTER) : "-");
                cDate.setCellStyle(centerStyle);

                Cell cStatus = row.createCell(3);
                cStatus.setCellValue(tx.isExpected() ? "Expected" : "Settled");
                cStatus.setCellStyle(centerStyle);

                Cell cAmt = row.createCell(4);
                cAmt.setCellValue(tx.getAmount());
                cAmt.setCellStyle(numberStyle);

                Cell cVia = row.createCell(5);
                cVia.setCellValue(tx.getPaymentVia() != null ? tx.getPaymentVia() : "-");
                cVia.setCellStyle(centerStyle);

                row.createCell(6).setCellValue(tx.getCategory() != null ? tx.getCategory() : "-");
                row.createCell(7).setCellValue(tx.getSubCategory() != null ? tx.getSubCategory() : "-");

                // Associated Master Details
                row.createCell(8).setCellValue(tx.getAssociatedMasterDisplay());
                Cell cMasterType = row.createCell(9);
                cMasterType.setCellValue(tx.getAssociatedMasterType());
                cMasterType.setCellStyle(centerStyle);

                row.createCell(10).setCellValue(tx.getNotes() != null ? tx.getNotes() : "");
            }
            int lastDataRowIdx = rowIdx - 1;

            // 7. Table Summary / Total Row
            if (lastDataRowIdx >= firstDataRowIdx) {
                Row totalRow = sheet.createRow(rowIdx++);
                Cell cTotLbl = totalRow.createCell(3);
                cTotLbl.setCellValue("Total:");
                cTotLbl.setCellStyle(filterKeyStyle);

                Cell cTotVal = totalRow.createCell(4);
                cTotVal.setCellFormula(String.format("SUM(E%d:E%d)", firstDataRowIdx + 1, lastDataRowIdx + 1));
                cTotVal.setCellStyle(totalStyle);

                // Enable Excel Auto-Filter on table header
                sheet.setAutoFilter(new CellRangeAddress(tableHeaderRowIdx, lastDataRowIdx, 0, headers.length - 1));
            }

            // 8. Auto-size columns with safety padding
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(currentWidth + 1200, 16000));
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * Exports a filtered transaction list to an elegant, publication-quality vector PDF report.
     * Features branded header, active filter criteria card, executive KPI cards,
     * landscape data table with associated master column, alternating row striping,
     * and dynamic page numbering ("Page X of Y") via PDF events.
     *
     * @param transactions List of transactions to export
     * @param criteria Filter criteria applied
     * @param file Target PDF output file
     * @throws Exception on PDF generation error
     */
    public void exportToPdf(List<Transaction> transactions, FilterCriteria criteria, File file) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 26, 30);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            PdfWriter writer = PdfWriter.getInstance(document, fos);
            writer.setPageEvent(new PdfReportPageEvent());
            document.open();

            // Fonts
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, new Color(24, 43, 73));
            Font subBrandFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, new Color(100, 116, 139));
            Font metaHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, new Color(100, 116, 139));
            Font metaValFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, new Color(24, 43, 73));
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(51, 65, 85));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font incomeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(16, 124, 65));
            Font expenseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(216, 59, 1));

            // 1. Header Banner Table
            PdfPTable headerBanner = new PdfPTable(2);
            headerBanner.setWidthPercentage(100);
            headerBanner.setWidths(new float[]{6.0f, 4.0f});
            headerBanner.setSpacingAfter(8);

            // Left: Branding Title and Subtitle
            PdfPCell leftBrand = new PdfPCell();
            leftBrand.setBorder(Rectangle.NO_BORDER);
            Paragraph pBrand = new Paragraph(AppConfig.APP_NAME + " - " + I18n.get("report.title"), brandFont);
            Paragraph pSubBrand = new Paragraph(AppConfig.APP_SUBTITLE, subBrandFont);
            leftBrand.addElement(pBrand);
            leftBrand.addElement(pSubBrand);
            headerBanner.addCell(leftBrand);

            // Right: Generated Timestamp and Database Engine
            PdfPCell rightMeta = new PdfPCell();
            rightMeta.setBorder(Rectangle.NO_BORDER);
            rightMeta.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph pGenLbl = new Paragraph(I18n.get("report.generatedOn", LocalDateTime.now().format(AppConfig.DATE_TIME_FORMATTER)), metaValFont);
            pGenLbl.setAlignment(Element.ALIGN_RIGHT);
            Paragraph pDbLbl = new Paragraph("Engine: " + AppConfig.getEffectiveDbType().getDisplayName(), metaHeaderFont);
            pDbLbl.setAlignment(Element.ALIGN_RIGHT);

            rightMeta.addElement(pGenLbl);
            rightMeta.addElement(pDbLbl);
            headerBanner.addCell(rightMeta);

            document.add(headerBanner);

            // 2. Filter Scope & Criteria Card
            PdfPTable filterCard = new PdfPTable(1);
            filterCard.setWidthPercentage(100);
            filterCard.setSpacingAfter(10);

            PdfPCell filterCell = new PdfPCell();
            filterCell.setBackgroundColor(new Color(248, 250, 252));
            filterCell.setBorderColor(new Color(203, 213, 225));
            filterCell.setBorderWidth(0.75f);
            filterCell.setPadding(7);

            Paragraph filterTitle = new Paragraph(I18n.get("report.filter.title").toUpperCase(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(51, 65, 85)));
            filterTitle.setSpacingAfter(4);
            filterCell.addElement(filterTitle);

            List<String[]> filterSummary = (criteria != null) ? criteria.getFilterSummary() : null;
            if (filterSummary == null || filterSummary.isEmpty()) {
                Paragraph noFilter = new Paragraph(I18n.get("report.filter.allRecords"),
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.DARK_GRAY));
                filterCell.addElement(noFilter);
            } else {
                PdfPTable filterGrid = new PdfPTable(4);
                filterGrid.setWidthPercentage(100);
                filterGrid.setWidths(new float[]{1.5f, 3.5f, 1.5f, 3.5f});

                for (int i = 0; i < filterSummary.size(); i += 2) {
                    String[] f1 = filterSummary.get(i);
                    addFilterTagCell(filterGrid, f1[0] + ":", f1[1]);

                    if (i + 1 < filterSummary.size()) {
                        String[] f2 = filterSummary.get(i + 1);
                        addFilterTagCell(filterGrid, f2[0] + ":", f2[1]);
                    } else {
                        addFilterTagCell(filterGrid, "", "");
                    }
                }
                filterCell.addElement(filterGrid);
            }
            filterCard.addCell(filterCell);
            document.add(filterCard);

            // 3. Executive KPI Summary Cards
            double totalIn = 0;
            double totalOut = 0;
            for (Transaction t : transactions) {
                if (t.getType() == TransactionType.INCOME) totalIn += t.getAmount();
                else totalOut += t.getAmount();
            }

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(10);

            addSummaryCard(summaryTable, I18n.get("report.totalRecords", transactions.size()),
                    String.valueOf(transactions.size()), new Color(241, 245, 249), new Color(203, 213, 225), new Color(15, 23, 42));
            addSummaryCard(summaryTable, I18n.get("dashboard.kpi.totalIncome"),
                    AppConfig.formatCurrency(totalIn), new Color(236, 253, 245), new Color(167, 243, 208), new Color(16, 124, 65));
            addSummaryCard(summaryTable, I18n.get("dashboard.kpi.totalExpense"),
                    AppConfig.formatCurrency(totalOut), new Color(254, 242, 242), new Color(254, 202, 202), new Color(220, 38, 38));

            Color balBg = (totalIn >= totalOut) ? new Color(239, 246, 255) : new Color(254, 243, 199);
            Color balBorder = (totalIn >= totalOut) ? new Color(191, 219, 254) : new Color(253, 230, 138);
            Color balFg = (totalIn >= totalOut) ? new Color(29, 78, 216) : new Color(180, 83, 9);
            addSummaryCard(summaryTable, I18n.get("dashboard.kpi.netBalance"),
                    AppConfig.formatCurrency(totalIn - totalOut), balBg, balBorder, balFg);

            document.add(summaryTable);

            // 4. Data Table (10 Columns, landscape)
            PdfPTable table = new PdfPTable(10);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.9f, 1.6f, 1.8f, 1.4f, 2.2f, 1.8f, 2.2f, 2.2f, 2.7f, 3.2f});
            table.setHeaderRows(1); // Repeats headers automatically across multi-page tables

            String[] tableHeaders = {
                    "#",
                    I18n.get("transactions.table.type"),
                    I18n.get("transactions.table.date"),
                    I18n.get("transactions.table.status"),
                    I18n.get("transactions.table.amount") + " (" + AppConfig.getCurrencySymbol() + ")",
                    I18n.get("transactions.table.via"),
                    I18n.get("transactions.table.category"),
                    I18n.get("transactions.table.subCategory"),
                    I18n.get("report.column.master"),
                    I18n.get("transactions.table.notes")
            };

            for (String h : tableHeaders) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(new Color(30, 41, 59));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                cell.setBorderColor(new Color(51, 65, 85));
                table.addCell(cell);
            }

            int seq = 1;
            boolean alt = false;
            Color altColor = new Color(248, 250, 252);

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

                // Associated Master Entity Column (Person, Bill, EMI, Card)
                addCell(table, tx.getAssociatedMasterDisplay(), regularFont, Element.ALIGN_LEFT, rowBg);

                addCell(table, tx.getNotes() != null ? tx.getNotes() : "", regularFont, Element.ALIGN_LEFT, rowBg);
            }

            document.add(table);
            document.close();
        }
    }

    private void addFilterTagCell(PdfPTable table, String label, String value) {
        Font lblFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, new Color(71, 85, 105));
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, new Color(15, 23, 42));

        PdfPCell cLbl = new PdfPCell(new Phrase(label, lblFont));
        cLbl.setBorder(Rectangle.NO_BORDER);
        cLbl.setPadding(1.5f);
        cLbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cLbl);

        PdfPCell cVal = new PdfPCell(new Phrase(value, valFont));
        cVal.setBorder(Rectangle.NO_BORDER);
        cVal.setPadding(1.5f);
        cVal.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cVal);
    }

    private void addSummaryCard(PdfPTable table, String label, String value, Color bg, Color border, Color textCol) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(border);
        cell.setBorderWidth(0.75f);
        cell.setPadding(6);

        Paragraph pLabel = new Paragraph(label.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(100, 116, 139)));
        pLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLabel);

        Paragraph pValue = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, textCol));
        pValue.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pValue);

        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4.5f);
        cell.setBackgroundColor(bg);
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setBorderWidth(0.5f);
        table.addCell(cell);
    }

    /**
     * Exports all database tables into individual timestamped CSV files.
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

    /**
     * OpenPDF page event listener providing per-page headers, dynamic page numbering ("Page X of Y"),
     * and security/engine footers on every page of the generated PDF report.
     */
    private static class PdfReportPageEvent extends PdfPageEventHelper {
        private PdfTemplate totalPagesTemplate;
        private BaseFont baseFont;
        private int maxPage = 1;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
                totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
            } catch (Exception ignored) {}
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            maxPage = Math.max(maxPage, writer.getPageNumber());
            PdfContentByte cb = writer.getDirectContent();
            float left = document.left();
            float right = document.right();
            float bottom = document.bottom() - 15;

            // Footer separator line
            cb.setColorStroke(new Color(226, 232, 240));
            cb.setLineWidth(0.5f);
            cb.moveTo(left, bottom + 11);
            cb.lineTo(right, bottom + 11);
            cb.stroke();

            // Footer text left
            cb.beginText();
            cb.setFontAndSize(baseFont, 7.5f);
            cb.setColorFill(new Color(100, 116, 139));
            cb.setTextMatrix(left, bottom);
            cb.showText(AppConfig.APP_NAME + " - " + AppConfig.APP_SUBTITLE + " | Engine: " + AppConfig.getEffectiveDbType().getDisplayName());

            // Page number right: "Page X of "
            String pageText = "Page " + writer.getPageNumber() + " of ";
            float textWidth = baseFont.getWidthPoint(pageText, 7.5f);
            cb.setTextMatrix(right - textWidth - 14, bottom);
            cb.showText(pageText);
            cb.endText();

            // Dynamic total page count template
            cb.addTemplate(totalPagesTemplate, right - 14, bottom);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPagesTemplate != null && baseFont != null) {
                totalPagesTemplate.beginText();
                totalPagesTemplate.setFontAndSize(baseFont, 7.5f);
                totalPagesTemplate.setColorFill(new Color(100, 116, 139));
                totalPagesTemplate.setTextMatrix(0, 0);
                totalPagesTemplate.showText(String.valueOf(maxPage));
                totalPagesTemplate.endText();
            }
        }
    }
}
