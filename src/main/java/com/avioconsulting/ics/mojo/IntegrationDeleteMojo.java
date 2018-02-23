package com.avioconsulting.ics.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "delete", requiresProject = false)
public class IntegrationDeleteMojo extends AbstractIntegrationMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Deleting integration " + integrationName + "_" + integrationVersion + " from " + baseUrl);
        checkEnvProperties();
        getLog().error("This method has not yet been implemented.");
        throw new MojoExecutionException("This method has not yet been implemented.");
    }
}
