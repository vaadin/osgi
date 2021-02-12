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

public class CustomContextPushResourceIT extends PushResourceIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/custom-context";
    }
}
