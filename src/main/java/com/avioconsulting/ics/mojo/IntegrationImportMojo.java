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
import org.apache.maven.project.MavenProject;
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
    @Parameter(property = "enableTrace", defaultValue = "true")
    private boolean enableTrace;
    @Parameter(property = "importFile", required = false)
    private String importFile;

    @Parameter(property = "basedir")
    private String projectDirectory;

    // Below for 'copy-resources' functionality
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    protected MavenProject project;

    @Parameter( defaultValue = "${session}", readonly = true, required = true )
    protected MavenSession session;

    @Component( role = MavenResourcesFiltering.class, hint = "default" )
    protected MavenResourcesFiltering mavenResourcesFiltering;

    @Parameter( defaultValue = "${project.build.filters}", readonly = true )
    protected List<String> buildFilters;

    @Parameter( defaultValue = "true" )
    protected boolean useBuildFilters;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Importing integration " + integrationName + "_" + integrationVersion + " to " + baseUrl);
        checkEnvProperties();

        try {
            Integration ii = new Integration(integrationName, integrationVersion, baseUrl, icsUser, icsPassword);
            ii.setLog(getLog());
            if (importFile == null || importFile.length() < 1) {
                importFile = projectDirectory + "target/" + integrationName + "_" + integrationVersion + ".iar";
            }
            ii.importIntegration(importFile);

            Map<String, Connection> conns = ii.getConnections();
            for(String key : conns.keySet()){
                Connection c = conns.get(key);
                String status = c.getStatus();
                getLog().info("Connection " + key + " has status " + status);
                copyConfigFiles("src/main/resources/config", "target/connections");
                c.updateConnection();
            }

            ii.activate(enableTrace);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException(e.getMessage());
        }

    }

    private void copyConfigFiles(String sourceDirectory, String destinationDirectory) throws MavenFilteringException {
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
