/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.smoketest;

public class CustomContextPushResourceIT extends PushResourceIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/custom-context";
    }
}
