package com.sep490.hdbhms.file.infrastructure.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "app.file")
public class FileProperties {
    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Download {
        String prefix;
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Temp {
        String directory;
    }

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Storage {
        @Getter
        @Setter
        @FieldDefaults(level = AccessLevel.PRIVATE)
        public static class Retry {
            int maxAttempts;
            long initialDelay;
            int multiplier;
            long timeOut;
        }

        String directory;
        int maxConcurrency;
        int maxBatchSize;
        Retry retry = new Retry();
    }

    final Download download = new Download();
    final Temp temp = new Temp();
    final Storage storage = new Storage();
}
