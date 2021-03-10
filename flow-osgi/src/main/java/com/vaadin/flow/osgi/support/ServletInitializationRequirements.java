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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.startup.AppShellPredicate;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;

/**
 * An implementation service which declares service dependencies required for
 * the Vaadin servlet initialization.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(scope = ServiceScope.BUNDLE, service = ServletInitializationRequirements.class)
public class ServletInitializationRequirements {

    @Reference
    private ApplicationConfigurationFactory configurationFactory;

    @Reference
    private StaticFileHandlerFactory staticFileHandler;

    @Reference
    private AppShellPredicate appShellPredicate;

    @Reference
    private ResourceProvider resourceProvider;

}
