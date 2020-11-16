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

import javax.servlet.ServletContainerInitializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.LoggerFactory;

/**
 * Bundle tracker to discover all classes in active bundles.
 * <p>
 * The tracker scans for all classes in active bundles which have
 * <b>Vaadin-OSGi-Extender</b> header and report them to the
 * {@link ServletContainerInitializerClasses} instance.
 * 
 * OSGi ServiceComponentRuntime creates the Service immediate while activating
 * the bundle.
 * 
 * @author Vaadin Ltd
 * @since
 */
public class VaadinBundleTracker extends BundleTracker<Bundle> {

    private Bundle flowServerBundle;

    private Executor executor = Executors.newSingleThreadExecutor();

    private final ServletContainerInitializerClasses initializerClasses;

    /**
     * Creates a new instance of a bundle tracker. OSGi ServiceComponentRuntime
     * injects the BundleContext because of the @Activate
     * 
     * @param context
     *            the {@code BundleContext} against which the tracking is done
     */
    public VaadinBundleTracker(BundleContext context,
            ServletContainerInitializerClasses initializerClasses) {
        super(context, Bundle.ACTIVE | Bundle.RESOLVED, null);
        this.initializerClasses = initializerClasses;
    }

    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        if ((bundle.getState() & Bundle.ACTIVE) != 0) {
            // Don't scan every individual bundle until flow-server is active
            if (isServerModule(bundle)) {
                flowServerBundle = bundle;
                // First: scan for servlet context initializers in flow-server
                // bundle to reuse the same logic
                executor.execute(this::scanContextInitializers);
                // Now scan all active bundles for all classes instead of
                // scanning every inidividual activated bundle/
                executor.execute(this::scanActiveBundles);
            } else if (flowServerBundle != null
                    && (flowServerBundle.getState() & Bundle.ACTIVE) != 0) {
                // If flow-server bundle is already active then scan bundle for
                // classes
                executor.execute(() -> scanActivatedBundle(bundle));
            }
        } else if (event != null
                && ((event.getType() & BundleEvent.STOPPED) > 0)
                && isVaadinExtender(bundle)) {
            // Remove all bundle classes once the bundle becomes stopped
            initializerClasses.removeScannedClasses(bundle.getBundleId());
        }
        return bundle;
    }

    private boolean isServerModule(Bundle bundle) {
        return "com.vaadin.flow.server".equals(bundle.getSymbolicName());
    }

    @SuppressWarnings("unchecked")
    private void scanContextInitializers() {
        Map<Long, Collection<Class<?>>> map = new HashMap<>();
        scanClasses(flowServerBundle, map, this::handleFlowServerClassError);
        Collection<Class<?>> classes = map.get(flowServerBundle.getBundleId());

        Predicate<Class<?>> isInterface = Class::isInterface;

        Collection<Class<? extends ServletContainerInitializer>> initializers = classes
                .stream()
                .filter(ServletContainerInitializer.class::isAssignableFrom)
                .filter(isInterface.negate())
                .map(clazz -> (Class<? extends ServletContainerInitializer>) clazz)
                .collect(Collectors.toList());
        initializerClasses.setServletContainerInitializers(initializers);
    }

    private void scanActivatedBundle(Bundle bundle) {
        if (!isActive(bundle) || !isVaadinExtender(bundle)) {
            return;
        }
        if (initializerClasses.hasInitializers()) {
            Map<Long, Collection<Class<?>>> map = new HashMap<>();
            scanClasses(bundle, map, this::handleBundleClassError);
            initializerClasses.addScannedClasses(map);
        } else {
            executor.execute(() -> scanActivatedBundle(bundle));
        }
    }

    private void scanActiveBundles() {
        Map<Long, Collection<Class<?>>> map = new HashMap<>();
        Stream.of(flowServerBundle.getBundleContext().getBundles())
                .filter(this::isActive).filter(this::isVaadinExtender)
                .forEach(activeBundle -> scanClasses(activeBundle, map,
                        this::handleBundleClassError));
        initializerClasses.addScannedClasses(map);
    }

    private boolean isActive(Bundle bundle) {
        return (bundle.getState() & Bundle.ACTIVE) > 0;
    }

    private boolean isVaadinExtender(Bundle bundle) {
        return !isServerModule(bundle) && Boolean.TRUE.toString()
                .equals(bundle.getHeaders().get("Vaadin-OSGi-Extender"));
    }

    private void handleFlowServerClassError(String className,
            Throwable throwable) {
        LoggerFactory.getLogger(VaadinBundleTracker.class)
                .trace("Couldn't load class '{}'", className, throwable);
    }

    private void handleBundleClassError(String className, Throwable throwable) {
        LoggerFactory.getLogger(VaadinBundleTracker.class)
                .warn("Couldn't load class '{}'", className, throwable);
    }

    private void scanClasses(Bundle bundle, Map<Long, Collection<Class<?>>> map,
            BiConsumer<String, Throwable> throwableHandler) {
        BundleWiring wiring = bundle.adapt(BundleWiring.class);

        // get all .class resources of this bundle
        Collection<String> classes = wiring.listResources("/", "*.class",
                /*
                 * Two options: recursive to visit all resources including
                 * sub-directories, and limit resources only to the current
                 * wiring (bundle) avoiding possibly returned classes from
                 * dependencies
                 */
                BundleWiring.LISTRESOURCES_RECURSE
                        | BundleWiring.LISTRESOURCES_LOCAL);

        Collection<Class<?>> bundleClasses = new ArrayList<>();

        for (String clazz : classes) {
            String className = clazz.replaceAll("\\.class$", "").replace('/',
                    '.');
            if (bundle.equals(flowServerBundle)
                    && !className.startsWith("com.vaadin")) {
                continue;
            }
            if ("module-info".equals(className)) {
                // New modular Java info class which we are not interested in
                continue;
            }
            try {
                bundleClasses.add(bundle.loadClass(className));
            } catch (ClassNotFoundException | NoClassDefFoundError exception) {
                throwableHandler.accept(className, exception);
            }
        }
        map.put(bundle.getBundleId(), bundleClasses);
    }

}
