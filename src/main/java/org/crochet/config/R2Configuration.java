package org.crochet.config;

import org.crochet.properties.R2Properties;
import org.crochet.service.R2StorageClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(R2Properties.class)
public class R2Configuration {
    @Bean
    public Clock uploadClock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "close")
    public R2StorageClient r2Storage(R2Properties properties) {
        properties.validateEnvironment();
        if (!properties.isConfigured()) {
            return new R2StorageClient(properties, null, null);
        }
        properties.requireConfigured();
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
        var s3Configuration = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        var client = S3Client.builder().endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of("auto")).credentialsProvider(credentials).serviceConfiguration(s3Configuration)
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(5)).socketTimeout(Duration.ofSeconds(30)))
                .overrideConfiguration(builder -> builder.apiCallTimeout(Duration.ofMinutes(2))
                        .apiCallAttemptTimeout(Duration.ofSeconds(45)))
                .build();
        var presigner = S3Presigner.builder().endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of("auto")).credentialsProvider(credentials).serviceConfiguration(s3Configuration).build();
        return new R2StorageClient(properties, client, presigner);
    }
}
