package com.avioconsulting.ics;

import com.avioconsulting.ics.util.RestUtilities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.util.Scanner;
import org.springframework.core.io.FileSystemResource;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * An ICS Integration class.  This exposes control commands
 * and stores properties associated with this integration.
 */
public class Integration {

    public static final String STATUS_NOT_FOUND = "HTTP 404 Not Found";
    public static final String STATUS_ACTIVATED = "ACTIVATED";
    public static final String EXPAND_DIR = "/target/export-iar";

    private static final Integer BYTE_SIZE = 2048;

    private final String name;
    private final String version;
    private RestUtilities util;
    private Log log;

    private static final String STATUS_URL = "/icsapis/v2/integrations/{integration}|{version}";
    private static final String STATUS_URL_V1 = "/icsapis/v1/integrations/{integration}/{version}";
    /* PUT to replace, POST for new */
    private static final String IMPORT_URL = "/icsapis/v2/integrations/archive";
    /* GET */
    private static final String EXPORT_URL = "/icsapis/v2/integrations/{id}/archive";
    /* DELETE */
    private static final String DELETE_URL = "/icsapis/v2/integrations/{id}";
    /* Activate and Deactivate must use v1 */
    private static final String DEACTIVATE_URL = "/icsapis/v1/integrations/{integration}/{version}/deactivate";
    private static final String ACTIVATE_URL = "/icsapis/v1/integrations/{integration}/{version}/activate?enablePayloadTracing={payloadTrace}&enableTracing={enableTrace}";

    private Map<String, Connection> connections;

    /**
     * Constructor for instantiating the integration.  Name and Version are required to
     * make this integration unique.
     *
     * @param name      The name of the integration ie. "APLM_AGILE_INTEG_SERVICE"
     * @param version   Version of the integration ie. "01.00.0000"
     * @param baseUrl   Environment base url / hostname
     * @param user      Username
     * @param password  Password for the username
     */
    public Integration(final String name, final String version, final String baseUrl, final String user, final String password) {
        this.name = name;
        this.version = version;
        setUtil(user, password, baseUrl);
    }

    /**
     * Constructor for instantiating the integration.  Name and Version are required to
     * make this integration unique.
     *
     * @param name      The name of the integration ie. "APLM_AGILE_INTEG_SERVICE"
     * @param version   Version of the integration ie. "01.00.0000"
     * @param util      RestUtilities class
     */
    public Integration(final String name, final String version, final RestUtilities util) {
        this.name = name;
        this.version = version;
        this.util = util;
    }

    /**
     * Deactivates the integration.
     */
    public void deactivate() {

        getLog().info("[Integration.deactivate] Starting");

        Map<String, String> params = new HashMap<String, String>();
        params.put("integration", getName());
        params.put("version", getVersion());

        ResponseEntity<String> res = util.invokeService(DEACTIVATE_URL, HttpMethod.POST, null, params, null);

        getLog().info("[Integration.deactivate] Complete");
    }

    /**
     * Activates an integration.
     *
     * @param enableTrace   Flag to enable trace and audit payload
     */
    public void activate(final boolean enableTrace) {
        getLog().info("[Integration.activate] Starting");

        Map<String, String> params = new HashMap<String, String>();
        params.put("integration", getName());
        params.put("version", getVersion());
        params.put("payloadTrace", enableTrace ? "true" : "false");
        params.put("enableTrace", enableTrace ? "true" : "false");

        ResponseEntity<String> res = util.invokeService(ACTIVATE_URL, HttpMethod.POST, null, params, null);

        getLog().info("[Integration.activate] Complete");
    }

