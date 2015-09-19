/*
 * Copyright 2013-2015 the original author or authors.
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

package org.cloudfoundry.client.spring.loggregator;

import org.cloudfoundry.client.CloudFoundryClient;

import javax.websocket.ClientEndpointConfig;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.oauth2.common.OAuth2AccessToken.BEARER_TYPE;

public final class AuthorizationConfigurator extends ClientEndpointConfig.Configurator {

    private final CloudFoundryClient cloudFoundryClient;

    public AuthorizationConfigurator(CloudFoundryClient cloudFoundryClient) {
        this.cloudFoundryClient = cloudFoundryClient;
    }

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        String authorizationHeader = String.format("%s %s", BEARER_TYPE, this.cloudFoundryClient.getAccessToken());
        headers.put(AUTHORIZATION, Collections.singletonList(authorizationHeader));
    }

}
