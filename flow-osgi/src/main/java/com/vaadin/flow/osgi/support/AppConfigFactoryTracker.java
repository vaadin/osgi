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

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.ApplicationClassLoaderAccess;
import com.vaadin.flow.internal.VaadinContextInitializer;
import com.vaadin.flow.osgi.support.OSGiVaadinInitialization.ResourceContextHelperFactory;
import com.vaadin.flow.osgi.support.OSGiVaadinInitialization.ResourceService;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;

/**
 * Internal implementation class.
 * <p>
 * Initializes the {@link Lookup} instance for the {@link VaadinServletContext}
 * as soon as {@link ApplicationConfigurationFactory} service is registered.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
class AppConfigFactoryTracker extends
        ServiceTracker<ApplicationConfigurationFactory, ApplicationConfigurationFactory>
        implements BundleListener {

    private final Bundle webAppBundle;

    private final VaadinServletContext servletContext;

    private final ServletContainerInitializerClasses initializerClasses;

    private static class ResourceBundleTracker extends BundleTracker<Bundle>
            implements BundleListener {

        private final Dictionary<String, String> properties;

        private final String symbolicName;

        private ResourceBundleTracker(Bundle bundle, String symbolicName,
                Dictionary<String, String> props) {
            super(bundle.getBundleContext(), Bundle.ACTIVE | Bundle.RESOLVED,
                    null);
            properties = props;
            this.symbolicName = symbolicName;
            bundle.getBundleContext().addBundleListener(this);
        }

        @Override
        public Bundle addingBundle(Bundle bundle, BundleEvent event) {
            Bundle result = super.addingBundle(bundle, event);
            if ((bundle.getState() & Bundle.ACTIVE) != 0
                    && symbolicName.equals(bundle.getSymbolicName())) {
                bundle.getBundleContext().registerService(ResourceService.class,
                        new ResourceService(), properties);
                stop();
            }
            return result;
        }

        @Override
        public void bundleChanged(BundleEvent event) {
            if ((event.getType() & BundleEvent.STOPPED) != 0) {
                stop();
            }
        }

        private void stop() {
            close();
            context.removeBundleListener(this);
        }
    }

    static class OsgiLookupImpl implements Lookup {

        private final VaadinServletContext context;

        private final Bundle webAppBundle;

        private OsgiLookupImpl(Bundle webAppBundle,
                VaadinServletContext context) {
            this.webAppBundle = webAppBundle;
            this.context = context;
        }

        @Override
        public <T> T lookup(Class<T> serviceClass) {
            ServiceReference<T> reference = getWebAppBundle().getBundleContext()
                    .getServiceReference(serviceClass);
            if (reference == null) {
                LoggerFactory.getLogger(OsgiLookupImpl.class)
                        .debug("No service found for '{}' SPI", serviceClass);
                return null;
            }
            return getWebAppBundle().getBundleContext().getService(reference);
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> serviceClass) {
            return OSGiVaadinInitialization.lookupAll(getWebAppBundle(),
                    serviceClass);
        }

        private Bundle getWebAppBundle() {
            return webAppBundle;
        }

    }

    /**
     * Creates a new tracker instance for the {@code webAppBundle} and Vaadin
     * {@code context}.
     * 
     * @param webAppBundle
     *            the web application bundle
     * @param context
     *            the Vaadin servlet context
     * @param initializerClasses
     *            {@link ServletContainerInitializerClasses} instance
     */
    AppConfigFactoryTracker(Bundle webAppBundle, VaadinServletContext context,
            ServletContainerInitializerClasses initializerClasses) {
        super(webAppBundle.getBundleContext(),
                ApplicationConfigurationFactory.class, null);
        this.webAppBundle = webAppBundle;
        servletContext = context;
        this.initializerClasses = initializerClasses;
        webAppBundle.getBundleContext().addBundleListener(this);
    }

    @Override
    public ApplicationConfigurationFactory addingService(
            ServiceReference<ApplicationConfigurationFactory> reference) {
        ApplicationConfigurationFactory factory = super.addingService(
                reference);
        AppConfigFactoryTracker tracker = servletContext
                .getAttribute(AppConfigFactoryTracker.class);
        if (tracker != null) {
            stop();
            servletContext.removeAttribute(AppConfigFactoryTracker.class);
        }
        initializeLookup();
        return factory;
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if ((event.getType() & BundleEvent.STOPPED) != 0) {
            stop();
        }
    }

    private void stop() {
        close();
        context.removeBundleListener(this);
    }

    private void initializeLookup() {
        /*
         * There are two cases:
         * 
         * - the servlet context is initialized before all of the Servlets or
         * 
         * - after at least one of the servlets with the same context.
         * 
         * SevletContext::getClassLoader doesn't return the container the web
         * app classloader for the servlet context instance here (in the
         * "contextInitialized" method). It returns the web app classloader for
         * the servlet context which is passed to the Servlet::init method: in
         * fact two servlet context instances are different even though they
         * share the same attributes and values. So to be able to identify the
         * servlet context (which is coming from Servlet) and the
         * "servletContext" instance here the Lookupx instance is used which is
         * set as an attribute and if the context "is the same" the attributes
         * should be the same.
         */

        // at this point at least one of the Servlet::init method should be
        // called and the classloader should be available

        // ensure the lookup is set into the context
        Lookup[] created = new Lookup[1];
        Lookup lookup = servletContext.getAttribute(Lookup.class, () -> {
            Lookup result = new OsgiLookupImpl(webAppBundle, servletContext);
            created[0] = result;
            return result;
        });
        if (lookup == null) {
            throw new IllegalStateException(
                    VaadinContextInitializer.class.getSimpleName()
                            + " has been executed but there is no "
                            + ApplicationClassLoaderAccess.class.getSimpleName()
                            + " instance available");
        }

        if (created[0] != lookup) {
            // The context has been already initialized if there is already
            // lookup instance: just return
            return;
        }

        registerResoures(servletContext);

        OSGiVaadinInitialization
                .checkLicense(ApplicationConfiguration.get(servletContext));

        Collection<Servlet> servlets = OSGiVaadinInitialization.lookupAll(
                FrameworkUtil.getBundle(OSGiVaadinInitialization.class),
                Servlet.class);
        for (Servlet servlet : servlets) {
            if (isUninitializedServlet(servlet)) {
                handleUninitializedServlet(lookup, servlet);
            }
        }

        initializerClasses.addContext(servletContext.getContext());
    }

    private void handleUninitializedServlet(Lookup lookup, Servlet servlet) {
        ServletContext ctx = servlet.getServletConfig().getServletContext();
        if (new VaadinServletContext(ctx)
                .getAttribute(Lookup.class) != lookup) {
            return;
        }
        try {
            servlet.init(servlet.getServletConfig());
        } catch (ServletException e) {
            LoggerFactory.getLogger(OSGiVaadinInitialization.class).error(
                    "Couldn't initialize {} {}",
                    VaadinServlet.class.getSimpleName(),
                    servlet.getClass().getName(), e);
        }
    }

    private boolean isUninitializedServlet(Object object) {
        if (object instanceof VaadinServlet) {
            VaadinServlet vaadinServlet = (VaadinServlet) object;
            return vaadinServlet.getServletConfig() != null
                    && vaadinServlet.getService() == null;
        }
        return false;
    }

    private void registerResoures(VaadinContext context) {
        String contextPath = ((VaadinServletContext) context).getContext()
                .getContextPath();

        if (contextPath.isEmpty()) {
            contextPath = "/";
        }

        BundleContext bundleContext = FrameworkUtil
                .getBundle(OSGiVaadinInitialization.class).getBundleContext();

        String contextName = generateUniqueContextName(contextPath);

        Dictionary<String, String> contextProps = new Hashtable<String, String>();
        // set unique name for the context
        contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME,
                contextName);
        contextProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH,
                contextPath);
        bundleContext.registerService(ServletContextHelper.class,
                new ResourceContextHelperFactory(), contextProps);

        registerClientResources(contextName);
        registerPushResources(contextName);
    }

    private String generateUniqueContextName(String contextPath) {
        String contextName = sanitizeContextName(contextPath);
        String name;
        if (contextName.isEmpty()) {
            name = "vaadinResourcesContext";
        } else {
            name = "vaadinResourcesContext." + contextName;
        }
        Set<String> contextNames = getAvailableContextNames();
        if (contextNames.contains(name)) {
            int i = 1;
            String result;
            do {
                result = name + i;
                i++;
            } while (contextNames.contains(result));
            return result;
        } else {
            return name;
        }
    }

    /**
     * Uses {@code suggestedName} as a basis to produce a context name with
     * "osgi.http.whiteboard.context.name" property via filtering via
     * "osgi.http.whiteboard.context.select".
     * 
     * @param suggestedName
     *            the basis to produce a context name
     * @return a name which can be used as a context name
     */
    private String sanitizeContextName(String suggestedName) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < suggestedName.length(); i++) {
            char nextChar = suggestedName.charAt(i);
            if (Character.isLetterOrDigit(nextChar) || nextChar == '.'
                    || nextChar == '_' || nextChar == '-') {
                builder.append(nextChar);
            }
        }
        return builder.toString();
    }

    private void registerClientResources(String contextName) {
        Dictionary<String, String> clientProps = new Hashtable<String, String>();
        clientProps.put(
                HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN,
                "/VAADIN/static/client/*");
        clientProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX,
                "/META-INF/resources/VAADIN/static/client");
        clientProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "="
                        + contextName + ")");

        ResourceBundleTracker resourceBoundleTracker = new ResourceBundleTracker(
                webAppBundle, "com.vaadin.flow.client", clientProps);
        resourceBoundleTracker.open();
    }

    private void registerPushResources(String contextName) {
        Dictionary<String, String> pushProps = new Hashtable<String, String>();
        pushProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN,
                "/VAADIN/static/push/*");
        pushProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX,
                "/META-INF/resources/VAADIN/static/push");
        pushProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "="
                        + contextName + ")");

        ResourceBundleTracker resourceBoundleTracker = new ResourceBundleTracker(
                webAppBundle, "com.vaadin.flow.push", pushProps);
        resourceBoundleTracker.open();
    }

    private Set<String> getAvailableContextNames() {
        BundleContext bundleContext = FrameworkUtil
                .getBundle(OSGiVaadinInitialization.class).getBundleContext();
        try {
            ServiceReference<?>[] references = bundleContext
                    .getAllServiceReferences(
                            ServletContextHelper.class.getName(), null);
            if (references == null) {
                return Collections.emptySet();
            }
            Set<String> contextNames = new HashSet<>();
            for (ServiceReference<?> reference : references) {
                Object nextName = reference.getProperty(
                        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);
                if (nextName != null) {
                    contextNames.add(nextName.toString());
                }
            }
            return contextNames;
        } catch (InvalidSyntaxException exception) {
            LoggerFactory.getLogger(OSGiVaadinInitialization.class).error(
                    "Couldn't get all {} services to generate unique context name",
                    ServletContextHelper.class);
            return Collections.emptySet();
        }
    }

}
