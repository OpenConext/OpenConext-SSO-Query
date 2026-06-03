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
package nl.kennisnet.services.web.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BuildPropertiesConfigTest {

    @Mock
    private ResourceLoader resourceLoader;

    @InjectMocks
    private final BuildPropertiesConfig buildPropertiesConfig = new BuildPropertiesConfig();


    @Test
    void buildPropertiesMissing() {
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(false);
        when(resourceLoader.getResource(any())).thenReturn(mockResource);

        BuildProperties buildProperties = buildPropertiesConfig.buildProperties(resourceLoader);

        assertEquals("0.0.0-TEST", buildProperties.getVersion());
        assertEquals("test-build", buildProperties.getName());
        assertEquals("test-group", buildProperties.getGroup());
        assertEquals("test-artifact", buildProperties.getArtifact());
    }

    @Test
    void buildPropertiesAvailable() throws IOException {
        Resource mockResource = mock(Resource.class);

        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenReturn(
                new ByteArrayInputStream((
                        """
                                build.artifact=oc-sso-query
                                build.group=nl.kennisnet.services
                                build.java_version=21
                                build.name=OC SSO Query Service
                                build.spring_boot_version=3.5.14
                                build.time=2026-05-13T13\\:36\\:47.506Z
                                build.version=2.5.0""").getBytes()));

        when(resourceLoader.getResource(any())).thenReturn(mockResource);

        BuildProperties buildProperties = buildPropertiesConfig.buildProperties(resourceLoader);

        assertEquals("oc-sso-query", buildProperties.getArtifact());
        assertEquals("nl.kennisnet.services", buildProperties.getGroup());
        assertEquals("OC SSO Query Service", buildProperties.getName());
        assertEquals("2.5.0", buildProperties.getVersion());
        assertEquals(Instant.parse("2026-05-13T13:36:47.506Z"), buildProperties.getTime());
        assertEquals("21", buildProperties.get("java_version"));
        assertEquals("3.5.14", buildProperties.get("spring_boot_version"));
    }

    @Test
    void buildPropertiesMissingIOException() throws IOException {
        Resource mockResource = mock(Resource.class);

        when(mockResource.exists()).thenReturn(true);
        doThrow(IOException.class).when(mockResource).getInputStream();

        when(resourceLoader.getResource(any())).thenReturn(mockResource);

        BuildProperties buildProperties = buildPropertiesConfig.buildProperties(resourceLoader);

        assertEquals("0.0.0-ERROR", buildProperties.getVersion());
        assertEquals("error-build", buildProperties.getName());
        assertEquals("error-group", buildProperties.getGroup());
        assertEquals("error-artifact", buildProperties.getArtifact());
    }

}
