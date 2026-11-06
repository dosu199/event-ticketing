package com.ticketing.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(BookingProperties.class)
public class AppConfig {

    /** "Now" as a bean, so tests can control time. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * HTTP client for event-service. Explicit timeouts matter in microservices: without
     * them, one slow service makes every caller hang and the slowness spreads.
     * Phase 6 adds retries and a circuit breaker (Resilience4j) on top.
     */
    @Bean
    RestClient eventServiceRestClient(@Value("${services.event-service.url}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
