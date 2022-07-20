package com.vaadin.flow.karaf.smoketest;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class SmokeIT extends WaitForUrlTest {

    @Override
    public void checkIfServerAvailable() {
    }

    @Override
    protected String getTestPath() {
        return "/smoke-view";
    }

    @Override
    protected int getDeploymentPort() {
        return 8181;
    }

    @Test
    public void buttonIsShown_clickIsHandled()
            throws MalformedURLException, InterruptedException {

        URL url = new URL(getRootURL() + getTestPath());
        waitAndGetUrl(url, 180);

        super.checkIfServerAvailable();

        open();

        WebElement action = findElement(By.id("action"));
        action.click();

        Assert.assertTrue(isElementPresent(By.id("info")));
    }

}
