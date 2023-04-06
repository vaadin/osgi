/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * OSGi capable implementation of instantiator factory.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(immediate = true, service = {
        InstantiatorFactory.class }, scope = ServiceScope.SINGLETON)
public class OSGiInstantiatorFactory implements InstantiatorFactory {

    private static final class OsgiInstantiator extends DefaultInstantiator
            implements Instantiator {

        private final Lookup lookup;

        private OsgiInstantiator(VaadinService service) {
            super(service);

            lookup = service.getContext().getAttribute(Lookup.class);
        }

        @Override
        public Stream<VaadinServiceInitListener> getServiceInitListeners() {
            return lookup.lookupAll(VaadinServiceInitListener.class).stream();
        }

        @Override
        public I18NProvider getI18NProvider() {
            I18NProvider provider = super.getI18NProvider();
            if (provider == null) {
                provider = lookup.lookup(I18NProvider.class);
            }
            return provider;
        }
    }

    @Override
    public Instantiator createInstantitor(VaadinService service) {
        return new OsgiInstantiator(service);
    }

}
