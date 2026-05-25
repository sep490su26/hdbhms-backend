package com.sep490.hdbhms.identityandaccess.infrastructure.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Slf4j
@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties(prefix = "redisson.bloom-filter")
public class BloomFilterConfig {
    String redisUrl;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress(redisUrl);
        return Redisson.create(config);
    }

    @Bean(name = "usernameBloomFilter")
    public RBloomFilter<String> usernameBloomFilter() {
        RBloomFilter<String> bloomFilter = redissonClient().getBloomFilter("usernameBF");
        bloomFilter.tryInit(1 << 25, 0.001);
        return bloomFilter;
    }

    @Bean(name = "emailBloomFilter")
    public RBloomFilter<String> emailBloomFilter() {
        RBloomFilter<String> bloomFilter = redissonClient().getBloomFilter("emailBF");
        bloomFilter.tryInit(1 << 25, 0.001);
        return bloomFilter;
    }
}
