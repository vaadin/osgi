package com.vaadin.flow.karaf.smoketest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

public class PushResourceIT extends WaitForUrlTest {

    @Test
    public void pushResourceIsRegistered() throws IOException, InterruptedException {
        String rootUrl = getRootURL();
        String pushPath = rootUrl + "/VAADIN/static/push/vaadinPush.js";

        URL url = new URL(pushPath);
        Optional<HttpURLConnection> connection = waitAndGetUrl(url, 180);
        Assert.assertTrue("URL '" + url.getPath() + "' is not available", connection.isPresent());
        String content = IOUtils
                .readLines(connection.get().getInputStream(), StandardCharsets.UTF_8)
                .stream().collect(Collectors.joining("\n"));
        MatcherAssert.assertThat(content,
                CoreMatchers.containsString("vaadinPush"));
    }
}
