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
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.startup.AppShellPredicate;

/**
 * OSGi capable implementation of {@link AppShellPredicate}.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(scope = ServiceScope.BUNDLE, service = AppShellPredicate.class)
public class OSGiAppShellPredicate implements AppShellPredicate {

    private Bundle bundle;

    @Override
    public boolean isShell(Class<?> clz) {
        if (!AppShellConfigurator.class.isAssignableFrom(clz)) {
            return false;
        }
        Bundle classBundle = FrameworkUtil.getBundle(clz);
        if (classBundle == null) {
            return false;
        }
        return bundle.getBundleId() == classBundle.getBundleId();
    }

    @Activate
    void activate(ComponentContext ctx) {
        bundle = ctx.getUsingBundle();
    }

}
