/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import com.vaadin.flow.di.ResourceProvider;

/**
 * OSGi capable implementation of {@link ResourceProvider}.
 * ServiceComponentRuntime activates this Service because of @Component
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(scope = ServiceScope.BUNDLE, service = { ResourceProvider.class })
public class OSGiResourceProvider implements ResourceProvider {

    private Bundle bundle;

    @Activate
    void activate(ComponentContext ctx) {
        bundle = ctx.getUsingBundle();
    }

    @Override
    public URL getApplicationResource(String path) {
        return bundle.getResource(path);
    }

    @Override
    public List<URL> getApplicationResources(String path) throws IOException {
        Enumeration<URL> resources = bundle.getResources(path);
        if (resources == null) {
            return Collections.emptyList();
        }
        return Collections.list(resources);
    }

    @Override
    public URL getClientResource(String path) {
        Bundle[] bundles = FrameworkUtil.getBundle(OSGiResourceProvider.class)
                .getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            if ("com.vaadin.flow.client".equals(bundle.getSymbolicName())) {
                return bundle.getResource(path);
            }
        }
        return null;
    }

    @Override
    public InputStream getClientResourceAsStream(String path)
            throws IOException {
        // No any caching !: flow-client may be reinstalled at any moment
        return getClientResource(path).openStream();
    }

}
