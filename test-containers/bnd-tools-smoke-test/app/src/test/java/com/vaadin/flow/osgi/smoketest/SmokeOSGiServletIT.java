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

public class SmokeOSGiServletIT extends SmokeIT {

    @Override
    protected String getTestPath() {
        return "/not-servlet-context-listener-based/smoke-view";
    }
}
