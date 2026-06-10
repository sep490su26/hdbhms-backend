#!/bin/sh
# Create the Kafka truststore at the location expected by the application
TRUSTSTORE_PATH="/application/certs/kafka.truststore.jks"

if [ ! -f "$TRUSTSTORE_PATH" ]; then
    echo "Creating Kafka truststore..."
    keytool -importcert \
        -alias aiven-ca \
        -file /application/certs/ca.pem \
        -keystore "$TRUSTSTORE_PATH" \
        -storepass "$KAFKA_TRUSTSTORE_PASSWORD" \
        -noprompt
fi

# Launch the Spring Boot application using the layered JarLauncher
exec java \
    -Xmx256m \
    -Xms128m \
    org.springframework.boot.loader.launch.JarLauncher