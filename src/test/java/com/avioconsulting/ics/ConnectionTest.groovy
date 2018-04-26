package com.avioconsulting.ics

import com.avioconsulting.ics.util.RestUtilities
import junit.framework.Assert
import org.apache.http.HttpStatus
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertThat
import org.mockito.Mock
import org.mockito.Mockito
import static org.mockito.Mockito.any
import static org.mockito.Mockito.eq
import static org.mockito.Mockito.when
import org.mockito.MockitoAnnotations
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class ConnectionTest extends groovy.util.GroovyTestCase {

    static String CONNECTION_ID = "AVIO_FTP"
    Connection conn

    @Mock
    RestUtilities util

    @Before
    void setUp() {
        MockitoAnnotations.initMocks(this)
        conn = new Connection(CONNECTION_ID, util)
    }

//    void testUpdateConnection() {
//        Assert.assertTrue(true)
//    }


    void testGetStatus_configured() {
        when(util.invokeService(any(), eq(HttpMethod.GET), any(), any(), any()))
                .thenReturn(getResponseConfigured())
        assertEquals(conn.getStatus(), "CONFIGURED")
    }

    void testGetStatus_unknown() {
        when(util.invokeService(any(), eq(HttpMethod.GET), any(), any(), any()))
                .thenReturn(getResponseNotFound())
        assertEquals(conn.getStatus(), "UNKNOWN")
    }

//    void testExportProperties() {
//        Assert.assertTrue(true)
//    }

    void testDeleteConnection_inUse() {
        when(util.invokeService(any(), eq(HttpMethod.DELETE), any(), any(), any()))
                .thenReturn(getResponse_inUse())
        try {
            conn.deleteConnection()
            // Should not reach here.
            Assert.fail("An exception should have been thrown.")
        } catch (Exception e){
            Assert.assertTrue(true)
        }


    }

    private ResponseEntity<String> getResponseConfigured(){
        File f = new File("src/test/resources/connections/AVIO_FTP_CONFIGURED.txt")
        ResponseEntity<String> res = new ResponseEntity<String>(f.text, HttpStatus.ACCEPTED)
        return res
    }

    private ResponseEntity<String> getResponse_inUse(){
        String details = "You are trying to delete the connection \"AVIO_FTP\" that is used by one or more integrations   (AVIO_FTP Integration | 1.0). First remove the connection from the integrations. Then you can delete the connection. [Cause: ICS-11040]"
        ResponseEntity<String> res = new ResponseEntity<String>(details, HttpStatus.ACCEPTED)
        return res
    }
    /*
    Not needed, return null
     */
    private ResponseEntity<String> getResponseNotFound(){
//        File f = new File("src/test/resources/connections/404_NOT_FOUND.txt")
//        ResponseEntity<String> res = new ResponseEntity<String>(f.text, HttpStatus.NOT_FOUND)
        return null
    }

}
