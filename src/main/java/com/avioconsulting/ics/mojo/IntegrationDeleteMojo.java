package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Integration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "delete", requiresProject = false)
public class IntegrationDeleteMojo extends AbstractIntegrationMojo {
    @Parameter(property = "removeConnections", defaultValue = "false")
    private boolean removeConnections;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Deleting integration " + integrationName + "_" + integrationVersion + " from " + baseUrl);
        checkEnvProperties();
//        getLog().error("This method has not yet been implemented.");
//        throw new MojoExecutionException("This method has not yet been implemented.");

        Integration ii = new Integration(integrationName, integrationVersion, baseUrl, icsUser, icsPassword);
        ii.setLog(getLog());
        try {
            if(!removeConnections) {
                getLog().info("[Mojo:delete] NOT removing connections. Set -DremoveConnections=true to remove connections for this integration.");
            }
            ii.delete(removeConnections);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
