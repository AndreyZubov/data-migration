package io.datamigration.core.format.xlsx;

import io.datamigration.core.format.RecordReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.Styles;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * Streaming XLSX reader based on Apache POI's event (SAX) API. The reader reads only the first
 * sheet; the first row is treated as headers.
 *
 * <p>Parsing runs on a dedicated daemon thread and pushes rows into a bounded queue, which provides
 * backpressure to the SAX parser when the consumer is slow.
 */
public final class XlsxRecordReader implements RecordReader {

    private static final int QUEUE_CAPACITY = 1024;
    private static final Map<String, Object> END_OF_STREAM = Map.of();

    private final BlockingQueue<Map<String, Object>> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    private final OPCPackage pkg;
    private final Thread worker;
    private volatile IOException workerError;
    private Map<String, Object> next;

    public XlsxRecordReader(InputStream in) throws IOException {
        try {
            this.pkg = OPCPackage.open(in);
        } catch (Exception e) {
            throw new IOException("Failed to open XLSX package", e);
        }
        this.worker = new Thread(this::parse, "xlsx-reader");
        this.worker.setDaemon(true);
        this.worker.start();
        advance();
    }

    private void parse() {
        try {
            XSSFReader reader = new XSSFReader(pkg);
            SharedStrings sst = reader.getSharedStringsTable();
            Styles styles = reader.getStylesTable();
            XMLReader xmlReader = XMLHelper.newXMLReader();

            Iterator<InputStream> sheets = reader.getSheetsData();
            if (!sheets.hasNext()) {
                return;
            }
            try (InputStream sheet = sheets.next()) {
                Handler handler = new Handler();
                xmlReader.setContentHandler(
                        new XSSFSheetXMLHandler(styles, null, sst, handler, new DataFormatter(), false));
                xmlReader.parse(new InputSource(sheet));
            }
        } catch (Exception e) {
            workerError = e instanceof IOException io ? io : new IOException(e);
        } finally {
            try {
                queue.put(END_OF_STREAM);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void advance() {
        if (workerError != null) {
            throw new UncheckedIOException(workerError);
        }
        try {
            Map<String, Object> row = queue.take();
            if (row == END_OF_STREAM) {
                if (workerError != null) {
                    throw new UncheckedIOException(workerError);
                }
                next = null;
            } else {
                next = row;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while reading XLSX", e);
        }
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public Map<String, Object> next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        Map<String, Object> current = next;
        advance();
        return current;
    }

    @Override
    public void close() throws IOException {
        worker.interrupt();
        try {
            pkg.close();
        } catch (Exception e) {
            throw new IOException("Failed to close XLSX package", e);
        }
    }

    /**
     * SAX-callback handler that converts XSSF cell events into ordered maps and pushes them onto
     * the consumer queue. Header detection: the first row encountered defines column names.
     */
    private final class Handler implements SheetContentsHandler {
        private final List<String> headers = new ArrayList<>();
        private Map<String, Object> currentRow;
        private int currentRowNumber = -1;
        private boolean isHeaderRow;

        @Override
        public void startRow(int rowNum) {
            currentRowNumber = rowNum;
            isHeaderRow = headers.isEmpty();
            currentRow = isHeaderRow ? new LinkedHashMap<>() : new LinkedHashMap<>(headers.size());
        }

        @Override
        public void endRow(int rowNum) {
            if (isHeaderRow) {
                for (Object value : currentRow.values()) {
                    headers.add(value == null ? "" : value.toString());
                }
                return;
            }
            if (currentRow.isEmpty()) {
                return;
            }
            try {
                queue.put(currentRow);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while enqueueing row", e);
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int columnIndex = new CellAddress(cellReference).getColumn();
            if (isHeaderRow) {
                currentRow.put(Integer.toString(columnIndex), formattedValue);
                return;
            }
            if (columnIndex < headers.size()) {
                currentRow.put(headers.get(columnIndex), formattedValue);
            }
        }
    }
}
