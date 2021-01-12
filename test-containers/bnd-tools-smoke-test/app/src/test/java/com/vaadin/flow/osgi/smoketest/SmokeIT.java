/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Developer License
 * 4.0 (CVDLv4).
 *
 *
 * For the full License, see <https://vaadin.com/license/cvdl-4.0>.
 */
package com.vaadin.flow.osgi.smoketest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class SmokeIT extends ChromeBrowserTest {

    @Override
    public void checkIfServerAvailable() {
    }

    @Override
    protected String getTestPath() {
        return "/smoke-view";
    }

    @Test
    public void buttonIsShown_clickIsHandled()
            throws MalformedURLException, InterruptedException {
        // It allows to wait when HTTP container inside OSGi becomes ready.
        // Without this workaround IT tests starts immediately because there is
        // no maven server plugin which runs the server and wait when it becomes
        // ready and then switch to the next maven phase.
        // With the current configuration the server start is done async in the
        // separate JVM and no one waits for its readiness.
        // As a result IT tests starts immediately and this workaround is used
        // to wait when HTTP server starts to handle HTTP requests.
        waitRootUrl(60);

        super.checkIfServerAvailable();

        open();

        WebElement action = findElement(By.id("action"));
        action.click();

        Assert.assertTrue(isElementPresent(By.id("info")));
    }

    private void waitRootUrl(int count)
            throws MalformedURLException, InterruptedException {
        String rootUrl = getRootURL();
        if (count == 0) {
            throw new IllegalStateException(
                    "URL '" + rootUrl + "' is not avialable");
        }
        URL url = new URL(rootUrl);
        try {
            URLConnection connection = url.openConnection();
            connection.connect();
        } catch (IOException exception) {
            Thread.sleep(1000);
            waitRootUrl(count - 1);
        }
    }
}
