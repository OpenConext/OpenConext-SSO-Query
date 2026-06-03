/*
 * Copyright 2021, Stichting Kennisnet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.kennisnet.services.web.controller;

import com.google.common.collect.Lists;
import nl.kennisnet.services.web.config.CacheConfig;
import nl.kennisnet.services.web.service.WhitelistRestProvider;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SsoQueryControllerTest {

    public static final String SSO_COOKIE_NAME = "sso_id";
    private MockMvc mvc;

    private static final String SSO_QUERY_URL = "/sso/ssoquery";

    @InjectMocks
    private SsoQueryController controller;

    @Mock
    private WhitelistRestProvider whitelistProvider;

    @Mock
    private CacheConfig cacheConfig;

    @BeforeEach
    public void setUp() {
        this.mvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(whitelistProvider.getWhitelist())
                .thenReturn(Lists.newArrayList("https://testapplicatie.kennisnet.nl", "https://*.kennisnet.nl"));
    }

    @Test
    public void testDataFetchError() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl"));

        when(whitelistProvider.getWhitelist())
                .thenReturn(Lists.newArrayList());

        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://testapplicatie.kennisnet.nl?result=false"));

        verify(cacheConfig, times(1)).cacheEvict();
    }

    @Test
    public void testNoData() throws Exception {
        when(whitelistProvider.getWhitelist())
                .thenReturn(Lists.newArrayList());

        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testJsonFormat() throws Exception {
        testJsonResponse("JSON");
    }

    @Test
    public void testJsonCaseInsensitiveFormat() throws Exception {
        testJsonResponse("json");
    }

    @Test
    public void testNoFormat() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://testapplicatie.kennisnet.nl?result=false"));
    }

    @Test
    public void testNotJsonFormat() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL).param(SsoQueryController.PARAM_FORMAT, "text")
                .param("response_url", "https://testapplicatie.kennisnet.nl"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://testapplicatie.kennisnet.nl?result=false"));
    }

    @Test
    public void testWildcardWithDashURL() throws Exception {
        ReflectionTestUtils.setField(controller, "wildcardEnabled", true);
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL).param(SsoQueryController.PARAM_FORMAT, "text")
                .param("response_url", "https://test-applicatie.kennisnet.nl"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://test-applicatie.kennisnet.nl?result=false"));
    }

    @Test
    public void testNoResponseUrlParam() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL).param(SsoQueryController.PARAM_FORMAT, "text"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(
                        containsString("parameter 'response_url' is not present")));
    }

    @Test
    public void testInvalidResponseUrlParam() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "testapplicatie.kennisnet.nl"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testWithoutQueryParameters() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://testapplicatie.kennisnet.nl?result=false"));
    }

    @Test
    public void testBlacklistedRedirectUrl() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL).param("response_url", "https://test.kennisnet.nl"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFirstDomainWildCardRedirectUrl() throws Exception {
        ReflectionTestUtils.setField(controller, "wildcardEnabled", true);
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL).param("response_url", "https://test.kennisnet.nl"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://test.kennisnet.nl?result=false"));
    }

    @Test
    public void testWhitelistedSubSubDomainRedirectUrl() throws Exception {
        ReflectionTestUtils.setField(controller, "wildcardEnabled", true);
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL).param("response_url", "https://test.test.kennisnet.nl"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://test.test.kennisnet.nl?result=false"));
    }

    @Test
    public void testWhitelistInvalidWildcardUseRedirectUrl() throws Exception {
        ReflectionTestUtils.setField(controller, "wildcardEnabled", true);
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL).param("response_url", "https://akennisnet.nl"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testBlacklistedFirstDomainWildCardRedirectUrl() throws Exception {
        ReflectionTestUtils.setField(controller, "wildcardEnabled", true);
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL).param("response_url", "https://test.test.nl"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testEmptySessionCookie() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl")
                .cookie(new Cookie("oa_sso_id", StringUtils.EMPTY)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://testapplicatie.kennisnet.nl?result=false"));
    }

    @Test
    public void testNotEmptySessionCookie() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl")
                        .cookie(new Cookie(SSO_COOKIE_NAME, "test")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://testapplicatie.kennisnet.nl?result=true"));
    }

    @Test
    public void testQueryParametersInUrl() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl?test=1&test2=3")
                        .cookie(new Cookie(SSO_COOKIE_NAME, "test")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://testapplicatie.kennisnet.nl?test=1&test2=3&result=true"));
    }

    @Test
    public void testEmptyNotificationCookie() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl")
                .cookie(new Cookie("ssonot", StringUtils.EMPTY)))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://testapplicatie.kennisnet.nl?result=false"));
    }

    @Test
    public void testNotEmptyNotificationCookie() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", "https://testapplicatie.kennisnet.nl")
                .cookie(new Cookie("ssonot", "test")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://testapplicatie.kennisnet.nl?result=remote"));
    }

    @Test
    public void testHeaderIsSet() throws Exception {
        String origin = "https://testapplicatie.kennisnet.nl";
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL)
                .param("response_url", origin)
                .param(SsoQueryController.PARAM_FORMAT, "JSON"))
                .andExpect(header().string("Vary", "Origin"));
    }

    private void testJsonResponse(String format) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get(SSO_QUERY_URL).param(SsoQueryController.PARAM_FORMAT, format)
                .param("response_url", "https://testapplicatie.kennisnet.nl"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"result\":\"false\"}"));
    }

}
