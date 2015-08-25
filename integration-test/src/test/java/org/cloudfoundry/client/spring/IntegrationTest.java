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
import org.cloudfoundry.client.loggregator.StreamLogsRequest;
import org.cloudfoundry.client.loggregator.StreamLogsResponse;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.spaces.ListSpacesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpacesResponse;
import org.cloudfoundry.client.v3.applications.CreateApplicationRequest;
import org.cloudfoundry.client.v3.applications.CreateApplicationResponse;
import org.cloudfoundry.client.v3.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v3.applications.DeleteApplicationResponse;
import org.cloudfoundry.client.v3.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationsResponse;
import org.cloudfoundry.client.v3.droplets.GetDropletRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageResponse;
import org.cloudfoundry.client.v3.packages.GetPackageRequest;
import org.cloudfoundry.client.v3.packages.StagePackageRequest;
import org.cloudfoundry.client.v3.packages.StagePackageResponse;
import org.cloudfoundry.client.v3.packages.UploadPackageRequest;
import org.cloudfoundry.client.v3.packages.UploadPackageResponse;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import reactor.Publishers;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.action.Control;

import java.io.File;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cloudfoundry.client.v3.packages.CreatePackageRequest.PackageType.BITS;

public final class IntegrationTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private volatile String application;

    private volatile File bits;

    private volatile CloudFoundryClient cloudFoundryClient;

    private volatile LoggregatorClient loggregatorClient;

    private volatile String space;

    @Before
    public void configure() throws Exception {
        RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(new StandardEnvironment(), null);

        this.application = resolver.getRequiredProperty("test.application");
        this.bits = resolver.getRequiredProperty("test.bits", File.class);
        this.space = resolver.getRequiredProperty("test.space");

        this.cloudFoundryClient = new SpringCloudFoundryClientBuilder()
                .withApi(resolver.getRequiredProperty("test.host"))
                .withCredentials(
                        resolver.getRequiredProperty("test.username"),
                        resolver.getRequiredProperty("test.password"))
                .withSkipSslValidation(resolver.getProperty("test.skipSslValidation", Boolean.class, false))
                .build();

        this.loggregatorClient = new SpringLoggregatorClientBuilder()
                .withCloudFoundryClient(this.cloudFoundryClient)
                .build();
    }

    @Test
    public void createApplication() {
        listApplications()
                .flatMap(response -> Streams.from(response.getResources()))
                .flatMap(this::deleteApplication)
                .consume(this::ignoreResponse, this::handleError,
                        r -> this.logger.info("All existing applications deleted"));

        Stream<CreateApplicationResponse> createApplicationStream = listSpaces()
                .flatMap(this::createApplication);
        // TODO: .broadcast();

        Control loggregatorControl = createApplicationStream
                .flatMap(this::streamLogs)
                .consume(this::printLog, this::handleError, r -> this.logger.info("Logging finished"));

        createApplicationStream
                .flatMap(this::createPackage)
                .flatMap(this::uploadPackage)
                .flatMap(this::waitForPackageUploadProcessing)
                .flatMap(this::stagePackage)
                .flatMap(this::waitForPackageStagingProcessing)
                .consume(this::ignoreResponse, this::handleError, r -> {
                    this.logger.info("Integration test finished");
                    loggregatorControl.cancel();
                });

        // TODO: createApplicationStream.connect();
    }

    private void handleError(Throwable exception) {
        this.logger.error("Error encountered: {}", exception.getMessage());
        System.exit(1);
    }

    private void ignoreResponse(Object response) {
    }

    private void printLog(StreamLogsResponse r) {
        this.logger.info("[{}/{}] {} {}", r.getSourceName(), r.getSourceId(), r
                .getMessageType(), r.getMessage());
    }

    private Stream<CreateApplicationResponse> createApplication(ListSpacesResponse response) {
        Resource.Metadata metadata = response.getResources().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not find space " + this.space))
                .getMetadata();

        CreateApplicationRequest request = new CreateApplicationRequest()
                .withSpaceId(metadata.getId())
                .withName(this.application);

        return Streams.wrap(this.cloudFoundryClient.applications().create(request))
                .observeSubscribe(s -> this.logger.info("Creating application"))
                .observe(r -> this.logger.info("Created application"));
    }

    private Publisher<CreatePackageResponse> createPackage(CreateApplicationResponse response) {
        CreatePackageRequest request = new CreatePackageRequest()
                .withApplicationId(response.getId())
                .withType(BITS);

        return Streams.wrap(this.cloudFoundryClient.packages().create(request))
                .observeSubscribe(s -> this.logger.info("Creating package"))
                .observe(r -> this.logger.info("Created package"));
    }

    private Publisher<DeleteApplicationResponse> deleteApplication(ListApplicationsResponse.Resource resource) {
        DeleteApplicationRequest request = new DeleteApplicationRequest()
                .withId(resource.getId());

        return Streams.wrap(this.cloudFoundryClient.applications().delete(request))
                .observeSubscribe(s -> this.logger.info("Deleting application"))
                .observe(r -> this.logger.info("Deleted application"));
    }

    private Stream<ListApplicationsResponse> listApplications() {
        return Streams.wrap(this.cloudFoundryClient.applications().list(new ListApplicationsRequest()));
    }

    private Stream<ListSpacesResponse> listSpaces() {
        return Streams.wrap(this.cloudFoundryClient.spaces().list(new ListSpacesRequest().filterByName(this.space)));
    }

    private Publisher<StagePackageResponse> stagePackage(UploadPackageResponse response) {
        StagePackageRequest request = new StagePackageRequest()
                .withId(response.getId())
                .withBuildpack("https://github.com/cloudfoundry/java-buildpack.git");

        return Streams.wrap(this.cloudFoundryClient.packages().stage(request))
                .observe(r -> this.logger.info("Staging package"));
    }

    private Publisher<StreamLogsResponse> streamLogs(CreateApplicationResponse response) {
        StreamLogsRequest request = new StreamLogsRequest()
                .withId(response.getId());

        return this.loggregatorClient.stream(request);
    }

    private Publisher<UploadPackageResponse> uploadPackage(CreatePackageResponse response) {
        UploadPackageRequest request = new UploadPackageRequest()
                .withId(response.getId())
                .withFile(this.bits);

        return Streams.wrap(this.cloudFoundryClient.packages().upload(request))
                .observeSubscribe(s -> this.logger.info("Uploading package"));
    }

    private Publisher<StagePackageResponse> waitForPackageStagingProcessing(StagePackageResponse response) {
        return Streams.wrap(Publishers.<StagePackageResponse>create(subscriber -> {
            GetDropletRequest request = new GetDropletRequest()
                    .withId(response.getId());

            Streams.wrap(this.cloudFoundryClient.droplets().get(request))
                    .observe(r -> this.logger.debug("Staging package processing: {}", r.getState()))
                    .consume(r -> {
                        if ("STAGED".equals(r.getState())) {
                            subscriber.onNext(response);
                            subscriber.onComplete();
                        } else if ("FAILED".equals(r.getState())) {
                            subscriber.onError(new ProcessingFailed(r.getError()));
                        } else {
                            subscriber.onError(new ProcessingIncomplete());
                        }
                    }, subscriber::onError);
        })).retryWhen(errors -> errors.flatMap(throwable -> {
                    if (throwable instanceof ProcessingIncomplete) {
                        return Streams.timer(1, SECONDS)
                                .observeSubscribe(s -> this.logger.info("Waiting for package staging processing"));
                    } else {
                        return Publishers.error(throwable);
                    }
                })
        ).observe(r -> this.logger.info("Staged package"));
    }

    private Publisher<UploadPackageResponse> waitForPackageUploadProcessing(UploadPackageResponse response) {
        return Streams.wrap(Publishers.<UploadPackageResponse>create(subscriber -> {
            GetPackageRequest request = new GetPackageRequest()
                    .withId(response.getId());

            Streams.wrap(this.cloudFoundryClient.packages().get(request))
                    .observe(r -> this.logger.warn("Upload package processing: {}", r.getState()))
                    .consume(r -> {
                        if ("READY".equals(r.getState())) {
                            subscriber.onNext(response);
                            subscriber.onComplete();
                        } else if ("FAILED".equals(r.getState())) {
                            subscriber.onError(new ProcessingFailed(r.getError()));
                        } else {
                            subscriber.onError(new ProcessingIncomplete());
                        }
                    }, subscriber::onError);
        })).retryWhen(errors -> errors.flatMap(throwable -> {
                    if (throwable instanceof ProcessingIncomplete) {
                        return Streams.timer(1, SECONDS)
                                .observeSubscribe(s -> this.logger.info("Waiting for package upload processing"));
                    } else {
                        return Publishers.error(throwable);
                    }
                })
        ).observe(r -> this.logger.info("Uploaded package"));
    }

}
