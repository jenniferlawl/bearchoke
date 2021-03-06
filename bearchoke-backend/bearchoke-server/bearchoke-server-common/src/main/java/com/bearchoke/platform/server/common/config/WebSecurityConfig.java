/*
 * Copyright (c) 2015. Bearchoke
 */

package com.bearchoke.platform.server.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.bearchoke.platform.server.common.security.ApiAuthenticationFailureHandler;
import com.bearchoke.platform.server.common.security.ApiAuthenticationSuccessHandler;
import com.bearchoke.platform.server.common.security.ApiRequestHeaderAuthenticationFilter;
import com.bearchoke.platform.server.common.security.UnauthorizedEntryPoint;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.inject.Inject;
import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bjorn Harvold
 * Date: 1/7/14
 * Time: 9:44 PM
 * Responsibility:
 */
@Configuration
@EnableWebSecurity
@Log4j2
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String API_ADMINISTRATION_URL = "/api/administration/**";
    private static final String API_MANAGER_URL = "/api/manager/**";
    private static final String API_USER_URL = "/api/secured/**";
    private static final String WEBSOCKET_URL = "/ws/*";
    private static final String API_LOGIN_URL = "/api/authenticate";
    private static final String API_PUBLIC_URL = "/*";

    @Inject
    private ApiAuthenticationSuccessHandler apiAuthenticationSuccessHandler;

    @Inject
    private ApiAuthenticationFailureHandler apiAuthenticationFailureHandler;

    @Inject
    @Qualifier("authenticationProvider")
    private AuthenticationProvider authenticationProvider;

    @Inject
    @Qualifier("preAuthAuthenticationManager")
    private AuthenticationManager preAuthAuthenticationManager;

    @Inject
    private ObjectMapper objectMapper;

    /**
     * Commons url security strategy
     *
     * @param http
     * @throws Exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("Configuring springSecurityFilterChain...");

        // header details
        http
                .headers()
                .frameOptions()
                .sameOrigin()
                .cacheControl()
                .and().xssProtection()
                .and().contentTypeOptions()
                .and().httpStrictTransportSecurity();

        http
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // the url patterns to secure
        http
                .authorizeRequests()
                .antMatchers(API_ADMINISTRATION_URL).hasRole("ADMIN")
                .antMatchers(API_MANAGER_URL).hasRole("MANAGER")
                .antMatchers(API_USER_URL).hasRole("USER")
                .antMatchers(API_LOGIN_URL).anonymous()
                .antMatchers(API_PUBLIC_URL).permitAll()
        ;

        // new logout handler from spring 4.0.2 - maybe nice to have. Has not been tested yet.
//        HttpStatusReturningLogoutSuccessHandler logoutHandler = new HttpStatusReturningLogoutSuccessHandler();
//        http
//                .logout().logoutSuccessHandler(logoutHandler);

        // filter details
        http
                .addFilterAfter(authFilter(), ApiRequestHeaderAuthenticationFilter.class)
                .addFilter(preAuthFilter());
        http
                .csrf().disable();

        http
                .exceptionHandling().authenticationEntryPoint(new UnauthorizedEntryPoint(objectMapper));
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .antMatchers(HttpMethod.GET, "/resources/*");
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authenticationProvider);
    }

    @Bean(name = "authFilter")
    public Filter authFilter() throws Exception {
        log.info("Creating authFilter...");

        RequestMatcher antReqMatch = new AntPathRequestMatcher(API_LOGIN_URL);

        List<RequestMatcher> reqMatches = new ArrayList<>();
        reqMatches.add(antReqMatch);
        RequestMatcher reqMatch = new AndRequestMatcher(reqMatches);

        UsernamePasswordAuthenticationFilter filter = new UsernamePasswordAuthenticationFilter();
        filter.setPostOnly(true);
        filter.setUsernameParameter(USERNAME);
        filter.setPasswordParameter(PASSWORD);
        filter.setRequiresAuthenticationRequestMatcher(reqMatch);
        filter.setAuthenticationSuccessHandler(apiAuthenticationSuccessHandler);
        filter.setAuthenticationFailureHandler(apiAuthenticationFailureHandler);
        filter.setAuthenticationManager(authenticationManager());

        return filter;
    }

    @Bean(name = "preAuthFilter")
    public Filter preAuthFilter() {
        log.info("Creating preAuthFilter...");
        ApiRequestHeaderAuthenticationFilter filter = new ApiRequestHeaderAuthenticationFilter();
        filter.setAuthenticationManager(preAuthAuthenticationManager);
        return filter;
    }


//
//    @Bean
//    public AccessDecisionManager accessDecisionManager() {
//        List<AccessDecisionVoter> voters = new ArrayList<>(1);
////        voters.add(new RoleVoter());
////        voters.add(new AuthenticatedVoter());
//        voters.add(new WebExpressionVoter());
//
//        AffirmativeBased ab = new AffirmativeBased(voters);
//        ab.setAllowIfAllAbstainDecisions(true);
//
//        return ab;
//    }

}
