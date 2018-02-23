package com.avioconsulting.ics.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

abstract class AbstractIntegrationMojo extends AbstractMojo {
    @Parameter(property = "ics.user", required = false)
    protected String icsUser;
    @Parameter(property = "ics.password", required = false)
    protected String icsPassword;
    @Parameter(property = "ics.baseUrl", required = false)
    protected String baseUrl;
    @Parameter(property = "ics.integration.name", required = true)
    protected String integrationName;
    @Parameter(property = "ics.integration.version", required = true)
    protected String integrationVersion;

    protected void checkEnvProperties() throws MojoExecutionException{
        if(icsUser == null || icsUser.length() < 1 ||
                icsPassword == null || icsPassword.length() < 1 ||
                baseUrl == null || baseUrl.length() < 1) {
            getLog().error("ERROR: Environment parameters are required. ");
            getLog().error("       Please include -Dics.user=XXX -Dics.password=XXX -Dics.baseUrl=XXX with your maven command");
            getLog().error("       Or include the 'initialize' phase before executing the goal directly.");
            getLog().error("       ie. mvn initialize com.avioconsulting.maven:ics-maven-plugin:activate -Denv=DEV");
            throw new MojoExecutionException("Additional parameters required.");
        }
    }
}
