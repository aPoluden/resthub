package net.resthub.server.converter;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.resthub.model.MdColumn;
import net.resthub.server.cache.CcData;
import net.resthub.server.handler.DataHandler;
import net.resthub.server.query.Query;

import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.WriterRepresentation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * JSONExporter
 * @author valdo
 */
public class XMLConverter implements DataConverter {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("y-M-d'T'H:ms:d");
    private static final DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();
    private static final TransformerFactory TRF = TransformerFactory.newInstance(); 
    
    private static Element appendElement(Document doc, String name, Node parent) {
        Element e = doc.createElement(name);
        parent.appendChild(e);
        return e;
    }

    @Override
    public Representation convert(DataHandler handler, final Reference ref, CcData data) throws Exception {
        Query query = handler.getQuery();
        DocumentBuilder db = DBF.newDocumentBuilder();
        final Document doc = db.newDocument();
        final Element root = appendElement(doc, "data", doc);
        
        if (handler.isPrintColumns()) {
            Element cols = appendElement(doc, "cols", root);
            for (MdColumn c: query.getColumns()) {
                Element col = appendElement(doc, "col", cols);
                appendElement(doc, "name", col).setTextContent(c.getName());
                appendElement(doc, "type", col).setTextContent(c.getType().name());
                appendElement(doc, "cname", col).setTextContent(c.getCName());
            }
        }
        
       new DataVisitor(handler) {
            
            private Element row;
            
            @Override
            public void startRow() {
                row = doc.createElement("row");
            }
            
            @Override
            public void visitCol() {
                if (value != null) {
                    String svalue = null;
                    switch (column.getType()) {
                        case DATE:
                            svalue = DATE_FORMAT.format((Date) value);
                            break;
                        case NUMBER:
                            svalue = ((BigDecimal) value).toPlainString();
                            break;
                        case STRING:
                            svalue = (String) value;
                            break;
                        case BLOB:
                            svalue = getLobReference(ref).toString();
                            break;
                        case CLOB:
                            svalue = getLobReference(ref).toString();
                            break;
                    }
                    
                    if (svalue != null) {
                        appendElement(doc, column.getCName(), row).setTextContent(svalue);
                    }
                    
                }
            }
            
            @Override
            public void endRow() {
                root.appendChild(row);
            }
            
        }.visit(data.getValue());
        
        return new WriterRepresentation(MediaType.TEXT_XML) {
            @Override
            public void write(Writer writer) throws IOException {
                try {
                    Transformer tr = TRF.newTransformer();
                    DOMSource source = new DOMSource(doc);
                    StreamResult result = new StreamResult(writer);
                    tr.transform(source, result);
                } catch (TransformerException ex) {
                    throw new IOException(ex);
                }
            }
        };
        
    }
    
}