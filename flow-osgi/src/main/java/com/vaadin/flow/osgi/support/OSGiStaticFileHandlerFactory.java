/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

import java.net.URL;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import com.vaadin.flow.server.StaticFileHandler;
import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinService;

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
            return null;
        }

    }

    @Override
    public StaticFileHandler createHandler(VaadinService service) {
        return new OSGiStaticFileHandler(service);
    }

}
