package com.vaadin.flow.karaf.smoketest;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

public class WaitHttpIT extends WaitForUrlTest {

    @Override
    public void checkIfServerAvailable() {
    }

    @Override
    public void setup() {
    }

    @Test
    public void waitForHttp()
            throws MalformedURLException, InterruptedException {
        // This is not really a test.
        // It allows to wait when HTTP container inside OSGi becomes ready.
        // Without this workaround IT tests starts immediately because there is
        // no maven server plugin which runs the server and wait when it becomes
        // ready and then switch to the next maven phase.
        // With the current configuration the server start is done async in the
        // separate JVM and no one waits for its readiness.
        // As a result IT tests starts immediately and this workaround is used
        // to wait when HTTP server starts to handle HTTP requests.
        // It's executed before any other IT test.
        URL url = new URL(getRootURL());
        waitAndGetUrl(url, 60);
    }

}
