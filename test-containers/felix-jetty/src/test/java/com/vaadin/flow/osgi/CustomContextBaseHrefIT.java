/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi;

import java.net.URISyntaxException;

import com.vaadin.flow.uitest.ui.BaseHrefIT;

public class CustomContextBaseHrefIT extends BaseHrefIT {

    @Override
    protected String getTestPath() {
        return "/custom-test-context/view/com.vaadin.flow.uitest.ui.BaseHrefView";
    }

    @Override
    public void testBaseHref() throws URISyntaxException {
        super.testBaseHref();

        checkLogsForErrors();
    }

}
