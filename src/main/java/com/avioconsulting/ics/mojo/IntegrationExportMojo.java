package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Connection;
import com.avioconsulting.ics.Integration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;

@Mojo(name = "export", requiresProject = false)
public class IntegrationExportMojo extends AbstractIntegrationMojo {

    @Parameter(property = "export", defaultValue = "false")
    private boolean doExport;
    @Parameter(property = "expand", defaultValue = "true")
    private boolean doExpand;
    @Parameter(property = "clean", defaultValue = "false")
    private boolean cleanFirst;
    @Parameter(property = "connection")
    private String connection;
    @Parameter(property = "overwrite", defaultValue = "false")
    private boolean overwrite;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        checkEnvProperties();
        String projectDirectory = project.getBasedir().getAbsolutePath();

        if (!doExport) {
            getLog().info("Skipping export, use -Dexport=true to enable it");
            getLog().info("                 use -Dconnection=CONNECTION_NAME to export a connection.");
            return;
        }

        try {
            if(connection == null || connection.length() < 0) {
                getLog().info("Exporting integration " + integrationName + "_" + integrationVersion + " from " + baseUrl);
                // Integration Export
                if (doExpand) {
                    getLog().info("Expanding ICS export, use -Dexpand=false to disable it.  Optionally add -Dclean=true to remove existing files before expanding.");
                }
                Integration ii = new Integration(integrationName, integrationVersion, baseUrl, icsUser, icsPassword);
                ii.setLog(getLog());
                ii.exportIntegration(doExpand, cleanFirst, projectDirectory);
            } else {
                getLog().info("Exporting connection " + connection + " from " + baseUrl);
                // Connection Export
                Connection conn = new Connection(connection, baseUrl, icsUser, icsPassword);
                conn.setLog(getLog());
                conn.exportProperties(overwrite);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new MojoExecutionException(ioe.getMessage());
        }

    }
}
