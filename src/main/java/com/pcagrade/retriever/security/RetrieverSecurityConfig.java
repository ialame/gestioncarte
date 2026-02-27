package com.pcagrade.retriever.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.*;

@Configuration
@EnableMethodSecurity
public class RetrieverSecurityConfig {

    private static final Logger LOGGER = LogManager.getLogger(RetrieverSecurityConfig.class);

    @Value("${retriever.security.login.enabled:true}")
    private boolean loginEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, Environment environment) throws Exception {
        if (!loginEnabled) {
            if (Arrays.asList(environment.getActiveProfiles()).contains("production")) {
                throw new IllegalStateException("Production profile requires authentication to be setup");
            }
            LOGGER.warn("Professional Card Retriever is running in insecure mode. Please configure a user and password to enable authentication.");
            return http.authorizeHttpRequests(r -> r.requestMatchers("/**").permitAll())
                    .csrf(AbstractHttpConfigurer::disable)
                    .build();
        }

        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
//                .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt(jwt -> jwt
//                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
//                        )
//                )
                .authorizeHttpRequests(r -> r
                        // CHANGE FROM hasRole TO hasAuthority
                        .requestMatchers(HttpMethod.DELETE, "/api/cards/pokemon/extracted/**").hasAnyAuthority("RET_ADMIN", "RET_EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/api/cards/pokemon/**").hasAuthority("RET_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cards/onepiece/extracted/**").hasAnyAuthority("RET_ADMIN", "RET_EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/api/cards/onepiece/**").hasAuthority("RET_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cards/lorcana/extracted/**").hasAnyAuthority("RET_ADMIN", "RET_EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/api/cards/lorcana/**").hasAuthority("RET_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/yugioh/extracted/**").hasAnyAuthority("RET_ADMIN", "RET_EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/api/yugioh/**").hasAuthority("RET_ADMIN")
                        .requestMatchers("/actuator/**").hasAuthority("RET_ADMIN")
                        .requestMatchers("/login*", "/oauth/**").hasAnyAuthority("RET_ADMIN", "RET_EMPLOYEE")
                        // Only users with RET_ADMIN or RET_EMPLOYEE roles can access the app after login
                        .anyRequest().hasAnyAuthority("RET_ADMIN", "RET_EMPLOYEE")
                )
                .oauth2Login(o -> o
                        .loginPage("/oauth2/authorization/authentik")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(this.oidcUserService())
                        )
                )
                .oauth2Client(Customizer.withDefaults())
                .logout(Customizer.withDefaults())
                .exceptionHandling(h -> h.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/**")))
                .build();
    }

//    @Bean
//    public JwtAuthenticationConverter jwtAuthenticationConverter() {
//        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
//
//        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
//            Set<GrantedAuthority> authorities = new HashSet<>();
//            authorities.add(new SimpleGrantedAuthority("OIDC_USER"));
//
//            List<String> groups = jwt.getClaim("groups");
//            if (groups != null) {
//                for (String group : groups) {
//                    authorities.add(new SimpleGrantedAuthority(group));
//                }
//            }
//
//            LOGGER.info("JWT Authorities: {}", authorities);
//            return authorities;
//        });
//
//        return converter;
//    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            Set<GrantedAuthority> authorities = new HashSet<>();
            authorities.add(new SimpleGrantedAuthority("OIDC_USER"));

            // Extract groups from claims
            List<String> groups = oidcUser.getClaim("groups");
            if (groups != null) {
                for (String group : groups) {
                    // Add group as authority
                    authorities.add(new SimpleGrantedAuthority(group));
                    // Also add with ROLE_ prefix for hasRole() checks
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + group));
                }
            }

            LOGGER.info("OIDC User Authorities: {}", authorities);
            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }
}
