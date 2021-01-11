package com.vaadin.flow.osgi.smoketest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class WaitHttpIT extends ChromeBrowserTest {

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
        waitHttp(60);
    }

    private void waitHttp(int count)
            throws MalformedURLException, InterruptedException {
        String rootUrl = getRootURL();
        if (count == 0) {
            throw new IllegalStateException(
                    "URL '" + rootUrl + "' is not avialable");
        }
        URL url = new URL(rootUrl);
        try {
            url.openConnection();
        } catch (IOException exception) {
            Thread.sleep(1000);
            waitHttp(count - 1);
        }
    }

}
