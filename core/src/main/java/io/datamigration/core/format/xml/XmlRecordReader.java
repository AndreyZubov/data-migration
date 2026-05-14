package io.datamigration.core.format.xml;

import io.datamigration.core.format.RecordReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Streaming reader for XML documents of the form:
 *
 * <pre>{@code
 * <records>
 *   <record>
 *     <Name>Alice</Name>
 *     <Age>30</Age>
 *   </record>
 *   ...
 * </records>
 * }</pre>
 *
 * <p>The root element name is ignored; each direct child element is treated as one record, and its
 * immediate children become field name → text value pairs. Deeper nesting is not supported.
 *
 * <p>External entity resolution and DTDs are disabled to protect against XXE attacks.
 */
public final class XmlRecordReader implements RecordReader {

    private final XMLStreamReader stax;
    private final InputStream source;
    private Map<String, Object> next;

    public XmlRecordReader(InputStream in) throws IOException {
        this.source = in;
        try {
            XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", Boolean.FALSE);
            this.stax = factory.createXMLStreamReader(in);
            advanceToRoot();
            advance();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private void advanceToRoot() throws XMLStreamException {
        while (stax.hasNext()) {
            int event = stax.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                return;
            }
        }
    }

    private void advance() throws XMLStreamException {
        while (stax.hasNext()) {
            int event = stax.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                next = readRecord();
                return;
            }
            if (event == XMLStreamConstants.END_ELEMENT
                    || event == XMLStreamConstants.END_DOCUMENT) {
                next = null;
                return;
            }
        }
        next = null;
    }

    private Map<String, Object> readRecord() throws XMLStreamException {
        Map<String, Object> row = new LinkedHashMap<>();
        while (stax.hasNext()) {
            int event = stax.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String field = stax.getLocalName();
                String text = stax.getElementText();
                row.put(field, text);
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return row;
            }
        }
        return row;
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
        try {
            advance();
        } catch (XMLStreamException e) {
            throw new UncheckedIOException(new IOException(e));
        }
        return current;
    }

    @Override
    public void close() throws IOException {
        try {
            stax.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } finally {
            source.close();
        }
    }
}
