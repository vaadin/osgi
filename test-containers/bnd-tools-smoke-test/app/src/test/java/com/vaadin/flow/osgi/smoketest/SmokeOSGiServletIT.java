/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.smoketest;

public class SmokeOSGiServletIT extends SmokeIT {

    @Override
    protected String getTestPath() {
        return "/not-servlet-context-listener-based/smoke-view";
    }
}
