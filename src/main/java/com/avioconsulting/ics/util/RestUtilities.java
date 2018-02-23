package com.avioconsulting.ics.util;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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
     * @return                  ResponseEntity<String> response
     */
    // Changed from MultiValueMap<String, Object> to 'Object'
    public ResponseEntity<String> invokeService(final String serviceUrl, final HttpMethod httpMethod, final Object body, Map<String, String> urlParams, Map<String, String> additionalHeaders){
        getLog().info("[RestUtilities.invokeService] Invoking REST Service");
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
            getLog().error("Error: " + ex.getMessage(), ex);

            return null;
        }
        getLog().info("[RestUtilities.invokeService] Calling " + httpMethod + " on " + getEnvUrl() + serviceUrl);
        getLog().info("[RestUtilities.invokeService] Using params: " + urlParams);
        getLog().info("[RestUtilities.invokeService] Service returned " + response.getStatusCode() + " in " + (System.currentTimeMillis() - tmr) + " ms.");

        return response;
    }

//    HttpHeaders createHeadersWithoutContent(String username, String password){
//        return new HttpHeaders() {{
//            String auth = username + ":" + password;
//        byte[] unencodedAuth = auth.getBytes(Charset.forName("US-ASCII"));
//        byte[] encodedAuth = Base64.encodeBase64(unencodedAuth);
//            String authHeader = "Basic " + new String( encodedAuth );
//            set( "Authorization", authHeader );
////            set( "Content-Type", "multipart/form-data");
//
//        }};
//    }

    /**
     * Accessor for a RestTemplate object.
     * @return        RestTemplate
     */
    public RestTemplate getRt() {
        if (rt == null) {
            rt = new RestTemplate();
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
}
