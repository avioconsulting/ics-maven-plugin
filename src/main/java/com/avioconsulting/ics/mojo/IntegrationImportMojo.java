package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Connection;
import com.avioconsulting.ics.Integration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mojo(name = "import", requiresProject = true)
public class IntegrationImportMojo extends AbstractIntegrationMojo {
    @Parameter(property = "enableTrace", defaultValue = "false")
    private boolean enableTrace;
    @Parameter(property = "importFile", required = false)
    private String importFile;



    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Importing integration " + integrationName + "_" + integrationVersion + " to " + baseUrl);
        checkEnvProperties();
        String projectDirectory = project.getBasedir().getAbsolutePath();
        try {
            Integration ii = new Integration(integrationName, integrationVersion, baseUrl, icsUser, icsPassword);
            ii.setLog(getLog());
            if (importFile == null || importFile.length() < 1) {
                importFile = projectDirectory + "/target/" + integrationName + "_" + integrationVersion + ".iar";
            }
            ii.importIntegration(importFile);

            Map<String, Connection> conns = ii.getConnections();
            for(String key : conns.keySet()){
                Connection c = conns.get(key);
                c.setConfigDirectory(projectDirectory + "/target/connections");
                String status = c.getStatus();
                if(status.equalsIgnoreCase("CONFIGURED")) {
                    getLog().info("Connection " + key + " has status " + status + " ignoring.");
                } else {
                    //TODO: just copy the one connection file?

                    copyConfigFiles(projectDirectory + connectionConfigDir, projectDirectory + "/target/connections");
//                copyConfigFiles(projectDirectory + "/src/main/resources/config", projectDirectory + "/target/connections");
                    c.updateConnection();
                }
            }

            ii.activate(enableTrace);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage());
        }

    }



}
