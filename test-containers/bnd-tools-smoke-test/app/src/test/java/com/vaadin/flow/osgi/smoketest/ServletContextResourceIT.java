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

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.hamcrest.MatcherAssert.assertThat;

public class ServletContextResourceIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/com/vaadin/flow/smoke/SmokeView.class";
    }

    @Test
    public void pushResourceIsRegistered() throws IOException {
        open();

        WebElement body = findElement(By.tagName("body"));
        assertThat(body.getText().trim(), CoreMatchers.startsWith(
                "Could not navigate to 'com/vaadin/flow/smoke/SmokeView.class'"));
    }
}
