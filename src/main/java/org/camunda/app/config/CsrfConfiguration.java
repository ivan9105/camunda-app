package org.camunda.app.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;

@Configuration
public class CsrfConfiguration {
    private static final String CSRF_PREVENTION_FILTER = "CsrfPreventionFilter";
    /**
     * Overwrite csrf filter from Camunda configured here
     * org.camunda.bpm.spring.boot.starter.webapp.CamundaBpmWebappInitializer
     * org.camunda.bpm.spring.boot.starter.webapp.filter.SpringBootCsrfPreventionFilter
     * Is configured with basically a 'no-op' filter
     */
    @Bean
    public ServletContextInitializer csrfOverwrite() {
        return servletContext -> {
            Filter filter = (request, response, chain) -> chain.doFilter(request, response);
            servletContext.addFilter(CSRF_PREVENTION_FILTER, filter);
        };
    }
}
