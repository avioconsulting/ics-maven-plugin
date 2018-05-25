package com.avioconsulting.ics.mojo;

import com.avioconsulting.ics.Replacement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mojo(name = "temp", requiresProject = false)
public class TempMojo extends AbstractIntegrationMojo {


    @Parameter
    private List<Replacement> replacements;

    @Parameter
    private List<String> includes;


    @Parameter
    private List<String> excludes;

    @Parameter
    private String outputDir;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting TempMojo");

        getLog().info("Found " + replacements.size() + " replacements");
        for(Replacement ment : replacements){
            getLog().info("Found replacement: " + ment.toString());
        }

        com.google.code
//        com.google.code.maven_replacer_plugin.Replacment r;
        for(String include : listIncludes(project.getBasedir().getAbsolutePath(), includes, excludes)){
            getLog().info("Included files: " + include);
        }
        getLog().info("Finished TempMojo");
    }


    public List<String> listIncludes(String basedir, List<String> includes, List<String> excludes){
        if (includes == null || includes.isEmpty()) {
            includes = getDefaultIncludes();
//            return Collections.emptyList();
        } else {
            getLog().info("Found " + includes.size() + " original filters.");
            includes.addAll(getDefaultIncludes());
            getLog().info("Added defaults, now found " + includes.size() + " filters.");
        }

        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.addDefaultExcludes();
        if (StringUtils.isNotBlank(basedir)) {
            directoryScanner.setBasedir(new File(basedir));
        }
        directoryScanner.setIncludes(stringListToArray(includes));
        directoryScanner.setExcludes(stringListToArray(excludes));

        directoryScanner.scan();
        return Arrays.asList(directoryScanner.getIncludedFiles());
    }

    private String[] stringListToArray(List<String> stringList) {
        if (stringList == null) {
            return null;
        }
        return stringList.toArray(new String[] {});
    }

    public List<String> getDefaultIncludes(){
        List<String> defaults = new ArrayList<String>();
        defaults.add("**/*.wsdl");
        defaults.add("**/*.xml");
        defaults.add("**/*.xsl");
        defaults.add("**/*.xsd");
        defaults.add("**/*.jca");
        return defaults;
    }

}
