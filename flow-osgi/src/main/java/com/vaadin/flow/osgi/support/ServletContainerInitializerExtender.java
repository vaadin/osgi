/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;

/**
 * Service to start bundle tracker.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(immediate = true)
public class ServletContainerInitializerExtender {

    private BundleTracker<Bundle> tracker;

    @Reference
    private ServletContainerInitializerClasses initializerClasses;

    /**
     * Activates the component.
     * 
     * @param context
     *            the provided bundle context
     */
    @Activate
    public void activate(BundleContext context) {
        tracker = new VaadinBundleTracker(context, initializerClasses);
        tracker.open();
    }

    /**
     * Deactivate the component.
     */
    @Deactivate
    public void deactivate() {
        tracker.close();
    }
}
