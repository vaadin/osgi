package com.vaadin.flow.karaf.smoketest;

public class CustomContextPushResourceIT extends PushResourceIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/custom-context";
    }
}
