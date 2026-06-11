#!/bin/sh
# Launch the Spring Boot application using the layered JarLauncher
exec java \
    -Xmx256m \
    -Xms128m \
    org.springframework.boot.loader.launch.JarLauncher