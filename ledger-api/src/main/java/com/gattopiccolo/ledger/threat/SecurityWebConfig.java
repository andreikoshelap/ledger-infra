package com.gattopiccolo.ledger.threat;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Attaches the security sensor to the account API. */
@Configuration
public class SecurityWebConfig implements WebMvcConfigurer {

    private final SecurityEventInterceptor sensor;
    private final SecurityProperties props;

    public SecurityWebConfig(SecurityEventInterceptor sensor, SecurityProperties props) {
        this.sensor = sensor;
        this.props = props;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (props.enabled()) {
            registry.addInterceptor(sensor).addPathPatterns("/api/accounts/**");
        }
    }
}
