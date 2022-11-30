/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi.support;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;

import com.vaadin.flow.component.page.AppShellConfigurator;

public class OSGiAppShellPredicateTest {

    private Bundle currentBundle = Mockito.mock(Bundle.class);

    private ComponentContext componentContext = Mockito
            .mock(ComponentContext.class);

    private MockedStatic<FrameworkUtil> util = Mockito
            .mockStatic(FrameworkUtil.class);

    OSGiAppShellPredicate predicate = new OSGiAppShellPredicate();

    private static class TestAppShellConfig implements AppShellConfigurator {

    }

    @Before
    public void setUp() {
        Mockito.when(componentContext.getUsingBundle())
                .thenReturn(currentBundle);
        Mockito.when(currentBundle.getBundleId()).thenReturn(1l);
        predicate.activate(componentContext);
    }

    @Test
    public void isShell_isShell_classesInSameBundle_returnsTrue() {
        try {
            util.when(() -> FrameworkUtil.getBundle(TestAppShellConfig.class))
                    .thenReturn(currentBundle);

            Assert.assertTrue(predicate.isShell(TestAppShellConfig.class));
        } finally {
            util.close();
        }
    }

    @Test
    public void isShell_isNotShell_classesInSameBundle_returnsFalse() {
        try {
            util.when(() -> FrameworkUtil.getBundle(List.class))
                    .thenReturn(currentBundle);

            Assert.assertFalse(predicate.isShell(List.class));
        } finally {
            util.close();
        }
    }

    @Test
    public void isShell_isShell_classesInDifferentBundles_returnsFalse() {
        try {
            Bundle bundle = Mockito.mock(Bundle.class);
            Mockito.when(bundle.getBundleId()).thenReturn(2l);

            util.when(() -> FrameworkUtil.getBundle(TestAppShellConfig.class))
                    .thenReturn(bundle);

            Assert.assertFalse(predicate.isShell(TestAppShellConfig.class));
        } finally {
            util.close();
        }

    }

}
