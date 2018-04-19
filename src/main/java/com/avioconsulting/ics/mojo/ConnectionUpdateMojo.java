package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Connection;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "updateConnection", requiresProject = true)
public class ConnectionUpdateMojo extends AbstractIntegrationMojo{
    @Parameter(property = "connection", required = true)
    private String connection;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Updating Connection:" + connection + " for integration " + integrationName + "_" + integrationVersion + " from " + baseUrl);
        checkEnvProperties();
        Connection conn = new Connection(connection, baseUrl, icsUser, icsPassword);
        conn.setLog(getLog());
        String projectDirectory = project.getBasedir().getAbsolutePath();
        try {
            //TODO add copy prop file to /target/config/ with filtering.
            // Update - just copy the one file... for now lets copy them all
            copyConfigFiles(projectDirectory + connectionConfigDir, projectDirectory + "/target/connections");
            conn.setConfigDirectory(projectDirectory + "/target/connections");
            conn.updateConnection();
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
