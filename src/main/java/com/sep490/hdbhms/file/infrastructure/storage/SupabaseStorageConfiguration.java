package com.sep490.hdbhms.file.infrastructure.storage;

import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "app.file.storage", name = "provider", havingValue = "supabase")
public class SupabaseStorageConfiguration {

    @Bean
    public HttpClient supabaseHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public SupabaseStorageSettings supabaseStorageSettings(FileProperties fileProperties) {
        FileProperties.Supabase supabase = fileProperties.getStorage().getSupabase();
        requireConfigured(supabase.getUrl(), "app.file.storage.supabase.url");
        requireConfigured(supabase.getBucket(), "app.file.storage.supabase.bucket");
        requireConfigured(supabase.getServiceRoleKey(), "app.file.storage.supabase.service-role-key");
        return new SupabaseStorageSettings(supabase.getUrl(), supabase.getBucket(), supabase.getServiceRoleKey());
    }

    private static void requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured when Supabase storage is enabled");
        }
    }

    public record SupabaseStorageSettings(String url, String bucket, String serviceRoleKey) {
    }
}
