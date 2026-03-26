package com.powercess.printer_system.config;

import dev.gotenberg.GotenbergClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.client.support.RestClientAdapter;

/**
 * Gotenberg 客户端配置
 */
@Configuration
@RequiredArgsConstructor
public class GotenbergConfig {

    private final GotenbergProperties properties;

    @Bean
    public GotenbergClient gotenbergClient() {
        RestClient restClient = RestClient.builder()
            .baseUrl(properties.getUrl())
            .build();

        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(GotenbergClient.class);
    }
}