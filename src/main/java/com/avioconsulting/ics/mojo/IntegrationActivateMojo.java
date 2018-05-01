package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Integration;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Activates an existing Integration
 */
@Mojo(name = "activate", requiresProject = false)
public class IntegrationActivateMojo extends AbstractIntegrationMojo{
    @Parameter(property = "enableTrace", defaultValue = "true")
    private boolean enableTrace;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("[Mojo:activate] Activating integration " + integrationName + "_" + integrationVersion + " from " + baseUrl);
        checkEnvProperties();

        if(enableTrace){
            getLog().info("[Mojo:activate] EnableTrace is set to true, set -DenableTrace=false to disable it.");
        }
        Integration ii = new Integration(integrationName, integrationVersion, baseUrl, icsUser, icsPassword);
        ii.setLog(getLog());
        ii.activate(enableTrace);

    }
}
