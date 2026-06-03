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
package nl.kennisnet.services.web.service;

import com.google.common.io.Resources;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(WhitelistRestProvider.class)
@TestPropertySource(locations = "classpath:application.properties")
public class WhitelistRestProviderTest {

    @Autowired
    private WhitelistRestProvider provider;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${api.endpoint.url}")
    private String url;

    @Test
    public void getWhitelistTest() throws Exception {
        Resource resource = resourceLoader.getResource("rest_response.json");
        this.server.expect(requestTo(url))
                .andRespond(withSuccess(Resources.toString(resource.getURL(), StandardCharsets.UTF_8),
                            MediaType.APPLICATION_JSON));
        List<String> whiteList = provider.getWhitelist();
        assertNotNull(whiteList);
        assertEquals(3, whiteList.size());
        assertTrue(whiteList.contains("https://testapplicatie.vm.openconext.org"));
        assertTrue(whiteList.contains("https://www.example.com"));
        assertTrue(whiteList.contains("https://*.vm.openconext.org"));
    }

    @Test
    public void getEmptyListOnStatusCodeExceptionTest() {
        this.server.expect(requestTo(url)).andRespond(withBadRequest());
        assertTrue(provider.getWhitelist().isEmpty());
    }

    @Test
    public void getEmptyListOnRestClientExceptionTest() {
        this.server.expect(requestTo(url)).andRespond(clientHttpRequest -> {
            throw new RestClientException("");
        });
        assertTrue(provider.getWhitelist().isEmpty());
    }

}
