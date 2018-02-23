package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Integration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "deactivate", requiresProject = false)
public class IntegrationDeactivateMojo extends AbstractIntegrationMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Deactivating integration " + integrationName + "_" + integrationVersion + " from " + baseUrl);
        checkEnvProperties();

        Integration ii = new Integration(integrationName, integrationVersion, baseUrl, icsUser, icsPassword);
        ii.setLog(getLog());
        ii.deactivate();
    }
}
