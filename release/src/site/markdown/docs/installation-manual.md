# Installation manual

The OC SSO Query Service uses:

- OpenJDK 21
- Spring Boot 4

## OpenJDK 21

The version of Java used is OpenJDK 21.x.

### Environment specific configuration files

The environment specific (configuration) files should be managed/set by the owner of the environment.

We ship a sample config-templates file contain sample files. These sample files contains fake data and have to
changed/set.

#### application.properties

The application environment uses an application.properties file. A sample configuration file is shipped in the application.

### Running the application

A run.sh bash script is added to the root of this project to easily start the (compiled) application. This files has the
following content:

    #!/bin/bash
    CONF_PATH=release/src/main/resources/sample/config/
    java -jar oc-sso-query/target/oc-sso-query-*.jar --spring.config.location=${CONF_PATH} --logging.config=file:${CONF_PATH}/logback-spring.xml --logging.file.path=${CONF_PATH}
