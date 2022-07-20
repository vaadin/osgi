package com.vaadin.flow.karaf.smoketest;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

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
public abstract class WaitForUrlTest extends ChromeBrowserTest {

    protected Optional<HttpURLConnection> waitAndGetUrl(URL url, int count)
            throws InterruptedException {
        if (count == 0) {
            return Optional.empty();
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            // check if resource is available
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException();
            }
            return Optional.of(connection);
        } catch (IOException exception) {
            Thread.sleep(1000);
            waitAndGetUrl(url, count - 1);
        }

        return Optional.empty();
    }

}
