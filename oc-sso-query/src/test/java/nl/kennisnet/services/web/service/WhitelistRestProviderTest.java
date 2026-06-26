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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WhitelistRestProviderTest {

    @Mock
    private RestClient restClient;

    private WhitelistRestProvider whitelistRestProvider;

    private Resource restResource;

    @Value("${api.endpoint.url}")
    private String url;

    @BeforeEach
    void setUp() {
        whitelistRestProvider = new WhitelistRestProvider(restClient);

        restResource = new ClassPathResource("rest_response.json");

        ReflectionTestUtils.setField(whitelistRestProvider, "dataLocation", restResource);

    }

    @Test
    public void getWhitelistTest() {
        List<String> whiteList = whitelistRestProvider.getWhitelist();
        assertNotNull(whiteList);
        assertEquals(3, whiteList.size());
        assertTrue(whiteList.contains("https://testapplicatie.vm.openconext.org"));
        assertTrue(whiteList.contains("https://www.example.com"));
        assertTrue(whiteList.contains("https://*.vm.openconext.org"));
    }

    @Test
    public void getEmptyListOnStatusCodeExceptionTest() {
        ReflectionTestUtils.setField(whitelistRestProvider, "endpointUrl", "http://localhost:3000/api/sso-query/all");
        when(restClient.get()).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertTrue(whitelistRestProvider.getWhitelist().isEmpty());
    }

    @Test
    public void getEmptyListOnRestClientExceptionTest() {
        ReflectionTestUtils.setField(whitelistRestProvider, "endpointUrl", "http://localhost:3000/api/sso-query/all");
        when(restClient.get()).thenThrow(new RestClientException(""));
        assertTrue(whitelistRestProvider.getWhitelist().isEmpty());
    }

}
