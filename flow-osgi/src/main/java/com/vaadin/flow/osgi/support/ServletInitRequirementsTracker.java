/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.ApplicationClassLoaderAccess;
import com.vaadin.flow.internal.VaadinContextInitializer;
import com.vaadin.flow.server.HandlerHelper;
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
class ServletInitRequirementsTracker extends
        ServiceTracker<ServletInitializationRequirements, ServletInitializationRequirements>
        implements BundleListener {

    private final Bundle webAppBundle;

    private final VaadinServletContext servletContext;

    private final ServletContainerInitializerClasses initializerClasses;

    /**
     * 
     * HTTP Whiteboard Resource service doesn't work reliably neither in Felix
     * Jetty nor in PAX web. So resources are registered via this dedicated
     * servlet which uses the existing {@link ServletContextHelper} service
     * registered for the same context path as the Vaadin servlet.
     *
     */
    private static class ResourceServlet extends HttpServlet {

        private final Bundle bundle;

        private final String path;

        private ResourceServlet(Bundle bundle, String path) {
            this.bundle = bundle;
            this.path = path;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            String pathInfo = req.getPathInfo();

            if (pathInfo == null) {
                resp.setStatus(HttpURLConnection.HTTP_NOT_FOUND);
                return;
            }
            if (HandlerHelper.isPathUnsafe(pathInfo)) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            URL resource = bundle.getResource(path + pathInfo);
            if (resource == null) {
                resp.setStatus(HttpURLConnection.HTTP_NOT_FOUND);
                return;
            }
            try (InputStream stream = resource.openStream()) {
                IOUtils.copy(stream, resp.getOutputStream());
            }
        }
    }

    private abstract static class ResourceBundleTracker
            extends BundleTracker<Bundle> implements BundleListener {

        private final String symbolicName;

        private final String contextPath;

        private ServiceRegistration<Servlet> resourceRegistration;

        private ServiceListener contextHelperListener;

        private WeakReference<Bundle> resourceBundle;

        private ResourceBundleTracker(Bundle webAppBundle, String symbolicName,
                String contextPath) {
            super(webAppBundle.getBundleContext(),
                    Bundle.ACTIVE | Bundle.RESOLVED, null);
            this.contextPath = contextPath;
            this.symbolicName = symbolicName;
            webAppBundle.getBundleContext().addBundleListener(this);
        }

        @Override
        public Bundle addingBundle(Bundle bundle, BundleEvent event) {
            Bundle result = super.addingBundle(bundle, event);
            if ((bundle.getState() & Bundle.ACTIVE) != 0
                    && symbolicName.equals(bundle.getSymbolicName())) {
                resourceBundle = new WeakReference<>(bundle);
                registerResource(bundle);
            }
            return result;
        }

        @Override
        public void bundleChanged(BundleEvent event) {
            if ((event.getType() & BundleEvent.STOPPED) != 0
                    && event.getBundle().getBundleId() == context.getBundle()
                            .getBundleId()) {
                stop();
            }
        }

        protected abstract String getResourceURI();

        protected abstract String getResourcePath();

        private void stop() {
            close();
            context.removeBundleListener(this);
            Bundle bundle = resourceBundle.get();
            if (bundle == null) {
                return;
            }
            if (contextHelperListener != null) {
                bundle.getBundleContext()
                        .removeServiceListener(contextHelperListener);
            }

            if (resourceRegistration != null
                    && bundle.getRegisteredServices().length > 0) {
                resourceRegistration.unregister();
            }
        }

        private void registerResource(Bundle bundle) {
            Dictionary<String, String> properties = new Hashtable<String, String>();
            properties.put(
                    HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
                    getResourceURI());
            String contextName = getContextName();
            if (contextName != null) {
                properties.put(
                        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                        "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME
                                + "=" + contextName + ")");
            }
            resourceRegistration = bundle.getBundleContext().registerService(
                    Servlet.class,
                    new ResourceServlet(bundle, getResourcePath()), properties);
        }

        private String getContextName() {
            if ("/".equals(contextPath)) {
                // default context path doesn't require any ServletContextHelper
                return null;
            }
            try {
                ServiceReference<?>[] references = context
                        .getAllServiceReferences(
                                ServletContextHelper.class.getName(), null);
                if (references == null) {
                    reportEmptyContextHelpers();
                    return null;
                }
                Map<Long, ServiceReference<?>> matchedReferences = filterReferencesByContextPath(
                        references);
                if (matchedReferences.isEmpty()) {
                    reportEmptyContextHelpers();
                    return null;
                }
                if (matchedReferences.size() == 1) {
                    return getContextNameProperty(
                            matchedReferences.values().iterator().next());
                } else {
                    ServiceReference<?> reference = matchedReferences
                            .get(context.getBundle().getBundleId());
                    if (reference == null) {
                        reference = matchedReferences.values().iterator()
                                .next();
                        trackContextHelperReferences(reference);
                    }
                    return getContextNameProperty(reference);
                }
            } catch (InvalidSyntaxException exception) {
                LoggerFactory.getLogger(OSGiVaadinInitialization.class).error(
                        "Couldn't get all {} services to find the context name",
                        ServletContextHelper.class);
                return null;
            }
        }

        private void reportEmptyContextHelpers() {
            if (!"/".equals(contextPath)) {
                LoggerFactory.getLogger(ResourceBundleTracker.class).error(
                        "No {} services found matched context path '{}'",
                        ServletContextHelper.class.getSimpleName(),
                        contextPath);
            }
        }

        private void trackContextHelperReferences(
                ServiceReference<?> reference) {
            LoggerFactory.getLogger(ResourceBundleTracker.class).debug(
                    "Found several {} services matched context path '{}'. "
                            + "The first one is used to register a resource and it's tracked for changes.",
                    ServletContextHelper.class.getSimpleName(), contextPath);
            contextHelperListener = event -> {
                if (reference.equals(event.getServiceReference())
                        && event.getType() != ServiceEvent.REGISTERED) {
                    if (contextHelperListener != null) {
                        reference.getBundle().getBundleContext()
                                .removeServiceListener(contextHelperListener);
                        contextHelperListener = null;
                    }
                    if (resourceRegistration != null) {
                        Bundle bundle = resourceRegistration.getReference()
                                .getBundle();
                        resourceRegistration.unregister();
                        registerResource(bundle);
                    }
                }
            };
            reference.getBundle().getBundleContext()
                    .addServiceListener(contextHelperListener);
        }

        private String getContextNameProperty(ServiceReference<?> reference) {
            return reference.getProperty(
                    HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME)
                    .toString();
        }

        private Map<Long, ServiceReference<?>> filterReferencesByContextPath(
                ServiceReference<?>[] references) {
            Map<Long, ServiceReference<?>> matchedReferences = new HashMap<>();
            for (ServiceReference<?> reference : references) {
                Bundle[] usingBundles = reference.getUsingBundles();
                if (usingBundles != null && usingBundles.length > 0) {
                    Object path = reference.getProperty(
                            HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);
                    if (contextPath.equals(path)) {
                        matchedReferences.put(
                                reference.getBundle().getBundleId(), reference);
                    }
                }
            }
            return matchedReferences;
        }

    }

    private static class PushResourceBundleTracker
            extends ResourceBundleTracker {

        private PushResourceBundleTracker(Bundle webAppBundle,
                String contextPath) {
            super(webAppBundle, "com.vaadin.flow.push", contextPath);
        }

        @Override
        protected String getResourceURI() {
            return "/VAADIN/static/push/*";
        }

        @Override
        protected String getResourcePath() {
            return "/META-INF/resources/VAADIN/static/push";
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
            Bundle bundle = getWebAppBundle();
            try {
                Collection<ServiceReference<T>> references = bundle
                        .getBundleContext()
                        .getServiceReferences(serviceClass, null);
                List<T> services = new ArrayList<>(references.size());
                for (ServiceReference<T> reference : references) {
                    T service = bundle.getBundleContext().getService(reference);
                    if (service != null) {
                        services.add(service);
                    }
                }
                return services;
            } catch (InvalidSyntaxException e) {
                LoggerFactory.getLogger(ServletInitRequirementsTracker.class)
                        .error("Unexpected invalid filter expression", e);
                assert false : "Implementation error: Unexpected invalid filter exception is "
                        + "thrown even though the service filter is null. Check the exception and update the impl";
            }

            return Collections.emptyList();
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
    ServletInitRequirementsTracker(Bundle webAppBundle,
            VaadinServletContext context,
            ServletContainerInitializerClasses initializerClasses) {
        super(webAppBundle.getBundleContext(),
                ServletInitializationRequirements.class, null);
        this.webAppBundle = webAppBundle;
        servletContext = context;
        this.initializerClasses = initializerClasses;
        webAppBundle.getBundleContext().addBundleListener(this);
    }

    @Override
    public ServletInitializationRequirements addingService(
            ServiceReference<ServletInitializationRequirements> reference) {
        ServletInitializationRequirements requirements = super.addingService(
                reference);
        ServletInitRequirementsTracker tracker = servletContext
                .getAttribute(ServletInitRequirementsTracker.class);
        if (tracker != null) {
            stop();
            servletContext
                    .removeAttribute(ServletInitRequirementsTracker.class);
        }
        initializeLookup();
        return requirements;
    }

    private boolean isFrameworkStarted() {
        Optional<Bundle> framework = Stream.of(context.getBundles()).filter(
                bundle -> Framework.class.isAssignableFrom(bundle.getClass()))
                .findFirst();
        if (framework.isPresent()) {
            return (Bundle.ACTIVE & framework.get().getState()) > 0;
        }
        return false;
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

        if (isFrameworkStarted()) {
            registerResoures(servletContext);
        } else {
            FrameworkListener listener = new FrameworkListener() {

                @Override
                public void frameworkEvent(FrameworkEvent event) {
                    if (isFrameworkStarted()) {
                        registerResoures(servletContext);
                        context.removeFrameworkListener(this);
                    }
                }
            };
            context.addFrameworkListener(listener);
        }

        OSGiVaadinInitialization
                .checkLicense(ApplicationConfiguration.get(servletContext));

        getRegisteredVaadinServlets().stream()
                .filter(this::isUninitializedServlet).forEach(
                        servlet -> handleUninitializedServlet(lookup, servlet));

        initializerClasses.addContext(servletContext.getContext());
    }

    private Collection<VaadinServlet> getRegisteredVaadinServlets() {
        Set<VaadinServlet> result = new HashSet<>();
        try {
            Stream.of(context.getAllServiceReferences(Servlet.class.getName(),
                    null))
                    .forEach(reference -> collectVaadinServlets(result,
                            reference));
        } catch (InvalidSyntaxException e) {
            LoggerFactory.getLogger(ServletInitRequirementsTracker.class)
                    .error("Unexpected invalid filter expression", e);
            assert false : "Implementation error: Unexpected invalid filter exception is "
                    + "thrown even though the service filter is null. Check the exception and update the impl";
        }
        return result;
    }

    private void collectVaadinServlets(Set<VaadinServlet> servlets,
            ServiceReference<?> reference) {
        Bundle[] bundles = reference.getUsingBundles();
        if (bundles == null) {
            return;
        }
        Stream.of(bundles)
                .map(bundle -> bundle.getBundleContext().getService(reference))
                .filter(VaadinServlet.class::isInstance)
                .map(VaadinServlet.class::cast).forEach(servlets::add);
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

    private boolean isUninitializedServlet(VaadinServlet servlet) {
        return servlet.getServletConfig() != null
                && servlet.getService() == null;
    }

    private void registerResoures(VaadinContext context) {
        String contextPath = ((VaadinServletContext) context).getContext()
                .getContextPath();

        if (contextPath.isEmpty()) {
            contextPath = "/";
        }

        registerPushResources(contextPath);
    }

    private void registerPushResources(String contextPath) {
        ResourceBundleTracker resourceBoundleTracker = new PushResourceBundleTracker(
                webAppBundle, contextPath);
        resourceBoundleTracker.open();
    }

}
