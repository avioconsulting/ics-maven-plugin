package com.avioconsulting.ics;

import com.avioconsulting.ics.util.RestUtilities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Connection {

    private static final String STATUS_URL = "/icsapis/v2/connections/{id}";
    private static final String UPDATE_URL = "/icsapis/v2/connections/{id}";
    private static final String DELETE_URL = "/icsapis/v2/connections/{id}";

    private String configFile;
    private String configDirectory;

    private final String id;
    private RestUtilities util;
    private Log log;

    public Connection(String id, RestUtilities util){
        this.id = id;
        this.util = util;
    }

    public Connection(String id, RestUtilities util, Log log){
        this.id = id;
        this.log = log;
        this.util = util;
    }

    public Connection(String id, final String baseUrl, final String user, final String password){
        this.id = id;
        setUtil(user, password, baseUrl);
    }

    public void updateConnection() throws Exception {
        getLog().debug("[Connection.updateConnection] Starting");

        File f = new File(getConfigFile());
        if (!f.exists()) {
            getLog().error("ERROR: Unable to find config file, " + getId() + ".json");
            getLog().error("       Execute the generate-resources phase with the export and connection name parameters to create a properties template.");
            getLog().error("       ie. mvn generate-resources -Dexport=true -Dconnection=" + getId());
            getLog().error("File location: " + f.getAbsolutePath());

            throw new Exception("Unable to find configuration for: " + getConfigFile());
        }
        FileInputStream fis = new FileInputStream(f);
        String connectionProps = IOUtils.toString(fis, "UTF-8");

        Map<String, String> params = new HashMap<String, String>();
        params.put("id", getId());

        Map<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.put("Content-Type", "application/json");
        additionalHeaders.put("X-HTTP-Method-Override", "PATCH");
        ResponseEntity<String> response = util.invokeService(UPDATE_URL, HttpMethod.POST, connectionProps, params, additionalHeaders);

        getLog().info("[Connection.updateConnection] Complete");
    }

    public String getStatus(){
        /* ACTIVATED, HTTP 404 Not Found, CONFIGURED, ... */
        JsonNode node = retrieveConnectionDetails();
        return node != null ? node.path("status").asText() : "UNKNOWN";
    }

    public void exportProperties(boolean overwrite) throws IOException {
        String filename = getConfigFile();
        File props = new File(filename);
        if(props.exists() && !overwrite){
            getLog().error("ERROR: The property file " + filename + " already exists.");
            getLog().error("       Use the -Doverwrite=true to overwrite it, or remove it manually.");
            throw new IOException("File already exists.");
        } else {
            new File(props.getParent()).mkdirs();
        }

        retrieveConnectionDetails(true, props);
        getLog().info("\n\n\n");
        getLog().info("--------------------------------------------------------------------------------------------------------------");
        getLog().info("Properties file created.  " + filename);
        getLog().info("IMPORTANT: Remember this is only a TEMPLATE.  Manually edit the template json file to include the ");
        getLog().info("           correct environment properties and values.  Store this file in src/main/resources/config directory.");
        getLog().info("--------------------------------------------------------------------------------------------------------------\n\n\n");
    }

    private JsonNode retrieveConnectionDetails(){
        // just return the JsonNode
        return retrieveConnectionDetails(false, null);
    }
    /**
     * Calls the rest service to retrieve details regarding the integration.
     *
     * @param writeToFile       (Optional) Boolean to write the details (property template) to a file
     * @param propertiesFile    (Optional) File object to write properties to
     * @return                  A JSONNode object with all integration details
     */
    private JsonNode retrieveConnectionDetails(boolean writeToFile, File propertiesFile){
        getLog().debug("[Connection.retrieveConnectionDetails] Starting");
        JsonNode node = null;

        Map<String, String> params = new HashMap<String, String>();
        params.put("id", getId());

        ResponseEntity<String> res = util.invokeService(STATUS_URL, HttpMethod.GET, null, params, null);
        ObjectMapper om = new ObjectMapper();
        if (res != null) {
            try {
                node = om.readTree(res.getBody());
                if(writeToFile){
                    Object json = om.readValue(node.toString(), Object.class);
                    ObjectWriter writer = om.writerWithDefaultPrettyPrinter();
                    writer.writeValue(propertiesFile, json);
                    // this escapes all quotes...
//                    om.writeValue(propertiesFile, res.getBody());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            getLog().info("[Connection.retrieveConnectionDetails] Complete");
        } else {
            getLog().info("[Connection.retrieveConnectionDetails] Url indicated connection did not exist.");
        }
        return node;
    }

    public String getId() {
        return id;
    }

    public void setUtil(final String username, final String password, final String baseUrl) {
        this.util = new RestUtilities(baseUrl, username, password);
    }

    public String getConfigFile() {
        if(configFile==null || configFile.length()<1){
            if(getConfigDirectory() != null && getConfigDirectory().length() > 0) {
                configFile = getConfigDirectory() + "/" + getId() + ".json";
            } else {
                configFile = "target/connections/" + getId() + ".json";
            }
        }
        return configFile;
    }

    public Log getLog() {
        if (log==null) {
            log = new SystemStreamLog();
        }
        return log;
    }

    public void setLog(Log log) {
        this.util.setLog(log);
        this.log = log;
    }

    public String getConfigDirectory(){
        return configDirectory;
    }

    public void setConfigDirectory(String configDir){
        configDirectory = configDir;
    }

    public void deleteConnection() throws Exception {
        getLog().debug("[Connection.deleteConnection] Starting");

        Map<String, String> params = new HashMap<String, String>();
        params.put("id", getId());

        ResponseEntity<String> res = util.invokeService(DELETE_URL, HttpMethod.DELETE, null, params, null);

        if (res!=null && res.getStatusCode() != HttpStatus.OK) {
            getLog().info("--------------------------------------------------------------------------------------------------------------");
            getLog().info("Connection " + getId() + " cannot be deleted.");
            getLog().info("Response: " + res.toString());
            getLog().info(res.getStatusCode().toString());
            getLog().info("--------------------------------------------------------------------------------------------------------------\n");
            throw new Exception(res.getBody());
        }

        getLog().debug("[Connection.deleteConnection] Complete");
    }
}
