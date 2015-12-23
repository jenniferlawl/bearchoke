/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bearchoke.platform.server.frontend.service;

import com.bearchoke.platform.base.config.EncryptionConfig;
import com.bearchoke.platform.server.common.config.AppLocalConfig;
import com.bearchoke.platform.server.common.config.WebSecurityConfig;
import com.bearchoke.platform.server.common.web.config.WebMvcConfig;
import com.bearchoke.platform.server.frontend.config.FrontendAppConfig;
import com.bearchoke.platform.domain.user.config.SecurityConfig;
import com.bearchoke.platform.server.frontend.web.config.MockServerConfig;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;

/**
 * Created by Bjorn Harvold
 * Date: 9/19/14
 * Time: 1:47 AM
 * Responsibility:
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes =
        {
                MockServerConfig.class,
                AppLocalConfig.class
        }
)
@ActiveProfiles("local")
@TestExecutionListeners(listeners={
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        WithSecurityContextTestExecutionListener.class})
@Log4j2
public class GreetingServiceTests {

    @Inject
    private GreetingService greetingService;

    @Test(expected = AuthenticationCredentialsNotFoundException.class)
    public void testSecuredGreetingWhileUnauthenticated() {
        log.info("Expecting an exception here");
        greetingService.securedGreeting(1.0f);
    }

    @Test
    @WithMockUser(username="admin",roles={"USER","ADMIN"})
    public void testSecuredGreetingWhileAuthenticated() {
        log.info("Expecting no errors here");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info(authentication.toString());
        Greeting message = greetingService.securedGreeting(1.0f);
        assertNotNull(message);
    }

//    @Test
//    @WithUserDetails("user")
//    public void getMessageWithUserDetails() {
//        String message = messageService.getMessage();
//    }
}
