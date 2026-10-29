package com.radusclifos.questlog.integration.igdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IgdbProperties.class)
public class IgdbConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "questlog.igdb", name = "enabled", havingValue = "true")
    static class Enabled {
        @Bean
        TwitchAppTokenProvider twitchAppTokenProvider(IgdbProperties properties) {
            return new TwitchAppTokenProvider(client(properties, "https://id.twitch.tv/oauth2/"),
                    properties, Clock.systemUTC());
        }

        @Bean
        IgdbClient igdbClient(IgdbProperties properties, TwitchAppTokenProvider tokens) {
            return new IgdbClient(client(properties, "https://api.igdb.com/v4/"),
                    properties, tokens);
        }

        @Bean
        @ConditionalOnProperty(prefix = "questlog.igdb", name = "verify-on-startup", havingValue = "true")
        ApplicationRunner igdbAuthenticationCheck(IgdbClient client) {
            return args -> {
                client.verifyAuthentication();
                log.info("IGDB authentication verified successfully; no games imported.");
            };
        }

        private static RestClient client(IgdbProperties properties, String baseUrl) {
            var requests = new SimpleClientHttpRequestFactory();
            requests.setConnectTimeout(properties.connectTimeout());
            requests.setReadTimeout(properties.readTimeout());
            return RestClient.builder().baseUrl(baseUrl).requestFactory(requests).build();
        }
    }

}
