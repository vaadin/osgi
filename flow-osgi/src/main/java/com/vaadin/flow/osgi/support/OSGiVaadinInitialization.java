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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(service = { VaadinServiceInitListener.class,
        HttpSessionListener.class,
        ServletContextListener.class }, scope = ServiceScope.SINGLETON, property = HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER
                + "=true")
public class OSGiVaadinInitialization implements VaadinServiceInitListener,
        HttpSessionListener, ServletContextListener {

    @Reference
    private ServletContainerInitializerClasses initializerClasses;

    private static final VaadinServletMarker MARKER_INSTANCE = new VaadinServletMarker();

    private static final class VaadinServletMarker {
    }

    private static final class ResourcesContextHelper
            extends ServletContextHelper {

        private ResourcesContextHelper(Bundle bundle) {
            super(bundle);
        }
    }

    public static class ResourceService {

    }

    private static class ResourceContextHelperFactory
            implements ServiceFactory<ServletContextHelper> {

        @Override
        public ServletContextHelper getService(Bundle bundle,
                ServiceRegistration<ServletContextHelper> registration) {
            return new ResourcesContextHelper(bundle);
        }

        @Override
        public void ungetService(Bundle bundle,
                ServiceRegistration<ServletContextHelper> registration,
                ServletContextHelper service) {
            // no op
        }

    }

    private static class OsgiLookupImpl implements Lookup {

        @Override
        public <T> T lookup(Class<T> serviceClass) {
            Bundle bundle = FrameworkUtil
                    .getBundle(ServletContainerInitializerClasses.class);
            ServiceReference<T> reference = bundle.getBundleContext()
                    .getServiceReference(serviceClass);
            if (reference == null) {
                LoggerFactory.getLogger(OsgiLookupImpl.class)
                        .debug("No service found for '{}' SPI", serviceClass);
                return null;
            }
            return bundle.getBundleContext().getService(reference);
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> serviceClass) {
            Bundle bundle = FrameworkUtil
                    .getBundle(ServletContainerInitializerClasses.class);
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
                LoggerFactory.getLogger(OsgiLookupImpl.class)
                        .error("Unexpected invalid filter expression", e);
                assert false : "Implementation error: Unexpected invalid filter exception is "
                        + "thrown even though the service filter is null. Check the exception and update the impl";
            }

            return Collections.emptyList();
        }

    }

    @Override
    public void serviceInit(ServiceInitEvent event) {
        VaadinService service = event.getSource();
        VaadinContext context = service.getContext();
        // associate servlet context with Vaadin servlet/service
        context.setAttribute(VaadinServletMarker.class, MARKER_INSTANCE);

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
        // ensure the context is set into the context
        context.getAttribute(Lookup.class, () -> new OsgiLookupImpl());

        initializerClasses.addContext(servletContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        initializerClasses.removeContext(event.getServletContext());
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
        getBundle("com.vaadin.flow.client").ifPresent(bundle -> bundle
                .getBundleContext().registerService(ResourceService.class,
                        new ResourceService(), clientProps));
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
        getBundle("com.vaadin.flow.push").ifPresent(bundle -> bundle
                .getBundleContext().registerService(ResourceService.class,
                        new ResourceService(), pushProps));
    }

    private Optional<Bundle> getBundle(String symbolicName) {
        BundleContext bundleContext = FrameworkUtil
                .getBundle(OSGiVaadinInitialization.class).getBundleContext();
        return Stream.of(bundleContext.getBundles())
                .filter(bundle -> (bundle.getState() & Bundle.ACTIVE) != 0)
                .filter(bundle -> symbolicName.equals(bundle.getSymbolicName()))
                .findFirst();
    }

    private String generateUniqueContextName(String contextPath) {
        String name = "vaadinContext." + sanitizeContextName(contextPath);
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

    private Set<String> getAvailableContextNames() {
        BundleContext bundleContext = FrameworkUtil
                .getBundle(OSGiVaadinInitialization.class).getBundleContext();
        try {
            ServiceReference<?>[] references = bundleContext
                    .getAllServiceReferences(
                            ServletContextHelper.class.getName(), null);
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

}
