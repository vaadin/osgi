/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.smoke.osgi;

import javax.servlet.ServletException;

import com.vaadin.flow.server.VaadinServlet;

public class CustomContextServlet extends VaadinServlet {

    @Override
    protected void servletInitialized() throws ServletException {
        getService().setClassLoader(getClass().getClassLoader());
    }
}
