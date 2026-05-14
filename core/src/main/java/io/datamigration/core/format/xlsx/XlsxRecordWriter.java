package io.datamigration.core.format.xlsx;

import io.datamigration.core.format.RecordWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Streaming XLSX writer using Apache POI's SXSSF API.
 *
 * <p>Rows beyond an in-memory window of {@value #WINDOW_SIZE} are flushed to a temporary file,
 * keeping memory usage bounded regardless of how many rows are written.
 */
public final class XlsxRecordWriter implements RecordWriter {

    private static final int WINDOW_SIZE = 100;

    private final SXSSFWorkbook workbook;
    private final SXSSFSheet sheet;
    private final OutputStream out;
    private final List<String> headers;
    private int rowIndex;
    private boolean closed;

    public XlsxRecordWriter(OutputStream out, List<String> headers) {
        this.out = out;
        this.workbook = new SXSSFWorkbook(WINDOW_SIZE);
        this.sheet = workbook.createSheet();
        this.headers = List.copyOf(headers);
        writeHeaderRow();
    }

    private void writeHeaderRow() {
        Row header = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.size(); i++) {
            header.createCell(i).setCellValue(headers.get(i));
        }
    }

    @Override
    public void write(Map<String, Object> record) {
        Row row = sheet.createRow(rowIndex++);
        for (int i = 0; i < headers.size(); i++) {
            Object value = record.get(headers.get(i));
            Cell cell = row.createCell(i);
            if (value == null) {
                cell.setBlank();
            } else if (value instanceof Number n) {
                cell.setCellValue(n.doubleValue());
            } else if (value instanceof Boolean b) {
                cell.setCellValue(b);
            } else {
                cell.setCellValue(value.toString());
            }
        }
    }

    @Override
    public void flush() {
        // SXSSF flushes rows automatically when they fall outside the in-memory window.
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            workbook.write(out);
        } finally {
            workbook.close();
            out.close();
        }
    }
}
