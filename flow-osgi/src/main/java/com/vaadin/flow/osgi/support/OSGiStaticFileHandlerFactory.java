/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import com.vaadin.flow.server.StaticFileHandler;
import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * OSGi {@link StaticFileHandlerFactory} service implementation.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(scope = ServiceScope.BUNDLE, service = StaticFileHandlerFactory.class)
public class OSGiStaticFileHandlerFactory implements StaticFileHandlerFactory {

    private static class OSGiStaticFileHandler extends StaticFileServer {

        private OSGiStaticFileHandler(VaadinService service) {
            super(service);
        }

        @Override
        protected URL getStaticResource(String path) {
            String relativePath = path.replaceFirst("^/", "");
            if (ApplicationConstants.VAADIN_PUSH_JS.equals(relativePath)
                    || ApplicationConstants.VAADIN_PUSH_DEBUG_JS
                            .equals(relativePath)) {
                return getPushResource(relativePath);
            }
            return null;
        }

        private URL getPushResource(String path) {
            Bundle[] bundles = FrameworkUtil
                    .getBundle(OSGiStaticFileHandlerFactory.class)
                    .getBundleContext().getBundles();
            for (Bundle bundle : bundles) {
                if ("com.vaadin.flow.push".equals(bundle.getSymbolicName())) {
                    return bundle.getResource("META-INF/resources/" + path);
                }
            }
            return null;
        }

    }

    @Override
    public StaticFileHandler createHandler(VaadinService service) {
        return new OSGiStaticFileHandler(service);
    }

}
