package com.avioconsulting.ics.util;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
//import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.codehaus.plexus.util.Base64;


/**
 * Utility class for calling the ICS REST API services.
 */
public class RestUtilities {
    /**
     * Username variable for ICS Rest API invocations.
     */
    private final String username;
    /**
     * Password variable for ICS Rest API invocations.
     */
    private final String password;
    /**
     * Environment Hostname/URL for basis of ICS Rest API invocations.
     */
    private final String envUrl;
    /**
     * Rest Template variable for easy access.
     */
    private RestTemplate rt;

    /**
     * Optional logger
     */
    private Log log;

    /**
     * Constructor requiring a Username/Password/Environment URLs.
     *
     * @param envBaseUrl        Environment Hostname/URL for ICS Rest API
     * @param username          Username for ICS Rest API calls
     * @param password          Password for ICS Rest API calls
     */
    public RestUtilities(final String envBaseUrl, final String username, final String password) {
        this.username = username;
        this.password = password;
        this.envUrl = envBaseUrl;
    }

    /**
     * Simplified method to create header object with
     * content type - application/json.
     *
     * @return      HttpHeaders object
     */
    public HttpHeaders createHeaders() {
        return createHeaders(null);
    }

    /**
     * Method to return a header object with optional content type.
     *
     * @param additionalHeaders     Map of additional headers to be added
     * @return                      HttpHeaders object
     */
    public HttpHeaders createHeaders(final Map<String, String> additionalHeaders) {
        HttpHeaders hh  = new HttpHeaders();
        String auth = getUsername() + ":" + getPassword();
        byte[] unencodedAuth = auth.getBytes(Charset.forName("US-ASCII"));
        byte[] encodedAuth = Base64.encodeBase64(unencodedAuth);
        String authHeader = "Basic " + new String(encodedAuth);
        hh.set("Authorization", authHeader);
        if(additionalHeaders != null) {
            for (String key : additionalHeaders.keySet()) {
                hh.set(key, additionalHeaders.get(key));
            }
        }
        return hh;
    }



    /**
     * Used to invoke the RestTemplate and return the resulting entity.
     *
     * @param serviceUrl        Context root for the REST call
     * @param httpMethod        HttpMethod for REST call
     * @param body              (Optional) Body element with parameters
     * @param urlParams         (Optional) URL Parameters
     * @param additionalHeaders (Optional) Additional header tags
     * @return                  ResponseEntity response from rest call
     */
    public ResponseEntity<String> invokeService(final String serviceUrl, final HttpMethod httpMethod, final Object body, Map<String, String> urlParams, Map<String, String> additionalHeaders){
        getLog().info("[RestUtilities.invokeService] Invoking a REST Service");
        getLog().info("[RestUtilities.invokeService] Calling " + httpMethod + " on " + getEnvUrl() + serviceUrl);
        getLog().info("[RestUtilities.invokeService] Using params: " + urlParams);
        long tmr = System.currentTimeMillis();
        ResponseEntity<String> response;
        if (urlParams == null){
           urlParams = new HashMap<String, String>();
        }

        try{
            if (body == null) {
                HttpEntity<String> entity = new HttpEntity<String>(createHeaders(additionalHeaders));
                response = getRt().exchange(getEnvUrl() + serviceUrl, httpMethod, entity, String.class, urlParams);
            } else {
                HttpEntity<Object> entity = new HttpEntity<Object>(body, createHeaders(additionalHeaders));
                response = getRt().exchange(getEnvUrl() + serviceUrl, httpMethod, entity, String.class, urlParams);
            }
        } catch (HttpClientErrorException ex){
            // Catch the error, and return null, handle 404's from the caller.
//            getLog().error("Error: " + ex.getMessage(), ex);
            getLog().warn("[RestUtilities.invokeService] HttpError: " + ex.getMessage());
            return null;
        }
        getLog().info("[RestUtilities.invokeService] Service returned " + response.getStatusCode() + " in " + (System.currentTimeMillis() - tmr) + " ms.");

        return response;
    }

    /**
     * Accessor for a RestTemplate object.
     * @return        RestTemplate
     */
    public RestTemplate getRt() {
        if (rt == null) {
            getLog().info("[RestUtilities.getRt] checking Proxy properties");
            Properties props = System.getProperties();
            String proxyHost = props.getProperty("http.proxyHost");
            String proxyPort = props.getProperty("http.proxyPort");
            if(proxyHost==null || proxyHost.length()<1){
                // check environment
                Map<String, String> envVars = System.getenv();
                proxyHost = envVars.get("HTTP_PROXY_HOST");
                proxyPort = envVars.get("HTTP_PROXY_PORT");
            }

            if(proxyHost!=null && proxyHost.length()>0){
                // we have a proxy!
                getLog().info("[RestUtilities.getRt] Using Proxy: " + proxyHost + ":" + proxyPort);
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                Proxy proxy= new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
                requestFactory.setProxy(proxy);
                rt = new RestTemplate(requestFactory);
            } else {
                getLog().info("[RestUtilities.getRt] No proxy information set.  To enable this feature set the properties http.proxyHost / http.proxyPort or system variables HTTP_PROXY_HOST / HTTP_PROXY_PORT.");
                rt = new RestTemplate();
            }

        }
        return rt;
    }

    /**
     * Accessor for Username.
     * @return      Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Accessor for Password.
     * @return      Password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Accessor for Environment Url.
     * @return      Env Url
     */
    public String getEnvUrl() {
        return envUrl;
    }

    /**
     * Accessor for logger.
     * @return      Logger object
     */
    public Log getLog() {
        if(log==null){
            log = new SystemStreamLog();
        }
        return log;
    }

    /**
     * Setter for Log object.
     * @param log   A Logger
     */
    public void setLog(Log log) {
        this.log = log;
    }

//    public void printSystemVariables(){
//        getLog().info("*********************   System Properties Start  ****************************");
//        Properties props = System.getProperties();
//        Set<String> sysProps = props.stringPropertyNames();
//        for(String key : sysProps){
//            getLog().info("  [" + key + "] " + props.getProperty(key));
//        }
//        getLog().info("*********************   System Properties End    ****************************");
//        getLog().info("*********************   ENV Variables Start  ****************************");
//        Map<String, String> sysVars = System.getenv();
//        for(String key : sysVars.keySet()){
//            getLog().info("  [" + key + "] " + sysVars.get(key));
//        }
//        getLog().info("*********************   ENV Variables End     ****************************");
//    }
}
