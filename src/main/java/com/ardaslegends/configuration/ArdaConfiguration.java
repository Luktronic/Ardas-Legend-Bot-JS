package com.ardaslegends.configuration;


import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
public class ArdaConfiguration {
    @Bean
    public ExecutorService virtualExecutorService() {
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        log.info("Initialized virtualExecutorService");
        return executor;
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .build();
    }

    @Bean
    public UrlValidator urlValidator() {
        return new UrlValidator();
    }
}
