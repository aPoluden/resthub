package net.resthub.client;

import net.resthub.model.QueryManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import net.resthub.model.Namespace;
import net.resthub.util.Helper;
import org.json.JSONException;
import org.json.JSONObject;

    /** 
     * The RestHubServer class is used to get Namespace and Query objects.
     */
@Getter
public class RestHubServer {

    private final String url;

    /**
     * Creates an object of RestHubServer.
     * 
     * @param url An URL of running RestHub server.
     */
    public RestHubServer(String url) {
        this.url = url;
    }
    
    /**
     * Creates and returns a QueryManager object by given sql string.
     * 
     * @param sql Sql string.
     * @return New QueryManager object.
     */
    public QueryManager newQueryManager(String sql) {  
        return new QueryManager(url, sql);
    }
    
    /**
     * Returns The list of namespaces from the server. There is one request to 
     * server.
     * 
     * @return The list of Namespace objects.
     */
    public List<Namespace> getNamespaces() throws IOException, JSONException{
        JSONObject jsonObject = Helper.getJSONObject(this.url);
        
        Iterator i = jsonObject.keys();
        List<Namespace> list = new ArrayList<>();
        while (i.hasNext()) {
            String name = i.next().toString();
            JSONObject namespace = jsonObject.getJSONObject(name);
            list.add(new Namespace(name, namespace));
        }
        return list;    
    }
    
    /**
     * Returns the list of query ids from the server. There is one request to 
     * server.
     * 
     * @return The list of query ids.
     */
    public List<String> getQueryIdList() throws IOException, JSONException{
        JSONObject jsonObject = Helper.getJSONObject(this.url + "/queries");
        
        Iterator i = jsonObject.keys();
        List<String> list = new ArrayList<>();
        while (i.hasNext()) {
            String name = i.next().toString();
            list.add(name);
        }
        return list;
    }
    
    /**
     * Creates and returns a QueryManager object by given query id. There is one
     * request to server.
     * 
     * @param id Existing Query id.
     * @return New QueryManager object.
     */
    public QueryManager getQueryManager(String id) throws IOException, JSONException{
        JSONObject jsonObject = Helper.getJSONObject(this.url + "/query/" + id);
        
        return new QueryManager(url, jsonObject.getString("query"));
    }
}