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

import nl.kennisnet.services.web.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sends rest request for getting whitelist and cached it.
 * Amount of seconds after which cache will expired determines from 'whitelist.cache.expiration.time.seconds' property.
 */
@Service
public class WhitelistRestProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhitelistRestProvider.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.endpoint.url}")
    private String endpointUrl;

    /**
     * The name of the header to add to the REST client calls which contains the api key
     */
    @Value("${api.key.header.key}")
    private String apiKeyHeaderKey;

    /**
     * The value to use for the api-key.
     */
    @Value("${api.key.header.value}")
    private String apiKeyHeaderValue;

    /**
     * Static file containing the whitelist of domains to be allowed
     */
    @Value("${data.location}")
    private Resource dataLocation;

    private final RestClient restClient;

    public WhitelistRestProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Provides hosts whitelist ({@link List<String>}).
     * Whitelist is received either using a REST request or a static file. The received data is cached.
     * Cache is refreshed once every 'whitelist.cache.expiration.time.seconds' seconds.
     *
     * @return {@link List<String>} hosts which are whitelisted.
     */
    @Cacheable(value = CacheConfig.WHITELIST_CACHE)
    public List<String> getWhitelist() {
        LOGGER.debug("Retrieving whitelist.");

        if (StringUtils.hasText(endpointUrl)) {
            return getWhitelistFromDataServices();
        } else {
            return getWhitelistFromStaticFile();
        }
    }

    private List<String> getWhitelistFromDataServices() {
        LOGGER.debug("Retrieving all SSO Query data from Data Services");
        HttpHeaders requestHeaders = new HttpHeaders();

        if (StringUtils.hasText(apiKeyHeaderKey) && StringUtils.hasText(apiKeyHeaderValue)) {
            requestHeaders.add(apiKeyHeaderKey, apiKeyHeaderValue);
        }

        HttpEntity<?> httpEntity = new HttpEntity<>(requestHeaders);

        try {
            return restClient.get()
                    .uri(endpointUrl)
                    .headers(headers -> headers.addAll(httpEntity.getHeaders()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<String>>() {});
        } catch (HttpStatusCodeException htsce) {
            LOGGER.error("Unexpected response received: " + htsce.getMessage());
            return new ArrayList<>();
        } catch (RestClientException rce) {
            LOGGER.error("Communication error occurred: " + rce.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> getWhitelistFromStaticFile() {
        LOGGER.debug("Retrieving all SSO Query data from static file");
        try {
            return objectMapper.readValue(dataLocation.getFile(), new TypeReference<>() { });
        } catch (IOException e) {
            LOGGER.error("Failed to parse ('{}')", dataLocation, e);
            return Collections.emptyList();
        }
    }

}
