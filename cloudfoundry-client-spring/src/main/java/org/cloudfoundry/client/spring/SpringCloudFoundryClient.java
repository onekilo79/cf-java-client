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
import org.cloudfoundry.client.spring.v2.info.SpringInfo;
import org.cloudfoundry.client.spring.v2.spaces.SpringSpaces;
import org.cloudfoundry.client.spring.v3.applications.SpringApplications;
import org.cloudfoundry.client.spring.v3.droplets.SpringDroplets;
import org.cloudfoundry.client.spring.v3.packages.SpringPackages;
import org.cloudfoundry.client.v2.info.Info;
import org.cloudfoundry.client.v2.spaces.Spaces;
import org.cloudfoundry.client.v3.applications.Applications;
import org.cloudfoundry.client.v3.droplets.Droplets;
import org.cloudfoundry.client.v3.packages.Packages;
import org.springframework.security.oauth2.client.OAuth2RestOperations;

import java.net.URI;

final class SpringCloudFoundryClient implements CloudFoundryClient {

    private final Applications applications;

    private final Droplets droplets;

    private final Info info;

    private final Packages packages;

    private final OAuth2RestOperations restOperations;

    private final Spaces spaces;

    SpringCloudFoundryClient(OAuth2RestOperations restOperations, URI root) {
        this.applications = new SpringApplications(restOperations, root);
        this.droplets = new SpringDroplets(restOperations, root);
        this.info = new SpringInfo(restOperations, root);
        this.packages = new SpringPackages(restOperations, root);
        this.spaces = new SpringSpaces(restOperations, root);

        this.restOperations = restOperations;
    }

    OAuth2RestOperations getRestOperations() {
        return this.restOperations;
    }

    @Override
    public Applications applications() {
        return this.applications;
    }

    @Override
    public Droplets droplets() {
        return this.droplets;
    }

    @Override
    public String getAccessToken() {
        return this.restOperations.getAccessToken().getValue();
    }

    @Override
    public Info info() {
        return this.info;
    }

    @Override
    public Packages packages() {
        return this.packages;
    }

    @Override
    public Spaces spaces() {
        return this.spaces;
    }

}
