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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CacheHashServiceTest {

    @Mock
    private RestClient restClient;

    @InjectMocks
    private CacheHashService cacheHashService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cacheHashService, "cacheHashEndpoint", "sso-query-url");
        ReflectionTestUtils.setField(cacheHashService, "API_KEY_HEADER", "api-key");
    }

    @Test
    void fetchCacheHashTest() {
        RestClient.RequestHeadersUriSpec<?> spec = buildRequestSpec("HASH");

        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) spec);

        String result = cacheHashService.fetchCacheHash();

        assertEquals("HASH", result);
    }

    @Test
    void fetchCacheNullReturnTest() {
        RestClient.RequestHeadersUriSpec<?> spec = buildRequestSpec(null);

        when(restClient.get()).thenReturn((RestClient.RequestHeadersUriSpec) spec);

        assertEquals("", cacheHashService.fetchCacheHash());
    }

    @Test
    void fetchCacheNullUrlTest() {
        ReflectionTestUtils.setField(cacheHashService, "cacheHashEndpoint", null);

        assertEquals("", cacheHashService.fetchCacheHash());
    }

    @Test
    void fetchCacheHashHttpExceptionTest() {
        when(restClient.get()).thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        assertEquals("", cacheHashService.fetchCacheHash());
    }

    @Test
    void fetchCacheRestClientExceptionTest() {
        when(restClient.get()).thenThrow(new RestClientException("ERROR"));

        assertEquals("", cacheHashService.fetchCacheHash());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private RestClient.RequestHeadersUriSpec<?> buildRequestSpec(String responseBody) {
        RestClient.RequestHeadersUriSpec requestSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(requestSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.headers(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(responseBody);

        return requestSpec;
    }

}