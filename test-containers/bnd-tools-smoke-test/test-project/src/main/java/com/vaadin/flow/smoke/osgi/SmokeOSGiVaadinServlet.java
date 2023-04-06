/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.smoke.osgi;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletAsyncSupported;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import com.vaadin.flow.osgi.support.servlet.OSGiVaadinServlet;

@Component(service = Servlet.class)
@HttpWhiteboardServletAsyncSupported
@HttpWhiteboardServletPattern("/not-servlet-context-listener-based/*")
public class SmokeOSGiVaadinServlet extends OSGiVaadinServlet {

    @Override
    protected void servletInitialized() throws ServletException {
        getService().setClassLoader(getClass().getClassLoader());
    }
}