    /**
     * Imports an integration.  This will automatically replace existing integrations, and will deactivate a
     * running integration.
     *
     * @param archiveName   Name of the IAR file.
     * @throws Exception    Any exceptions while connecting to ICS
     */
    public void importIntegration(final String archiveName) throws Exception {
        getLog().info("[Integration.importIntegration] Starting");

        HttpMethod hm = null;
        // check status
        String status = getStatus();

        getLog().info("[Integration.importIntegration] Current status: " + status);
        if (status.equalsIgnoreCase(STATUS_NOT_FOUND) || status.equalsIgnoreCase("UNKNOWN")) {
            // import new integration
            hm = HttpMethod.POST;
        } else if (status.equalsIgnoreCase(STATUS_ACTIVATED)) {
            // deactivate
            deactivate();
            hm = HttpMethod.PUT;
        } else {
            //FAILEDACTIVATION,
            // or some other state... just retry
            hm = HttpMethod.PUT;
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        FileSystemResource fsr = new FileSystemResource(archiveName);
        if (!fsr.exists()){
            getLog().error("ERROR: File " + archiveName + " does not exist.");
            getLog().error("       Try running phase 'package' first.");
            getLog().error("       ie. mvn package com.avioconsulting.maven:ics-maven-plugin:import -Denv=DEV");
            throw new Exception("File " + archiveName + " does not exist.");
        }
        body.add("file", new FileSystemResource(archiveName));
        body.add("content-type", "multipart/form-data");

        ResponseEntity<String> response = util.invokeService(IMPORT_URL, hm, body, null, null);

        getLog().info("[Integration.importIntegration] Complete");
    }



    /**
     * Exports the integration writing a file Name_Version.iar
     *
     * @param expand        Boolean to expand the export
     * @param cleanFirst    Boolean to remove the src folder before exporting
     * @param basedir       Base directory of the project
     * @throws IOException  Any exceptions while connecting to ICS
     */
    public void exportIntegration(final Boolean expand, final Boolean cleanFirst, String basedir) throws IOException {
        getLog().info("[Integration.exportIntegration] Starting");
        String exportFilename = getName() + "_" + getVersion() + ".iar";

        Map<String, String> params = new HashMap<String, String>();
        params.put("id", getName() + "|" + getVersion());

        ResponseEntity<String> res = util.invokeService(EXPORT_URL, HttpMethod.GET, null, params, null);
        final String filename = basedir + "/target/" + exportFilename;
        writeFile(filename, res.getBody());

        if (expand) {
            expandIar(filename, cleanFirst, basedir);
            iarToProjectStructure(basedir + EXPAND_DIR, basedir);
        } else {
            getLog().info("[Integration.exportIntegration] Integration exported to target/" + exportFilename);
        }

        getLog().info("[Integration.exportIntegration] Complete");
    }

    /**
     * Helper method to remove directories.
     *
     * @param dir               File object to the directory
     * @throws IOException      Exceptions while removing directory
     */
    private void removeDir(File dir) throws IOException {
        if(dir.exists() && dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
        }
    }
    /**
     * Expands out the compressed file, creating directories as necessary.
     *
     * @param fileZip           Compressed filename
     * @param cleanFirst        Boolean to remove exisiting folders before expansion
     * @throws IOException      Exceptions while removing directory
     */
    private void expandIar(final String fileZip, final Boolean cleanFirst, final String basedir) throws IOException {
        getLog().info("[Integration.expandIar] Decompression for " + fileZip + " has commenced.");

        if (cleanFirst) {
            // src/main/iar
            // src/main/resources/connections
            // src/main/resources/lookups
            // src/main/resources/schedule
            getLog().info("[Integration.expandIar] all src directories (except config) are being deleted before expanding current export.");
            removeDir(new File(basedir + "src/main/iar"));
            removeDir(new File(basedir + "src/main/resources/connections"));
            removeDir(new File(basedir + "src/main/resources/lookups"));
            removeDir(new File(basedir + "src/main/resources/schedule"));
        }

        byte[] buffer = new byte[BYTE_SIZE];

        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            String fileName = zipEntry.getName();

            getLog().debug("[Integration.expandIar] writing " + basedir +  EXPAND_DIR + "/" + fileName);
            File newFile = new File(basedir + EXPAND_DIR + "/" + fileName);

            new File(newFile.getParent()).mkdirs();

            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        getLog().info("[Integration.expandIar] Decompression for " + fileZip + " has completed.");
    }

    /**
     * Rearranges export into standard project format.
     *   icspackage/appinstances  - to - src/main/resources/connections
     *   icspackage/dvms          - to - src/main/resources/lookups
     *   icspackage/schedule      - to - src/main/resources/schedule
     *   icspackage/project/<INT> - to - src/main/iar
     *
     * @param baseLocation          String location of the export
     * @throws IOException
     */
    private void iarToProjectStructure(String baseLocation, String basedir) throws IOException {

        getLog().info("[Integration.iarToProjectStructure] Starting, copying from " + baseLocation);
        if(baseLocation == null || baseLocation.length()<1){
            // keep base location from creating on root
            baseLocation = ".";
        }

        copyDirectory(new File(baseLocation + "/icspackage/appinstances"), basedir + "/src/main/resources/connections");
        copyDirectory(new File(baseLocation + "/icspackage/dvms"), basedir + "/src/main/resources/lookups");
        copyDirectory(new File(baseLocation + "/icspackage/schedule"), basedir + "/src/main/resources/schedule");
        copyDirectory(new File(baseLocation + "/icspackage/project/" + getName() + "_" + getVersion()), basedir + "/src/main/iar");

        getLog().info("[Integration.iarToProjectStructure] Finished");

    }

    /**
     * Converts the project structure to an export format, and creates an iar file.
     *
     * @param basedir       Base directory of the project
     * @throws IOException  Exception during packaging
     */
    public void packageProject(String basedir) throws IOException {
        packageProject(basedir, false);
    }

    /**
     * Converts the project structure to an export format, and creates an iar file.
     *
     * @param basedir       Base directory of the project
     * @throws IOException  Exception during packaging
     */
    public void packageProject(String basedir, boolean replaceTokens) throws IOException {
        projectToIarStructure(basedir + EXPAND_DIR, basedir);
        buildIarWithArchiver(basedir + EXPAND_DIR, basedir + "/target");
    }

    /**
     * Builds an archive into destLocation using sourceLocation.
     *
     * @param sourceLocation        Source directory for archive
     * @param destLocation          Final archive file
     * @throws IOException          Exceptions building archive
     */
    private void buildIarWithArchiver(String sourceLocation, String destLocation) throws IOException {
        getLog().info("[Integration.buildIarWithArchiver] Starting");

        // create iar file and associated directory
        File destination = new File(destLocation + "/" + getName() + "_" + getVersion() + ".iar");
        new File(destination.getParent()).mkdirs();

        ZipArchiver za = new ZipArchiver();
        za.setDestFile(destination);

        DefaultFileSet dfs = new DefaultFileSet();
        dfs.setUsingDefaultExcludes(true);
        dfs.setDirectory(new File(sourceLocation));

        za.addFileSet(dfs);
        za.createArchive();

        getLog().info("[Integration.buildIarWithArchiver] Completed building " + destination.getPath());
    }

    /**
     * Deprecated.  Use buildIarWithArchiver.
     * @param sourceLocation        Source directory to build IAR
     * @param destLocation          Destination location for the IAR
     * @throws IOException          Exceptions during building
     */
    private void buildIar(String sourceLocation, String destLocation) throws IOException {
        getLog().info("[Integration.buildIar] Starting");
        getLog().warn("Don't use this archiver, appears to be creating corrupt archives.");

        // create iar file and associated directory
        File destination = new File(destLocation + "/" + getName() + "_" + getVersion() + ".iar");
        new File(destination.getParent()).mkdirs();

        FileOutputStream dest = new FileOutputStream(destination);
        ZipOutputStream zos = new ZipOutputStream(dest);

        File source = new File(sourceLocation);
        addDirToArchive(source, source, zos);

        zos.close();

        getLog().info("[Integration.buildIar] Completed building " + destination.getPath());
    }

    /**
     * Helper method that recurses through a root/current directory to add files to a ZipOutputStream.
     *
     * @param rootDirectory             Directory to add to the ZipOutputStream
     * @param currentDirectory          Initially the same as root
     * @param out                       ZipOutputStream
     * @throws IOException              Exceptions during building archive
     */
    private void addDirToArchive(File rootDirectory, File currentDirectory, ZipOutputStream out) throws IOException {
        byte[] data = new byte[BYTE_SIZE];

        File[] files = currentDirectory.listFiles();
        if (files == null) {
            // no files were found or this is not a directory

        } else {
            for (File file : files) {
                if (file.isDirectory()) {
                    addDirToArchive(rootDirectory, file, out);
                } else {
                    FileInputStream fi = new FileInputStream(file);
                    // creating structure and avoiding duplicate file names
                    String name = file.getAbsolutePath().replace(rootDirectory.getAbsolutePath(), "");

                    ZipEntry entry = new ZipEntry(name);
                    out.putNextEntry(entry);
                    int count;
                    BufferedInputStream origin = new BufferedInputStream(fi,BYTE_SIZE);
                    while ((count = origin.read(data, 0 , BYTE_SIZE)) != -1){
                        out.write(data, 0, count);
                    }
//                    out.closeEntry();
                    origin.close();
                }
            }
        }
    }

    /**
     * Rearranges project into iar format.
     *
     *   src/main/resources/connections  - to - icspackage/appinstances
     *   src/main/resources/lookups      - to - icspackage/dvms
     *   src/main/resources/schedule     - to - icspackage/schedule
     *   src/main/iar                    - to - icspackage/project/NAME_VERSION
     * @param destLocation          String location for the iar structure
     * @throws IOException          Exceptions moving around files
     */
    private void projectToIarStructure(String destLocation, String basedir) throws IOException {
        getLog().info("[Integration.projectToIarStructure] Starting");

        if(destLocation == null || destLocation.length()<1){
            // dest location must be something, else it'll be copied to root.
            destLocation = ".";
        }

        copyDirectory(new File(basedir + "/src/main/resources/connections"),destLocation + "/icspackage/appinstances");
        copyDirectory(new File(basedir + "/src/main/resources/lookups"),destLocation + "/icspackage/dvms");
        copyDirectory(new File(basedir + "/src/main/resources/schedule"),destLocation + "/icspackage/schedule");
        copyDirectory(new File(basedir + "/src/main/iar"),destLocation + "/icspackage/project/" + getName() + "_" + getVersion());

        getLog().info("[Integration.projectToIarStructure] Complete");
    }

    /**
     * Helper method wrapping utils to copy a directory if the source exists and it is a directory.
     *
     * @param source            File object of the source directory
     * @param dest              String location of the
     * @throws IOException      Exceptions copying directories
     */
    private void copyDirectory(File source, String dest) throws IOException {
        getLog().info("[Integration.copyDirectory] Copying " + source.getName() + " to " + dest);

        if(source.exists() && source.isDirectory()) {

            File destFile = new File(dest);
            new File(destFile.getParent()).mkdirs();

            // Don't copy in the .json property files (these break the import...)
            IOFileFilter ff = FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter(".json"));
            FileUtils.copyDirectory(source, destFile, ff);

        } else {
            getLog().warn("[Integration.copyDirectory] Attempted to copy " + source.getName() + " either it doesn't exist, or isn't a directory.");
        }
        getLog().info("[Integration.copyDirectory] Done copying " + source + " to " + dest);

    }

