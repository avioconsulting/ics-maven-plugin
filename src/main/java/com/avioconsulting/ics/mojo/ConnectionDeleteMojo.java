package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Connection;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deleteConnection", requiresProject = true)
public class ConnectionDeleteMojo extends AbstractIntegrationMojo{
    @Parameter(property = "connection", required = true)
    private String connection;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Deleting Connection:" + connection + " from " + baseUrl);
        checkEnvProperties();
        Connection conn = new Connection(connection, baseUrl, icsUser, icsPassword);
        conn.setLog(getLog());

        try {
            conn.deleteConnection();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage());
        }
    }
}