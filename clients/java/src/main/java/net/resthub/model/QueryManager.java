package net.resthub.model;

import au.com.bytecode.opencsv.CSVReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.Getter;
import lombok.Setter;
import net.resthub.util.Helper;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class QueryManager {

    private final String url;
    @Getter
    private final String entity;
    @Getter
    private String id;
    @Getter
    private Map<String, Object> params = new HashMap<>();
    @Getter
    @Setter
    private HashMap headers;
    @Getter
    @Setter
    private long page = 0;
    @Getter
    @Setter
    private long ppage = 0;
    @Getter
    @Setter
    private boolean forceRefresh = false;
    
    private final ReentrantReadWriteLock readWriteLock;
    private final Lock read;
    private final Lock write;
    
    public QueryManager(String url, String sql) {
        this.url = url;
        this.entity = sql;
        this.readWriteLock =  new ReentrantReadWriteLock();
        this.read  = readWriteLock.readLock();
        this.write = readWriteLock.writeLock();
    }

    /**
     * Refreshes query in server. There is one request to server.
     */
    public void refresh() throws IOException {
        this.write.lock();
        try {
            ClientResource client = new ClientResource(this.url + "/query");
            this.id = client.post(this.entity).getText();
            client.release();
        } finally {
            this.write.unlock();
        }
    }
    
    /**
     * Gets getData from server and returns String. There is one request to 
     * server.
     * 
     * @param contentType Content type of the string.
     * @return An String object.
     */  
    public DataResponse getData(String contentType) throws IOException, JSONException {        
        return getData(MediaType.valueOf(contentType));
    }
    
    /**
     * Gets getData from server and returns String. There is one request to 
     * server.
     * 
     * @param mediaType MediaType of the string.
     * @return An String object.
     */  
    public DataResponse getData(MediaType mediaType) throws IOException, JSONException {
        this.read.lock();
        try {
            if ((id == null) || (forceRefresh)) {
                this.read.unlock();
                refresh();
                this.read.lock();
            }

            String path = "/query/" + id;
            if (ppage > 0 && page > 0) {
                path += "/page/" + ppage + "/" + page;
            }
            path += "/data";

            if (!this.params.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                path += "?";
                
                Iterator<Map.Entry<String, Object>> entries = this.params.entrySet().iterator();
                while (entries.hasNext()) {
                    Entry<String, Object> entry = entries.next();
                    sb.append(entry.getKey());
                    sb.append('=');
                    sb.append(entry.getValue());
                    if(entries.hasNext()){
                        sb.append('&');      
                    }
                }
                path += sb.toString();
            }

            ClientResource client = new ClientResource(this.url + path);
            if (headers != null) {
                addHeaders(client);
            }
            try {
                if (mediaType == null) {
                    client.get();
                } else {
                    client.get(mediaType);
                }
                client.release();
                
            } catch (ResourceException e) {
                if (!forceRefresh && e.getStatus().getCode() == 404) {
                    forceRefresh = true;
                    this.read.unlock();
                    try {
                        return getData(mediaType);
                    } finally {
                        this.read.lock();
                    }
                } else {
                    return null;
                }
            }
            Representation r = client.getResponseEntity();
            Map responseHeaders = ((Series)client.getResponseAttributes().get("org.restlet.http.headers")).getValuesMap();
            int result = client.getStatus().getCode();
            return new DataResponse(r, responseHeaders, result);
        } finally {
            this.read.unlock();
        }
    }
    
    /**
     * Gets getData from server and returns JSONObject. There is one request to 
     * server.
     * 
     * @return An JSONObject object.
     */
    public JSONObject getDataJSON() throws JSONException, IOException{
        String s = getData(MediaType.APPLICATION_JSON).getString();
        return new JSONObject(s);
    }
    
    /**
     * Gets getData from server and returns XML Document. There is one request to 
     * server.
     * 
     * @return An Document object.
     */
    public Document getDataXML() throws IOException, JSONException, SAXException, ParserConfigurationException{
        String s = getData(MediaType.APPLICATION_XML).getString();
        return stringToDom(s);
    }
    
    /**
     * Gets getData from server and returns table. There is one 
     * request to server.
     * 
     * @return An Two-Dimensional Array.
     */
    public String[][] getDataTable() throws IOException, JSONException {
        Reader r = getData(MediaType.TEXT_CSV).getReader();
        CSVReader csvReader = new CSVReader(r);
        List<String[]> list = csvReader.readAll();
        String[][] dataArr = new String[list.size()][];
        dataArr = list.toArray(dataArr);
        return dataArr;
    }
    
    private static Document stringToDom(String xmlSource)
            throws SAXException, ParserConfigurationException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlSource)));
    }

    private void addHeaders(ClientResource client) {
        Series<Header> reqHeaders = (Series<Header>) client.getRequestAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
        if (reqHeaders == null) {
            reqHeaders = new Series(Header.class);
            client.getRequestAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, reqHeaders);
        }

        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            reqHeaders.add(new Header(pairs.getKey().toString(), pairs.getValue().toString()));
        }
    }

    /**
     * Deletes the query from server.
     */
    public void delete() {
        this.write.lock();
        try {
            if (id != null) {
                String path = "/query/" + this.id;
                ClientResource client = new ClientResource(this.url + path);
                if (headers != null) {
                    addHeaders(client);
                }
                client.delete();
                client.release();
            }
        } finally {
            this.write.unlock();
        }
    }
    
    public Map options() throws IOException {
        
        this.read.lock();
        try {
            Map m;
            if ((id == null) || (forceRefresh)) {
                this.read.unlock();
                refresh();
                this.read.lock();
            }
            String path = "/query/" + id + "/data";
            ClientResource client = new ClientResource(this.url + path);
            if (headers != null) {
                addHeaders(client);
            }
            client.options();
            client.release();
            
            m = ((Series) client.getResponseAttributes().get("org.restlet.http.headers")).getValuesMap();
            return m;
        } finally {
            this.read.unlock();
        }
    }
    
