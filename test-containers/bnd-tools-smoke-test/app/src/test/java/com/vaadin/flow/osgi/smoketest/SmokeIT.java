/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.smoketest;

import java.net.MalformedURLException;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class SmokeIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/smoke-view";
    }

    @Test
    public void buttonIsShown_clickIsHandled()
            throws MalformedURLException, InterruptedException {
        open();

        WebElement action = findElement(By.id("action"));
        action.click();

        Assert.assertTrue(isElementPresent(By.id("info")));
    }

}
