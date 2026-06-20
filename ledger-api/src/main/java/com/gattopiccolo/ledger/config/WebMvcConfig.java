package com.gattopiccolo.ledger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Prefixes every {@link RestController} endpoint with {@code /api}. The Angular
 * dev server proxies {@code /api/**} straight through to this service without
 * rewriting the path (see ledger-web/proxy.conf.json), so the controllers must
 * live under {@code /api} for the front-end calls to land. The H2 console and
 * Spring's error endpoint are not annotated with @RestController and stay put.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", c -> c.isAnnotationPresent(RestController.class));
    }
}
