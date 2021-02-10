/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Developer License
 * 4.0 (CVDLv4).
 *
 *
 * For the full License, see <https://vaadin.com/license/cvdl-4.0>.
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
