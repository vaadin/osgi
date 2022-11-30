/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.karaf.smoketest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class PushResourceIT extends BaseKarafTest {

    @Test
    public void pushResourceIsRegistered() throws IOException {
        HttpURLConnection connection = waitUntilHttpOk(getRootURL() +
                "/VAADIN/static/push/vaadinPush.js", 180);
        String content = IOUtils
                .readLines(connection.getInputStream(), StandardCharsets.UTF_8)
                .stream().collect(Collectors.joining("\n"));
        MatcherAssert.assertThat(content,
                CoreMatchers.containsString("vaadinPush"));
    }
}
