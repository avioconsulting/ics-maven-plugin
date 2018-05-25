package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Integration;
import com.avioconsulting.ics.Replacement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;
import java.util.List;

@Mojo(name = "package", requiresProject = true)
public class IntegrationPackageMojo extends AbstractIntegrationMojo{


    @Parameter(property = "basedir")
    private String projectDirectory;

    /**
     * Used to pass in token / value replacements
     */
    @Parameter
    private List<Replacement> replacements;

    /**
     * List of additional included files.  By default - [wsdl, xsd, xsl, xml, jca]
     */
    @Parameter
    private List<String> includes;

    /**
     * List of excluded files.
     */
    @Parameter
    private List<String> excludes;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Packaging integration " + integrationName + "_" + integrationVersion );
        try {
            Integration ii = new Integration(integrationName, integrationVersion, baseUrl, icsUser, icsPassword);
            ii.setLog(getLog());
            if(replacements!=null && replacements.size()>0){
                ii.packageProject(projectDirectory, true);
            } else {
                ii.packageProject(projectDirectory);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new MojoExecutionException(ioe.getMessage());
        }
    }
}
