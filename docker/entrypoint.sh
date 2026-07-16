#!/bin/sh
# Launch the Spring Boot application using the layered JarLauncher
: "${SPRING_PROFILES_ACTIVE:?SPRING_PROFILES_ACTIVE must be set explicitly (dev, stag, or prod)}"

exec java \
    -Xmx256m \
    -Xms128m \
    org.springframework.boot.loader.launch.JarLauncher
