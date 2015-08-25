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

package org.cloudfoundry.client.spring;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.LoggregatorClient;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.info.Info;
import org.junit.Before;
import org.junit.Test;
import reactor.rx.Streams;

import javax.websocket.WebSocketContainer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class SpringLoggregatorClientBuilderTest extends AbstractRestTest {

    private final CloudFoundryClient cloudFoundryClient = mock(CloudFoundryClient.class);

    private final Info info = mock(Info.class);

    private final WebSocketContainer webSocketContainer = mock(WebSocketContainer.class);

    private final SpringLoggregatorClientBuilder builder = new SpringLoggregatorClientBuilder(this.webSocketContainer);

    @Before
    public void setUp() throws Exception {
        when(this.cloudFoundryClient.info()).thenReturn(this.info);
    }

    @Test
    public void test() {
        when(this.info.get()).thenReturn(Streams.just(
                new GetInfoResponse().withLoggingEndpoint("test-logging-endpoint")));

        LoggregatorClient loggregatorClient = this.builder
                .withCloudFoundryClient(this.cloudFoundryClient)
                .build();
    }

    @Test
    public void defaultConstructor() {
        new SpringLoggregatorClientBuilder();
    }
}
