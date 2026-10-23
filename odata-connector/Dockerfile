ARG CAMUNDA_CONNECTORS_VERSION=0.23.2
FROM camunda/connectors:${CAMUNDA_CONNECTORS_VERSION}

# Spring Boot 3 expects "jackson", not the base image's Boot 4-style "jackson2".
ENV SPRING_HTTP_CONVERTERS_PREFERRED_JSON_MAPPER=jackson

COPY target/odata-*-with-dependencies.jar /opt/custom
