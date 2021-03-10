/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Developer License
 * 4.0 (CVDLv4).
 *
 *
 * For the full License, see <https://vaadin.com/license/cvdl-4.0>.
 */
package com.vaadin.flow.osgi.support;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.ApplicationClassLoaderAccess;
import com.vaadin.flow.internal.VaadinContextInitializer;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.pro.licensechecker.LicenseChecker;

/**
 * Initialize {@link Lookup} and register internal resources (client engine
 * resource and push resources).
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(immediate = true, service = { VaadinServiceInitListener.class,
        HttpSessionListener.class, ServletContextListener.class,
        OSGiVaadinInitialization.class }, scope = ServiceScope.SINGLETON, property = {
                HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER + "=true",
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=("
                        + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME
                        + "=*)" })
public class OSGiVaadinInitialization implements VaadinServiceInitListener,
        HttpSessionListener, ServletContextListener {

    @Reference
    private ServletContainerInitializerClasses initializerClasses;

    private static final VaadinServletMarker MARKER_INSTANCE = new VaadinServletMarker();

    private static final String PROJECT_NAME = "vaadin-osgi";

    private static final String VERSION = readVersion();

    private static final class VaadinServletMarker {
    }

    static class IllegalContextState extends Exception {

        private IllegalContextState(String msg) {
            super(msg);
        }

    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        VaadinService service = event.getSource();
        VaadinContext context = service.getContext();

        checkLicense(service.getDeploymentConfiguration());

        // associate servlet context with Vaadin servlet/service
        context.setAttribute(VaadinServletMarker.class, MARKER_INSTANCE);
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        /*
         * Cleans up servlet context in case it's not associated with some
         * Vaadin servlet/service
         */
        ServletContext servletContext = event.getSession().getServletContext();

        VaadinServletContext vaadinContext = new VaadinServletContext(
                servletContext);
        VaadinServletMarker attribute = vaadinContext
                .getAttribute(VaadinServletMarker.class);
        if (attribute == null) {
            vaadinContext.removeAttribute(Lookup.class);
            vaadinContext.removeAttribute(VaadinContextInitializer.class);
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        // no op
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();

        VaadinServletContext context = new VaadinServletContext(servletContext);

        // ServletContextListener::contextInitialized should be called once per
        // servlet context if the class instance is used as a
        // ServletContextListener according to the Servlet spec (or HTTP
        // Whiteboard spec) but in our OSGi support OSGiVaadinServlet may call
        // this method directly for OSGiVaadinInitialization class instance. So
        // to avoid executing the logic here several times let's first check
        // whether initialization has been already done.
        if (context.getAttribute(Lookup.class) != null) {
            return;
        }

        ApplicationClassLoaderAccess classLoaderAccess = context
                .getAttribute(ApplicationClassLoaderAccess.class);
        context.getAttribute(VaadinContextInitializer.class,
                () -> (VaadinContextInitializer) this::initContext);

        if (classLoaderAccess != null) {
            initContext(context);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        initializerClasses.removeContext(event.getServletContext());

        VaadinServletContext servletContext = new VaadinServletContext(
                event.getServletContext());

        ServletInitRequirementsTracker tracker = servletContext
                .getAttribute(ServletInitRequirementsTracker.class);
        if (tracker != null) {
            tracker.close();
            servletContext.removeAttribute(ServletInitRequirementsTracker.class);
        }
    }

    private void initContext(VaadinContext context) {
        context.removeAttribute(VaadinContextInitializer.class);

        VaadinServletContext servletContext = (VaadinServletContext) context;
        try {
            ServletInitRequirementsTracker tracker = new ServletInitRequirementsTracker(
                    findBundle(servletContext), servletContext,
                    initializerClasses);
            servletContext.setAttribute(tracker);
            tracker.open();
        } catch (IllegalContextState exception) {
            LoggerFactory.getLogger(OSGiVaadinInitialization.class)
                    .warn("Couldn't initialize Vaadin Context", exception);
        }
    }

    private Bundle findBundle(VaadinContext context)
            throws IllegalContextState {
        ApplicationClassLoaderAccess classLoaderAccess = context
                .getAttribute(ApplicationClassLoaderAccess.class);
        if (classLoaderAccess == null) {
            throw new IllegalContextState(
                    ApplicationClassLoaderAccess.class.getName()
                            + "' instance is not available in "
                            + VaadinContext.class.getName());
        }
        ClassLoader classloader = classLoaderAccess.getClassloader();

        if (classloader instanceof BundleReference) {
            return ((BundleReference) classloader).getBundle();
        } else {
            throw new IllegalStateException(
                    "Unexpected classloader for the web app '" + classloader
                            + "'. It's not possible to get an OSGi bundle from it");
        }
    }

    static void checkLicense(AbstractConfiguration configuration) {
        if (!configuration.isProductionMode()) {
            LicenseChecker.checkLicense(PROJECT_NAME, VERSION);
        }
    }

    private static String readVersion() {
        URL versionUrl = FrameworkUtil.getBundle(OSGiVaadinInitialization.class)
                .getResource("vaadin-osgi-version.properties");
        if (versionUrl == null) {
            throw new RuntimeException(
                    "Couldn't find 'vaadin-osgi-version.properties' file in the bundle");
        }
        Properties properties = new Properties();
        try (InputStream stream = versionUrl.openStream()) {
            properties.load(stream);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Couldn't read the 'vaadin-osgi-version.properties' file",
                    exception);
        }
        return properties.getProperty("vaadin.osgi.version");
    }
}
