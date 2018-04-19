package com.avioconsulting.ics.mojo;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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



    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    @Parameter(property = "connection.config.dir", required = true, defaultValue = "/src/main/resources/config")
    protected String connectionConfigDir;

    // Below for 'copy-resources' functionality

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    @Component( role = MavenResourcesFiltering.class, hint = "default" )
    protected MavenResourcesFiltering mavenResourcesFiltering;

    @Parameter( defaultValue = "${project.build.filters}", readonly = true )
    protected List<String> buildFilters;

    @Parameter( defaultValue = "true" )
    protected boolean useBuildFilters;

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

    /**
     * TODO: Refactor this not to use the MavenResourceFiltering, and just use a DirectoryScanner + CopyDirectoryWithScanner
     *
     * @param sourceDirectory
     * @param destinationDirectory
     * @throws MavenFilteringException
     */
    protected void copyConfigFiles(String sourceDirectory, String destinationDirectory) throws MavenFilteringException {
        // Build resources
        ArrayList<Resource> resources = new ArrayList<Resource>();
        Resource resource = new Resource();
        resource.setDirectory(sourceDirectory);
        ArrayList<String> includes = new ArrayList<String>();
        includes.add("*.json");
        resource.setIncludes(includes);
        resource.setFiltering(true);
        resources.add(resource);

        //output directory
        File outputDir = new File(destinationDirectory);
        if(!outputDir.exists()){
            new File(outputDir.getParent()).mkdirs();
        }

        // encoding
        String encoding = ReaderFactory.FILE_ENCODING;

        //List<String> nonFilteredFileExtensions
        List<String> nonFilteredFileExtensions = Collections.<String>emptyList();

        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution( resources, outputDir, project, encoding, buildFilters, nonFilteredFileExtensions, session );

        mavenResourcesFiltering.filterResources( mavenResourcesExecution );

        List<MavenResourcesFiltering> mavenFilteringComponents = new ArrayList<MavenResourcesFiltering>();

        for ( MavenResourcesFiltering filter : mavenFilteringComponents )
        {
            filter.filterResources( mavenResourcesExecution );
        }
    }
}
