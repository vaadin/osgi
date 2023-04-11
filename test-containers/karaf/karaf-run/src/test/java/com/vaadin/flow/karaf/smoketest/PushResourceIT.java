/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.karaf.smoketest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PushResourceIT extends ChromeBrowserTest {

    @Override
    protected int getDeploymentPort() {
        return 8181;
    }

    @Test
    public void pushResourceIsRegistered() throws IOException {
        String rootUrl = getRootURL();
        String pushPath = rootUrl + "/VAADIN/static/push/vaadinPush.js";

        URL url = new URL(pushPath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        Assert.assertEquals(HttpURLConnection.HTTP_OK,
                connection.getResponseCode());
        String content = IOUtils
                .readLines(connection.getInputStream(), StandardCharsets.UTF_8)
                .stream().collect(Collectors.joining("\n"));
        MatcherAssert.assertThat(content,
                CoreMatchers.containsString("vaadinPush"));
    }
}