//    public Object metaDataObject() throws IOException, JSONException {
//        if ((id == null) || (forceRefresh)) {
//            refresh();
//        }
//
//        String path = "/query/" + id + "?v=" + isVerbose();
//
//        ClientResource client = new ClientResource(this.url + path);
//        if (headers != null) {
//            addHeaders(client);
//        }
//        if (getMediaType() == null) {
//            client.get();
//        } else {
//            client.get(getMediaType());
//        }
//        String r = client.getResponseEntity().getText();
//        if (getMediaType() == MediaType.APPLICATION_JSON) {
//            return new JSONObject(r);
//        }
//        return r;
//    }

//    public Object columnsObject() throws IOException, JSONException {
//        return ((JSONObject) metaDataObject()).get("columns");
//    }

    private Query getQ(boolean v) throws JSONException, IOException {
        if ((id == null) || (forceRefresh)) {
            refresh();
        }
        String localUrl = this.url + "/query/" + id;
        if (v) {
            localUrl += "?v=true";
        }

        JSONObject jsonObject = Helper.getJSONObject(localUrl);

        return new Query(id, jsonObject, v);
    }

    /**
     * Gets a Query object. There is one request to server.
     * 
     * @return Created Query object.
     */
    public Query getQuery() throws JSONException, IOException {
        return getQ(false);
    }

    /**
     * Gets a Query object with additional getData. There is one 
     * request to server.
     * 
     * @return Created Query object.
     */
    public Query getVerboseQuery() throws JSONException, IOException {
        return getQ(true);
    }
    
    /**
     * Adds a parameter to QueryManager.
     * 
     * @param key Key of the parameter.
     * @param object Value of the parameter.
     */
    public void addParameter(String key, Object object) {
        this.params.put(key, object);
    }
}
