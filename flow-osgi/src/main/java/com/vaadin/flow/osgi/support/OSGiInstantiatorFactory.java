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

import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(immediate = true, service = InstantiatorFactory.class)
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
