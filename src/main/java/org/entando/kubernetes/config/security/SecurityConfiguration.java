package org.entando.kubernetes.config.security;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.config.tenant.TenantFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.client.RestTemplate;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Import(SecurityProblemSupport.class)
@Slf4j
@RequiredArgsConstructor
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final String ADMIN = "ROLE_ADMIN";//From JHipster generated code.

    private final SecurityProblemSupport problemSupport;
    private final TenantFilter tenantFilter;
    private final Map<String, AuthenticationManager> authenticationManagers;

    @Override
    public void configure(HttpSecurity http) throws Exception {

        JwtIssuerAuthenticationManagerResolver authenticationManagerResolver =
                new JwtIssuerAuthenticationManagerResolver(issuer -> {
                    log.debug("Resolving authentication manager for issuer: '{}'", issuer);
                    return authenticationManagers.get(issuer);
                });

        // @formatter:off
        http
                .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class)
                .csrf()
                .disable()
                .exceptionHandling()
                .accessDeniedHandler(problemSupport)
                .and()
                .headers()
                .contentSecurityPolicy(
                        "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage"
                                + ".googleapis.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:")
                .and()
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
                .featurePolicy(
                        "geolocation 'none'; midi 'none'; sync-xhr 'none'; microphone 'none'; camera 'none'; magnetometer 'none'; "
                                + "gyroscope 'none'; speaker 'none'; fullscreen 'self'; payment 'none'")
                .and()
                .frameOptions()
                .deny()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/actuator/info").permitAll()
                .antMatchers("/actuator/prometheus").permitAll()
                .antMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
                .antMatchers("/actuator/**").hasAuthority(ADMIN)
                .antMatchers("/**").authenticated()
                .and()
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver))
                .oauth2Client();
        // @formatter:on
    }

    @Bean
    public RestTemplate simpleRestTemplate() {
        return new RestTemplate();
    }
}
