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

package org.cloudfoundry.client;

import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.client.v2.spaces.Spaces;
import org.cloudfoundry.client.v3.applications.Applications;
import org.cloudfoundry.client.v3.droplets.Droplets;
import org.cloudfoundry.client.v3.packages.Packages;

/**
 * Main entry point to the Cloud Foundry Client API
 */
public interface CloudFoundryClient {

    /**
     * Main entry point to the Cloud Foundry Applications Client API
     *
     * @return the Cloud Foundry Application Client API
     */
    Applications applications();

    /**
     * Main entry point to the Cloud Foundry Applications Droplets API
     *
     * @return the Cloud Foundry Application Droplets API
     */
    Droplets droplets();

    /**
     * Returns the OAuth2 access token currently being used by the client.  This is typically used by implementations of
     * {@link LoggregatorClient} to construct {@code Authorization} headers.
     *
     * @return the OAuth2 access token currently being used by the client
     */
    String getAccessToken();

    /**
     * Main entry point to the Cloud Foundry Info Client API
     *
     * @return the Cloud Foundry Info Client API
     */
    Info info();

    /**
     * Main entry point to the Cloud Foundry Packages Client API
     *
     * @return the Cloud Foundry Packages Client API
     */
    Packages packages();

    /**
     * Main entry point to the Cloud Foundry Spaces Client API
     *
     * @return the Cloud Foundry Space Client API
     */
    Spaces spaces();

}
