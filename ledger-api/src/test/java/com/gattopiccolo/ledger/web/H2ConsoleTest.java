package com.gattopiccolo.ledger.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class H2ConsoleTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void h2ConsoleIsRegistered() {
        Map<String, ServletRegistrationBean> beans = context.getBeansOfType(ServletRegistrationBean.class);
        System.out.println("[DEBUG_LOG] Found " + beans.size() + " ServletRegistrationBeans:");
        beans.forEach((name, bean) -> System.out.println("[DEBUG_LOG] Bean: " + name + ", Servlet: " + bean.getServletName()));
        
        boolean found = beans.values().stream()
                .anyMatch(reg -> reg.getServletName().toLowerCase().contains("h2"));
        
        assertTrue(found, "H2 Console Servlet should be registered");
    }
}
