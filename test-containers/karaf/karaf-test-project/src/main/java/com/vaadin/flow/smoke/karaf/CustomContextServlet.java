/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Developer License
 * 4.0 (CVDLv4).
 *
 *
 * For the full License, see <https://vaadin.com/license/cvdl-4.0>.
 */
package com.vaadin.flow.smoke.karaf;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletAsyncSupported;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import com.vaadin.flow.osgi.support.servlet.OSGiVaadinServlet;

@HttpWhiteboardServletAsyncSupported
@HttpWhiteboardServletPattern("/*")
@Component(service = Servlet.class, property = HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT
        + "=(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME
        + "=customContext)")
public class CustomContextServlet extends OSGiVaadinServlet {

    @Override
    protected void servletInitialized() throws ServletException {
        getService().setClassLoader(getClass().getClassLoader());
    }
}
