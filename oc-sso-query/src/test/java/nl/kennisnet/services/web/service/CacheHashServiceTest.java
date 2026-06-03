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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
public class CacheHashServiceTest {

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    CacheHashService cacheHashService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cacheHashService, "cacheHashEndpoint", "sso-query-url");
        ReflectionTestUtils.setField(cacheHashService, "apiHeader", "api-key");
    }

    @Test
    void fetchCacheHashTest() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(new ResponseEntity<>("HASH", HttpStatus.OK));

        String result = cacheHashService.fetchCacheHash();

        assertEquals("HASH", result);
    }

    @Test
    void fetchCacheNullReturnTest() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        String result = cacheHashService.fetchCacheHash();

        assertEquals("", result);
    }

    @Test
    void fetchCacheHashHttpExceptionTest() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        String result = cacheHashService.fetchCacheHash();

        assertEquals("", result);
    }

    @Test
    void fetchCacheRestClientExceptionTest() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
                any(ParameterizedTypeReference.class))).thenThrow(new RestClientException("ERROR"));

        String result = cacheHashService.fetchCacheHash();

        assertEquals("", result);
    }

}