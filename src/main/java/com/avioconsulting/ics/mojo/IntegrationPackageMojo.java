package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Integration;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;

@Mojo(name = "package", requiresProject = true)
public class IntegrationPackageMojo extends AbstractIntegrationMojo{
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Packaging integration " + integrationName + "_" + integrationVersion );
        try {
            Integration ii = new Integration(integrationName, integrationVersion, baseUrl, icsUser, icsPassword);
            ii.setLog(getLog());
            ii.packageProject();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new MojoExecutionException(ioe.getMessage());
        }
    }
}
