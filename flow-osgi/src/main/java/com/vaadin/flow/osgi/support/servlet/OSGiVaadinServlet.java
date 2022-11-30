/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support.servlet;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.osgi.support.OSGiVaadinInitialization;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;

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

    @Override
    public void destroy() {
        ServletContext servletContext = getServletContext();
        Lookup lookup = new VaadinServletContext(servletContext)
                .getAttribute(Lookup.class);
        super.destroy();
        if (lookup == null) {
            return;
        }

        BundleContext bundleContext = FrameworkUtil
                .getBundle(OSGiVaadinServlet.class).getBundleContext();
        Set<Servlet> servlets = new HashSet<>();
        try {
            ServiceReference<?>[] references = bundleContext
                    .getAllServiceReferences(Servlet.class.getName(), null);
            references = references == null ? new ServiceReference<?>[0]
                    : references;
            for (ServiceReference<?> reference : references) {
                servlets.addAll(handleDestroy(lookup, reference));
            }
        } catch (InvalidSyntaxException e) {
            // this may not happen because filter parameter is {@code null} so
            // it may not have invalid syntax
            assert false;
        }
        servlets.remove(this);
        if (servlets.size() > 0) {
            return;
        }
        ServiceReference<OSGiVaadinInitialization> reference = bundleContext
                .getServiceReference(OSGiVaadinInitialization.class);
        if (reference == null) {
            return;
        }
        OSGiVaadinInitialization initialization = bundleContext
                .getService(reference);
        initialization
                .contextDestroyed(new ServletContextEvent(servletContext));
    }

    private Set<Servlet> handleDestroy(Lookup lookup,
            ServiceReference<?> reference) {
        Set<Servlet> servlets = new HashSet<>();
        Bundle[] usingBundles = reference.getUsingBundles();
        usingBundles = usingBundles == null ? new Bundle[0] : usingBundles;
        for (Bundle bundle : usingBundles) {
            Servlet servlet = (Servlet) bundle.getBundleContext()
                    .getService(reference);
            if (servlet instanceof OSGiVaadinServlet) {
                ServletContext servletContext = ((VaadinServlet) servlet)
                        .getServletContext();
                Lookup servletLookup = new VaadinServletContext(servletContext)
                        .getAttribute(Lookup.class);
                if (servletLookup == lookup) {
                    servlets.add(servlet);
                }
            }
        }
        return servlets;
    }

}
