#!/bin/bash
CONF_PATH=release/src/main/resources/sample/config/
java -jar oc-sso-query/target/oc-sso-query-*.jar --spring.config.location=${CONF_PATH} --logging.config=file:${CONF_PATH}/logback-spring.xml --logging.file.path=${CONF_PATH}
