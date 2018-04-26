package com.avioconsulting.ics

import com.avioconsulting.ics.util.RestUtilities
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import static org.mockito.Mockito.any
import static org.mockito.Mockito.eq
import static org.mockito.Mockito.when
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.atLeast
import static org.mockito.Mockito.atMost
import static org.mockito.Mockito.times



class IntegrationTest extends GroovyTestCase {
    static String INTEGRATION_ID = "AVIO_FTP"
    static String INTEGRATION_VERSION = "01.00.0000"
    Integration integration

    @Mock
    RestUtilities util

    void setUp() {
        MockitoAnnotations.initMocks(this)
        integration = new Integration(INTEGRATION_ID, INTEGRATION_VERSION, util)
    }

    void testGetStatus_active() {
        when(util.invokeService(any(), eq(HttpMethod.GET), any(), any(), any()))
                .thenReturn(getResponseActivated())
        assertEquals("ACTIVATED", integration.getStatus())
    }

    void testGetStatus_configured() {
        when(util.invokeService(any(), eq(HttpMethod.GET), any(), any(), any()))
                .thenReturn(getResponseConfigured())
        assertEquals("CONFIGURED", integration.getStatus())
    }

    void testGetStatus_unknown() {
        when(util.invokeService(any(), eq(HttpMethod.GET), any(), any(), any()))
                .thenReturn(getResponseNotFound())
        assertEquals("UNKNOWN", integration.getStatus())
    }


    void testGetConnections_multiple() {
        when(util.invokeService(any(), eq(HttpMethod.GET), any(), any(), any()))
                .thenReturn(getResponseConfigured())
        Map<String, Connection> connections = integration.getConnections()
        assertNotNull(connections)

        assertEquals(3, connections.size())
    }

    void testDeleteIntegration_fail() {
        when(util.invokeService(any(), eq(HttpMethod.GET), any(), any(), any()))
                .thenReturn(getResponseNotFound())
        try{
            integration.delete(false)
            fail("Should have thrown an exception")
        } catch (Exception e){
            assertTrue(true)
        }
    }

    /**
     * Delete activated integration (no connections)
     */
    void testDeleteIntegration_activated() {
        when(util.invokeService(any(), eq(HttpMethod.GET), any(), any(), any()))
                .thenReturn(getResponseActivated())
        when(util.invokeService(any(), eq(HttpMethod.POST), any(), any(), any()))
                .thenReturn(getResponseSuccess())
        when(util.invokeService(any(), eq(HttpMethod.DELETE), any(), any(), any()))
                .thenReturn(getResponseSuccess())

        try{
            integration.delete(false)
        } catch (Exception e){
            fail(e.getMessage())
        }

        // get status
        verify(util, times(2)).invokeService(any(), eq(HttpMethod.GET), any(), any(), any())
        // deactivate
        verify(util).invokeService(any(), eq(HttpMethod.POST), any(), any(), any())
        // delete
        verify(util).invokeService(any(), eq(HttpMethod.DELETE), any(), any(), any())

    }

    /**
     * Delete integration and connections.
     */
    void testDeleteIntegration_withConnections() {
        when(util.invokeService(any(), eq(HttpMethod.GET), any(), any(), any()))
                .thenReturn(getResponseActivated(), getResponseActivated())
        when(util.invokeService(any(), eq(HttpMethod.POST), any(), any(), any()))
                .thenReturn(getResponseSuccess())
        when(util.invokeService(any(), eq(HttpMethod.DELETE), any(), any(), any()))
                .thenReturn(getResponseSuccess())

        try{
            integration.delete(true)
        } catch (Exception e){
            e.printStackTrace()
            fail(e.getMessage())
        }


        // get status, get connections
        verify(util, times(2)).invokeService(any(), eq(HttpMethod.GET), any(), any(), any())
        // deactivate
        verify(util).invokeService(any(), eq(HttpMethod.POST), any(), any(), any())
        // delete integration, delete connection
        verify(util, times(2)).invokeService(any(), eq(HttpMethod.DELETE), any(), any(), any())

    }

    private ResponseEntity<String> getResponseActivated(){
        println "Returning response activated."
        File f = new File("src/test/resources/integrations/AVIO_FTP_INTEGRATION_ACTIVATED.txt")
        ResponseEntity<String> res = new ResponseEntity<String>(f.text, HttpStatus.ACCEPTED)
        return res
    }

    private ResponseEntity<String> getResponseConfigured(){
        println "Returning response configured."
        File f = new File("src/test/resources/integrations/AVIO_FTP_INTEGRATION_CONFIGURED.txt")
        ResponseEntity<String> res = new ResponseEntity<String>(f.text, HttpStatus.ACCEPTED)
        return res
    }

    private ResponseEntity<String> getResponseConnections(){
        println "Returning response configured."
        File f = new File("src/test/resources/integrations/AVIO_FTP_CONFIGURED.txt")
        ResponseEntity<String> res = new ResponseEntity<String>(f.text, HttpStatus.ACCEPTED)
        return res
    }

//    private ResponseEntity<String> getResponse_inUse(){
//        String details = "You are trying to delete the connection \"AVIO_FTP\" that is used by one or more integrations   (AVIO_FTP Integration | 1.0). First remove the connection from the integrations. Then you can delete the connection. [Cause: ICS-11040]"
//        ResponseEntity<String> res = new ResponseEntity<String>(details, HttpStatus.ACCEPTED)
//        return res
//    }

    /*
    Not needed, return null
     */
    private ResponseEntity<String> getResponseNotFound() {
        println "Returning response not found."
        return null
    }

    private ResponseEntity<String> getResponseSuccess() {
        println "Returning response success."
        ResponseEntity<String> res = new ResponseEntity<String>(HttpStatus.OK)
        return res
    }
}
