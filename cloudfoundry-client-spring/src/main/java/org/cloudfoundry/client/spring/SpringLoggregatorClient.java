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

import org.cloudfoundry.client.LoggregatorClient;
import org.cloudfoundry.client.RequestValidationException;
import org.cloudfoundry.client.Validatable;
import org.cloudfoundry.client.ValidationResult;
import org.cloudfoundry.client.loggregator.StreamLogsRequest;
import org.cloudfoundry.client.loggregator.StreamLogsResponse;
import org.cloudfoundry.client.spring.loggregator.LoggregatorMessageHandler;
import org.cloudfoundry.client.spring.loggregator.ReactiveEndpoint;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.Publishers;
import reactor.rx.Stream;
import reactor.rx.Streams;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

final class SpringLoggregatorClient implements LoggregatorClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ClientEndpointConfig clientEndpointConfig;

    private final WebSocketContainer webSocketContainer;

    private final URI root;

    SpringLoggregatorClient(ClientEndpointConfig clientEndpointConfig, WebSocketContainer webSocketContainer,
                            URI root) {
        this.clientEndpointConfig = clientEndpointConfig;
        this.webSocketContainer = webSocketContainer;
        this.root = root;
    }

    @Override
    public Publisher<StreamLogsResponse> stream(StreamLogsRequest request) {
        return ws(request,
                builder -> builder.path("tail/").queryParam("app", request.getId()),
                LoggregatorMessageHandler::new);
    }

    private <T> Stream<T> ws(Validatable request, Consumer<UriComponentsBuilder> builderCallback,
                             Function<Subscriber<T>, MessageHandler> messageHandlerCreator) {

        AtomicReference<Session> session = new AtomicReference<>();

        return exchange(request, (Subscriber<T> subscriber) -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUri(this.root);
            builderCallback.accept(builder);
            URI uri = builder.build().toUri();

            MessageHandler messageHandler = messageHandlerCreator.apply(subscriber);
            ReactiveEndpoint<T> endpoint = new ReactiveEndpoint<>(messageHandler, subscriber);

            try {
                this.logger.debug("WS {}", uri);
                session.set(this.webSocketContainer.connectToServer(endpoint, this.clientEndpointConfig, uri));
            } catch (DeploymentException | IOException e) {
                subscriber.onError(e);
            }
        }).observeCancel(r -> Optional.ofNullable(session.get()).ifPresent(s -> {
            try {
                s.close();
            } catch (IOException e) {
                this.logger.warn("Failure closing session", e);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    private <T> Stream<T> exchange(Validatable request, Consumer<Subscriber<T>> exchange) {
        return Streams.wrap(Publishers.create(subscriber -> {
            if (request != null) {
                ValidationResult validationResult = request.isValid();
                if (validationResult.getStatus() == ValidationResult.Status.INVALID) {
                    subscriber.onError(new RequestValidationException(validationResult));
                    return;
                }
            }

            exchange.accept(subscriber);
        }));
    }

}
