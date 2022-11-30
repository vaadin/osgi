package com.vaadin.flow.karaf.smoketest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class SmokeIT extends BaseKarafTest {

    @Override
    protected String getTestPath() {
        return "/smoke-view";
    }

    @Override
    protected int getDeploymentPort() {
        return 8181;
    }

    @Test
    public void buttonIsShown_clickIsHandled() {

        waitUntilHttpOk(getRootURL() + getTestPath(), 180);

        open();

        WebElement action = findElement(By.id("action"));
        action.click();

        Assert.assertTrue(isElementPresent(By.id("info")));
    }

}
