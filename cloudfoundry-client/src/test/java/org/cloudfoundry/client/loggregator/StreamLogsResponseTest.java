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

package org.cloudfoundry.client.loggregator;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.cloudfoundry.client.loggregator.StreamLogsResponse.MessageType.OUT;
import static org.junit.Assert.assertEquals;

public final class StreamLogsResponseTest {

    @Test
    public void test() {
        List<String> drainUrls = Arrays.asList("test-drain-url-1", "test-drain-url-2");

        Date timestamp = new Date();

        StreamLogsResponse response = new StreamLogsResponse()
                .withApplicationId("test-id")
                .withDrainUrl(drainUrls.get(0))
                .withDrainUrls(Collections.singletonList(drainUrls.get(1)))
                .withMessage("test-message")
                .withMessageType(OUT)
                .withSourceId("test-source-id")
                .withSourceName("test-source-name")
                .withTimestamp(timestamp);

        assertEquals("test-id", response.getApplicationId());
        assertEquals(drainUrls, response.getDrainUrls());
        assertEquals("test-message", response.getMessage());
        assertEquals(OUT, response.getMessageType());
        assertEquals("test-source-id", response.getSourceId());
        assertEquals("test-source-name", response.getSourceName());
        assertEquals(timestamp, response.getTimestamp());
    }

}
