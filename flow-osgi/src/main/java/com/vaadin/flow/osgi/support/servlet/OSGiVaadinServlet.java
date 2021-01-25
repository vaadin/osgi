/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Developer License
 * 4.0 (CVDLv4).
 *
 *
 * For the full License, see <https://vaadin.com/license/cvdl-4.0>.
 */
package com.vaadin.flow.osgi.support.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.vaadin.flow.osgi.support.OSGiVaadinInitialization;
import com.vaadin.flow.server.VaadinServlet;

/**
 * The Vaadin servlet to use in OSGi.
 * <p>
 * Extend this servlet to use Vaadin in OSGi in case {@link VaadinServlet}
 * doesn't work out of the box. That may happen in OSGi HTTP Whiteboard
 * specification has some limitations regarding to
 * {@link ServletContextListener} support. There is no need to use
 * {@link OSGiVaadinServlet} at all if
 * {@link ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)}
 * is called regardless of bundles deployment order and filtering based on
 * "osgi.http.whiteboard.context.select" property is properly working.
 * 
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
public class OSGiVaadinServlet extends VaadinServlet {

    private static class OSGiInitializationTracker extends
            ServiceTracker<OSGiVaadinInitialization, OSGiVaadinInitialization> {

        private final ServletContext servletContext;

        private OSGiInitializationTracker(BundleContext context,
                ServletContext servletContext) {
            super(context, OSGiVaadinInitialization.class, null);
            this.servletContext = servletContext;
        }

        @Override
        public OSGiVaadinInitialization addingService(
                ServiceReference<OSGiVaadinInitialization> reference) {
            OSGiVaadinInitialization result = super.addingService(reference);
            result.contextInitialized(new ServletContextEvent(servletContext));

            close();
            return result;
        }
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        BundleContext bundleContext = FrameworkUtil
                .getBundle(OSGiVaadinServlet.class).getBundleContext();

        OSGiInitializationTracker tracker = new OSGiInitializationTracker(
                bundleContext, getServletContext());
        tracker.open();
    }
}
