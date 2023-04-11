/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.karaf.smoketest;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.NotFoundException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * It allows to wait when HTTP container inside OSGi becomes ready.
 * Without this workaround IT tests starts immediately because there is
 * no maven server plugin which runs the server and wait when it becomes
 * ready and then switch to the next maven phase.
 * With the current configuration the server start is done async in the
 * separate JVM and no one waits for its readiness.
 * As a result IT tests starts immediately and this workaround is used
 * to wait when HTTP server starts to handle HTTP requests.
 */
public abstract class BaseKarafTest extends ChromeBrowserTest {

    // do not use default server availability checking due to Karaf startup delay
    @Override
    public void checkIfServerAvailable() {
    }

    @Override
    protected int getDeploymentPort() {
        return Integer.getInteger("serverPort");
    }

    protected HttpURLConnection waitUntilHttpOk(String path, int timeoutInSeconds) {
        return waitUntil(input -> {
            try {
                URL url = new URL(path);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                // check if resource is available
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return connection;
                }
            } catch (IOException exception) {
                throw new NotFoundException(exception);
            }
            return null;
        }, timeoutInSeconds);
    }

}
