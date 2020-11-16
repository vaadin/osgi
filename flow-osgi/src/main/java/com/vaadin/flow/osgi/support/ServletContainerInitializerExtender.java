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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.BundleTracker;

/**
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(immediate = true)
public class ServletContainerInitializerExtender {

    private BundleTracker<Bundle> tracker;

    @Reference
    private ServletContainerInitializerClasses initializerClasses;

    @Activate
    public void activate(BundleContext context) {
        tracker = new VaadinBundleTracker(context, initializerClasses);
        tracker.open();
    }

    @Deactivate
    public void deactivate() {
        tracker.close();
    }
}
