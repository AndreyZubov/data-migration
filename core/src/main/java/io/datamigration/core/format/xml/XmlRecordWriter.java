package io.datamigration.core.format.xml;

import io.datamigration.core.format.RecordWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Streaming writer producing XML documents with a fixed {@code <records><record>} layout. Each
 * field is emitted as a child element with text content; {@code null} values produce empty
 * elements.
 */
public final class XmlRecordWriter implements RecordWriter {

    private static final String ROOT = "records";
    private static final String RECORD = "record";

    private final XMLStreamWriter stax;
    private final OutputStream out;
    private boolean closed;

    public XmlRecordWriter(OutputStream out) throws IOException {
        this.out = out;
        try {
            XMLOutputFactory factory = XMLOutputFactory.newDefaultFactory();
            this.stax = factory.createXMLStreamWriter(out, "UTF-8");
            stax.writeStartDocument("UTF-8", "1.0");
            stax.writeStartElement(ROOT);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(Map<String, Object> record) throws IOException {
        try {
            stax.writeStartElement(RECORD);
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                stax.writeStartElement(entry.getKey());
                if (entry.getValue() != null) {
                    stax.writeCharacters(entry.getValue().toString());
                }
                stax.writeEndElement();
            }
            stax.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            stax.flush();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            stax.writeEndElement();
            stax.writeEndDocument();
            stax.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } finally {
            out.close();
        }
    }
}
