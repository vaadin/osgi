/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.googlecode.gentyref.GenericTypeReflector;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.startup.ClassLoaderAwareServletContainerInitializer;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;

/**
 * Manages scanned classes inside OSGi container.
 * <p>
 * It doesn't do anything outside of OSGi.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(scope = ServiceScope.SINGLETON, service = ServletContainerInitializerClasses.class)
public final class ServletContainerInitializerClasses {

    private final AtomicReference<Collection<Class<? extends ServletContainerInitializer>>> initializerClasses = new AtomicReference<>();

    private final Map<Long, Collection<Class<?>>> cachedClasses = new ConcurrentHashMap<>();

    private final Set<ServletContextReference> contexts = Collections
            .newSetFromMap(new ConcurrentHashMap<>());

    private static class ServletContextReference
            extends WeakReference<ServletContext> {

        private ServletContextReference(ServletContext context) {
            super(context);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!obj.getClass().equals(ServletContextReference.class)) {
                return false;
            }
            return Objects.equals(((ServletContextReference) obj).get(), get());
        }

        @Override
        public int hashCode() {
            ServletContext servletContext = get();
            if (servletContext == null) {
                return super.hashCode();
            }
            return servletContext.hashCode();
        }

    }

    public ServletContainerInitializerClasses() {
        // The class is a singleton. Avoid instantiation outside of the class.
        UsageStatistics.markAsUsed("flow/osgi", getOSGiVersion());
    }

    /**
     * Sets the discovered servlet context initializer classes.
     * <p>
     * The OSGi bundle tracker is used to scan all classes in bundles and it
     * also scans <b>flow-server</b> module for servlet initializer classes.
     * They are set using this method once they are collected.
     *
     * @param contextInitializers
     *            servlet context initializer classes
     */
    public void setServletContainerInitializers(
            Collection<Class<? extends ServletContainerInitializer>> contextInitializers) {
        assert contextInitializers != null;
        initializerClasses.set(new ArrayList<>(contextInitializers));
    }

    /**
     * Checks whether the servlet initializers are discovered.
     *
     * @return {@code true} if servlet initializers are set, {@code false}
     *         otherwise
     */
    public boolean hasInitializers() {
        return initializerClasses.get() != null;
    }

    /**
     * Adds scanned classes in active bundles.
     * <p>
     * The map contains a bundle id as a key and classes discovered in the
     * bundle as a value.
     *
     * @param extenderClasses
     *            a map with discovered classes in active bundles
     */
    public void addScannedClasses(
            Map<Long, Collection<Class<?>>> extenderClasses) {
        cachedClasses.putAll(extenderClasses);
        List<InvalidApplicationConfigurationException> thrown = new ArrayList<>();
        Iterator<ServletContextReference> iterator = contexts.iterator();
        while (iterator.hasNext()) {
            WeakReference<ServletContext> ref = iterator.next();
            ServletContext context = ref.get();
            if (context == null) {
                iterator.remove();
            }
            try {
                resetContextInitializers(context);
            } catch (InvalidApplicationConfigurationException exception) {
                // don't stop context initializers processing for different
                // context, instead initialize all the contexts and then throw
                thrown.add(exception);
            }
        }
        // Now if there was an exception throw it.
        // This is a workaround for #9417 which causes an exception being thrown
        // by VaadinAppShellInitializer in our ITs
        if (!thrown.isEmpty()) {
            throw thrown.get(0);
        }
    }

    /**
     * Removes classes from the bundle identified by the {@code bundleId}.
     * <p>
     * When a bundle becomes inactive its classes should not be used anymore.
     * This method removes the classes from the bundle from the collection of
     * discovered classes.
     *
     * @param bundleId
     *            the bundle identifier
     */
    public void removeScannedClasses(Long bundleId) {
        cachedClasses.remove(bundleId);
        contexts.forEach(this::resetContextInitializers);
    }

    /**
     * Adds the {@code servletContext} to run servlet context initializers
     * against of.
     * 
     * @param servletContext
     *            the servlet context to run servlet context initializers
     */
    public void addContext(ServletContext servletContext) {
        contexts.add(new ServletContextReference(servletContext));

        if (hasInitializers()) {
            resetContextInitializers(servletContext);
        }
    }

    /**
     * Removes the {@code servletContext} from tracking contexts for servlet
     * context initializers.
     * 
     * @param servletContext
     *            the servlet context from tracking contexts
     */
    public void removeContext(ServletContext servletContext) {
        contexts.remove(new ServletContextReference(servletContext));
    }

    private void resetContextInitializers(ServletContextReference reference) {
        ServletContext context = reference.get();
        if (context != null) {
            resetContextInitializers(context);
        }
    }

    private void resetContextInitializers(ServletContext context) {
        /*
         * exclude dev mode initializer (at least for now) because it doesn't
         * work in its current state anyway (so it's no-op) but its initial
         * calls breaks assumptions about Servlet registration in OSGi.
         * 
         * Lookup is set immediately in the context, so no need to initialize it
         */
        initializerClasses.get().stream().filter(clazz -> !clazz.getName()
                .equals("com.vaadin.base.devserver.startup.DevModeInitializer")
                && !clazz.getName()
                .equals("com.vaadin.flow.server.startup.DevModeInitializer")
                && !clazz.equals(LookupServletContainerInitializer.class))
                .map(ReflectTools::createInstance)
                .forEach(initializer -> handleTypes(initializer, context));
    }

    private void handleTypes(ServletContainerInitializer initializer,
            ServletContext context) {
        Optional<HandlesTypes> handleTypes = AnnotationReader
                .getAnnotationFor(initializer.getClass(), HandlesTypes.class);
        /*
         * Every initializer should be an instance of
         * ClassLoaderAwareServletContainerInitializer : there is a test which
         * forces this. So assert should be enough here.
         */
        assert initializer instanceof ClassLoaderAwareServletContainerInitializer;
        try {
            // don't use onStartup method because a fake servlet context is
            // passed here: no need to detect classloaders in OSGi case
            ((ClassLoaderAwareServletContainerInitializer) initializer)
                    .process(filterClasses(handleTypes.orElse(null)), context);
        } catch (ServletException e) {
            throw new RuntimeException(
                    "Couldn't run servlet context initializer "
                            + initializer.getClass(),
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Class<?>> filterClasses(HandlesTypes typesAnnotation) {
        Set<Class<?>> result = new HashSet<>();
        if (typesAnnotation == null) {
            cachedClasses.forEach((bundle, classes) -> result.addAll(classes));
        } else {
            Class<?>[] requestedTypes = typesAnnotation.value();

            Predicate<Class<?>> isAnnotation = Class::isAnnotation;

            List<Class<? extends Annotation>> annotations = Stream
                    .of(requestedTypes).filter(isAnnotation)
                    .map(clazz -> (Class<? extends Annotation>) clazz)
                    .collect(Collectors.toList());

            List<Class<?>> superTypes = Stream.of(requestedTypes)
                    .filter(isAnnotation.negate()).collect(Collectors.toList());

            Predicate<Class<?>> hasType = clazz -> annotations.stream()
                    .anyMatch(annotation -> AnnotationReader
                            .getAnnotationFor(clazz, annotation).isPresent())
                    || superTypes.stream()
                            .anyMatch(superType -> GenericTypeReflector
                                    .isSuperType(HasErrorParameter.class,
                                            clazz));

            cachedClasses.forEach((bundle, classes) -> result.addAll(classes
                    .stream().filter(hasType).collect(Collectors.toList())));

        }
        return result;
    }

    private static String getOSGiVersion() {
        try {
            Bundle osgiBundle = org.osgi.framework.FrameworkUtil
                    .getBundle(Bundle.class);
            return osgiBundle.getVersion().toString();
        } catch (Throwable throwable) {
            // just eat it so that any failure in the version detection
            // doesn't break OSGi usage
            LoggerFactory.getLogger(ServletContainerInitializerClasses.class)
                    .info("Unable to detect used OSGi framework version due to "
                            + throwable.getMessage());
        }
        return null;
    }

}