    /**
     * Scanner imposed directory copy to only copy 'good' directories (addExcludes/Includes)
     *
     * @param sourceDirectory           File object of the source directory
     * @param destinationDirectory      File object of the destination directory
     * @param scanner                   Scanner object
     * @throws IOException              Exception copying directories
     */
    private void copyDirectoryLayout( File sourceDirectory, File destinationDirectory, Scanner scanner )
            throws IOException
    {
        if ( sourceDirectory == null )
        {
            throw new IOException( "source directory can't be null." );
        }

        if ( destinationDirectory == null )
        {
            throw new IOException( "destination directory can't be null." );
        }

        if ( sourceDirectory.equals( destinationDirectory ) )
        {
            throw new IOException( "source and destination are the same directory." );
        }

        if ( !sourceDirectory.exists() )
        {
            throw new IOException( "Source directory doesn't exists (" + sourceDirectory.getAbsolutePath() + ")." );
        }

        List<String> includedDirectories = Arrays.asList( scanner.getIncludedDirectories() );

        for ( String name : includedDirectories )
        {
            File source = new File( sourceDirectory, name );

            if ( source.equals( sourceDirectory ) )
            {
                continue;
            }

            File destination = new File( destinationDirectory, name );
            destination.mkdirs();
        }
    }


    /**
     * Handles writing the file to disk.
     *
     * @param filename      Name of the file to be created
     * @param contents      String contents of the file
     * @throws IOException  Exceptions writing file
     */
    private void writeFile(final String filename, final String contents) throws IOException {
        File f = new File("target");
        if (!f.exists()){
            f.mkdir();
        }
        FileOutputStream fos = new FileOutputStream(filename);
        DataOutputStream dos = new DataOutputStream(fos);
        dos.writeBytes(contents);

        dos.flush();
        dos.close();
        fos.flush();
        fos.close();

        // write goes too fast, and immediately tries to expand the file
        // causing a file not found error.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Default call to get integration details using V2 of the API
     *
     * @return      A JSONNode object with the integration details
     */
    private JsonNode retrieveIntegrationDetails() {
        return retrieveIntegrationDetails(false);
    }
    /**
     * Calls the rest service to retrieve details regarding the integration.
     *
     * @return      A JSONNode object with the integration details
     */
    private JsonNode retrieveIntegrationDetails(boolean versionOne){
        getLog().info("[Integration.retrieveIntegrationDetails] Starting");
        JsonNode node = null;

        Map<String, String> params = new HashMap<String, String>();
        params.put("integration", getName());
        params.put("version", getVersion());
        String url = versionOne ? STATUS_URL_V1 : STATUS_URL;
        ResponseEntity<String> res = util.invokeService(url, HttpMethod.GET, null, params, null);
        ObjectMapper om = new ObjectMapper();
        if(res != null) {
            try {
                node = om.readTree(res.getBody());
            } catch (IOException e) {
                e.printStackTrace();
            }
            getLog().info("[Integration.retrieveIntegrationDetails] Complete");
        } else {
            getLog().info("[Integration.retrieveIntegrationDetails] Url determined the integration does not exist.");
        }
        return node;
    }

    /**
     * Invokes the retrieveIntegrationDetails service to retrieve the status.
     *
     * @return      Status of the integration
     */
    public String getStatus() {
        /* ACTIVATED, HTTP 404 Not Found, CONFIGURED, ... */
        JsonNode node = retrieveIntegrationDetails();
        return node != null ? node.path("status").asText() : "UNKNOWN";
    }

    /**
     * Returns a map of unique connections.
     *
     * @return      Map of connection name and details
     */
    public Map<String, Connection> getConnections(){
        return getConnections(false);
    }


    /**
     * Looks at the 'endPoints' tag of the connection, returning
     * a map of unique connections.  This uses v2 of the API
     *
     * @param versionOne    Flag to use v1
     * @return      Map of connection name and details
     */
    public Map<String, Connection> getConnections(boolean versionOne){
        if(connections == null || connections.size() == 0) {
            getLog().info("[Integration.getConnections] Finding connections.");
            Map<String, Connection> m = new HashMap<String, Connection>();
            JsonNode integration = retrieveIntegrationDetails(versionOne);
            if(integration != null) {
                if (versionOne) {
                    if (integration.hasNonNull("invokes")) {
                        m = parseConnectionProperties(integration.path("invokes").path("items"), integration.path("sourceConnection"), integration.path("targetConnection"));
                    } else {
                        getLog().warn("Unable to find 'invokes' element in integration details (version one)");
                    }
                } else {
                    if (integration.hasNonNull("endPoints")) {
                        m = parseEndPoints(integration.path("endPoints"));
                    } else {
                        getLog().warn("Unable to find 'endPoints' element in integration details (version two)");
                    }
                }
            }
            connections = m;
        }
        return connections;
    }

    /**
     * Parsing ConnectionProperties element from API v1
     *
     * @param itemsNode         JsonNode of items in v1
     *                          JsonNode of source
     * @return                  Map<String, Connection> of unique connections for an integration
     */
    private Map<String, Connection> parseConnectionProperties(JsonNode itemsNode, JsonNode source, JsonNode target) {
        getLog().debug("[Integration.parseConnectionProperties] Starting parsing");
        Map<String, Connection> m = new HashMap<String, Connection>();
        if(itemsNode.isArray()) {
            Iterator<JsonNode> items = itemsNode.elements();
            while (items.hasNext()) {
                JsonNode item = items.next();
                String connName = item.path("code").asText();
                if(item.hasNonNull("connectionproperties")){
                    m.put(connName, new Connection(connName, util, getLog()));
                    getLog().debug("[Integration.parseConnectionProperties] Adding Connection: " + connName + " to map.");
                } else {
                    getLog().debug("[Integration.parseConnectionProperties] Not a connection... : " + connName);
                }
            }
        } else {
            //Single item
            String connName = itemsNode.path("code").asText();

            if(itemsNode.hasNonNull("connectionproperties")) {
                getLog().debug("[Integration.parseConnectionProperties] Adding Connection: " + connName + " to map.");
                m.put(connName, new Connection(connName, util, getLog()));
            } else {
                getLog().debug("[Integration.parseConnectionProperties] Not a connection... : " + connName);
            }
        }

//        JsonNode source = integration.path("sourceConnection");
        String sourceName = source.path("code").asText();
        if(source.hasNonNull("connectionproperties")) {
            getLog().debug("[Integration.parseConnectionProperties] Adding Source connection: " + sourceName + " to map.");
            m.put(sourceName, new Connection(sourceName, util, getLog()));
        }else {

            getLog().debug("[Integration.parseConnectionProperties] Not a connection... : " + sourceName);
        }
//        JsonNode target = integration.path("targetConnection");
        String targetName = target.path("code").asText();
        if(target.hasNonNull("connectionproperties")) {
            getLog().debug("[Integration.parseConnectionProperties] Adding Target connection: " + targetName + " to map.");
            m.put(targetName, new Connection(targetName, util, getLog()));
        }else {

            getLog().debug("[Integration.parseConnectionProperties] Not a connection... : " + targetName);
        }
        getLog().debug("[Integration.parseConnectionProperties] Completed parsing");
        return m;
    }

    /**
     * Parsing EndPoints element from API v2
     *
     * @param itemsNode         JsonNode of items in v2
     * @return                  Map<String, Connection> of unique connections for an integration
     */
    private Map<String, Connection> parseEndPoints(JsonNode itemsNode){
        getLog().debug("[Integration.parseEndPoints] Starting parsing");
        Map<String, Connection> m = new HashMap<String, Connection>();
        if(itemsNode.isArray()) {
            Iterator<JsonNode> items = itemsNode.elements();
            while (items.hasNext()) {
                JsonNode item = items.next();
                if(item.hasNonNull("connection")){
                    String connName = item.path("connection").path("id").asText();
                    m.put(connName, new Connection(connName, util, getLog()));
                    getLog().debug("[Integration.parseEndPoints] Adding Connection: " + connName + " to map.");
                } else {
                    getLog().debug("[Integration.parseEndPoints] Not a connection... : " + item.asText());
                }
            }
        } else {
            /* should always be an array now... */
            getLog().error("[Integration.parseEndPoints] Something is wrong... endPoints element should return an array. ");

        }
        getLog().debug("[Integration.parseEndPoints] Completed parsing");
        return m;
    }


    /**
     * Accessor for Name attribute.
     * @return      Name
     */
    public String getName() {
        return name;
    }
    /**
     * Accessor for Version attribute.
     * @return      Version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Accessor for RestUtilities
     * @return      RestUtilities
     */
    public RestUtilities getUtil() {
        return util;
    }

    /**
     * Setter for RestUtility
     * @param username      Username of ICS user
     * @param password      Password of said user
     * @param baseUrl       ICS base url
     */
    public void setUtil(String username, String password, String baseUrl) {
        this.util = new RestUtilities(baseUrl, username, password);
    }

    /**
     * Setter for Logger
     * @param log   Logger
     */
    public void setLog(Log log) {
        this.util.setLog(log);
        this.log = log;
    }

    /**
     * Accessor for Logger
     * @return  Logger
     */
    public Log getLog() {
        if (log==null) {
            log = new SystemStreamLog();
        }
        return log;
    }

    public void delete(boolean removeConnections) throws Exception {
        getLog().info("[Integration.delete] Starting.");
        String status = getStatus();

        getLog().info("[Integration.delete] Current status: " + status);
        if (status.equalsIgnoreCase("ACTIVATED")){
        // if configured, deactivate
            deactivate();
        } else if(status.equalsIgnoreCase("UNKNOWN")) {
            // if unknown... STOP
            getLog().error("[Integration.delete] Integration status is unknown.  Does the integration exist?");
            throw new Exception("Integration status is unknown.  Does the integration exist?");
        }
        //save off connections
        Map<String,Connection> connections = getConnections();

        // delete
        getLog().info("[Integration.delete] Deleting integration.");
        Map<String, String> params = new HashMap<String, String>();
        params.put("id", getName() + "|" + getVersion());

        ResponseEntity<String> res = util.invokeService(DELETE_URL, HttpMethod.DELETE, null, params, null);

        if(removeConnections){
            for(String name : connections.keySet()){
                getLog().info("[Integration.delete] Removing connection " + name);
                connections.get(name).deleteConnection();
            }
        }
        getLog().info("[Integration.delete] Completed.");
    }
}
