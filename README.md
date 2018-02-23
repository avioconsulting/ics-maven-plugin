# ICS Maven Plugin
Maven plugin for interacting with Oracle Integration Cloud Service (ICS)

## Summary

This plugin helps adopt a development lifecycle when working with Oracle Integration Cloud Service (ICS).  

## Maven phases

This plugin defines a new packaging type (iar) and hooks into the Maven lifecycle at the following phases:

### intialize
* Copies property file into target directory and loads the property file

### generate-resources
* Runs the `export` goal if the `export` property is set to true. This will export the integration defined in the POM file.
* If the `connection` property is set (name of a connection), a template JSON config file will be created.

### package
* Runs the `package` goal. This goal generates an iar archive file.

## Maven goals

### activate
* Activates the integration
* Parameters:
| Name | Default Value | Description |
| ---- | ------------- | ----------- |
| enableTrace | false | Enables trace and audit payload |

	* enableTrace - Defaults to false - This enables trace and audit payload.

### deactivate
* Dectivates the integration

### export
* Exports the integration as an iar file
* Expands archive into standard project directory
* Parameters:
	* export - Defaults to false - Enables the export
	* expand - Defaults to true - Expands into standard project directory
	* clean - Defaults to false - Removes local src/main directories (except src/main/config) to be replaced by export
	* connection - Name of connection to export (must be used in conjection with export=true)
	* overwrite - Defaults to false - Replaces connection property file (must be used in conjection with connection)

### import

### importOnly

### package

### delete

### updateConnection

### deleteConnection


## Setup

### Building/installing

1. 

#### POM setup, example: document definitions

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.avioconsulting</groupId>
  <artifactId>INTEGRATION_ID</artifactId>
  <!-- version needs to be the integration version in format XX.XX.XXXX -->
  <version>01.00.0000</version>
  <description>ICS Integration</description>
  <packaging>iar</packaging>
  <properties>
    <ics.integration.name>${project.artifactId}</ics.integration.name>
    <ics.integration.version>${project.version}</ics.integration.version>
    <ics.importFile>${project.build.directory}/${project.artifactId}_${project.version}.iar</ics.importFile>
  </properties>
  <build>
    <plugins>
	  <!-- enforces -Denv property -->
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-enforcer-plugin</artifactId>
        <version>1.4</version>
        <executions>
          <execution>
            <id>enforce-property</id>
            <goals>
              <goal>enforce</goal>
            </goals>
            <configuration>
              <rules>
                <requireProperty>
                  <property>env</property>
                  <message>
------------------------------------------------------------------
       You must pass a value for the 'env' property!
       Example: mvn -Denv=DEV
------------------------------------------------------------------
                  </message>
                </requireProperty>
              </rules>
              <fail>true</fail>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <!-- copies property files to target directory -->
      <plugin>
        <artifactId>maven-resources-plugin</artifactId>
        <version>2.7</version>
        <configuration>
          <encoding>UTF-8</encoding>
        </configuration>
        <executions>
          <execution>
            <id>copy-project-properties</id>
            <phase>validate</phase>
            <goals>
              <goal>copy-resources</goal>
            </goals>
            <configuration>
              <outputDirectory>${project.basedir}/target</outputDirectory>
              <resources>
                <resource>
                  <directory>${project.basedir}/../../../env</directory>
                  <includes>
                    <include>*.properties</include>
                  </includes>
                  <filtering>true</filtering>
                </resource>
              </resources>
            </configuration>
          </execution>
        </executions>
      </plugin>
      <!-- reads/loads property files -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>properties-maven-plugin</artifactId>
        <version>1.0-alpha-2</version>
        <executions>
          <execution>
            <phase>initialize</phase>
            <goals>
              <goal>read-project-properties</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <files>
            <file>${project.basedir}/target/${env}.properties</file>
          </files>
        </configuration>
      </plugin>
      <!-- ics-maven-plugin plugin -->
      <plugin>
        <groupId>com.avioconsulting.maven</groupId>
        <artifactId>ics-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <extensions>true</extensions>
        <configuration />
      </plugin>
    </plugins>
    <filters>
      <filter>${project.basedir}/../../../env/${env}.properties</filter>
    </filters>
  </build>
  <dependencies/>
</project>
```

## Running

First export your project into an empty directory.

```
mvn generate-resources -Dexport=true -Denv=DEV
```

This will export the iar, and expand it into a standard project format (/src/main).

Import the integration:

```
mvn initialize ics:import -Denv=DEV
```

Connections need to be manually configured.  After exporting an integration, export a connection property file as follows:

```
mvn generate-resources -Denv=DEV -Dexport=true -Dconnection=CONNECTION_NAME
```

Update the property file, and put into the /src/main/resources/config folder.

