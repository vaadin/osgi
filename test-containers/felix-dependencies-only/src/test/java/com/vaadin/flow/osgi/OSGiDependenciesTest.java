/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.osgi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.felix.framework.FrameworkFactory;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.main.AutoProcessor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.slf4j.LoggerFactory;

@Ignore
public class OSGiDependenciesTest {

    private static final org.slf4j.Logger LOGGER = LoggerFactory
            .getLogger(OSGiDependenciesTest.class);

    private static class TestBundleLogger extends Logger {

        private List<String> errors = new ArrayList<>();

        @Override
        protected void doLog(Bundle bundle, ServiceReference sr, int level,
                             String msg, Throwable throwable) {
            if (throwable instanceof BundleException) {
                String[] splitted = throwable.getMessage()
                        .split("osgi\\.wiring\\.package; ");
                errors.add(String.format("\t%s:\n\t\t%s", bundle.toString(),
                        splitted[splitted.length - 1]));
            }
        }

    }

    @Test
    public void dependenciesCheck() {

        String bundleDir = "target/bundle";
        String cacheDir = "target/cache";

        TestBundleLogger logger = new TestBundleLogger();

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(AutoProcessor.AUTO_DEPLOY_DIR_PROPERTY, bundleDir);
        configProps.put(AutoProcessor.AUTO_DEPLOY_ACTION_PROPERTY,
                "install,start");
        configProps.put(Constants.FRAMEWORK_STORAGE, cacheDir);
        configProps.put(FelixConstants.LOG_LOGGER_PROP, logger);

        try {
            FrameworkFactory factory = getFrameworkFactory();
            Framework m_fwk = factory.newFramework(configProps);
            m_fwk.init();
            AutoProcessor.process(configProps, m_fwk.getBundleContext());
            m_fwk.start();
            m_fwk.waitForStop(120);

            if (!logger.errors.isEmpty()) {
                LOGGER.error("Unresolved OSGi dependencies:");
                LOGGER.error(logger.errors.stream().collect(
                        Collectors.joining("\n")));
                Assert.fail("There are unresolved OSGi dependencies. " +
                        "Please check log for details.");
            }

        } catch (Exception ex) {
            Assert.fail("Could not create framework: " + ex);
        }
    }

    private static FrameworkFactory getFrameworkFactory() throws Exception {
        URL url = OSGiDependenciesTest.class.getClassLoader().getResource(
                "META-INF/services/org.osgi.framework.launch.FrameworkFactory");
        if (url != null) {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(url.openStream()));
            try {
                for (String s = br.readLine(); s != null; s = br.readLine()) {
                    s = s.trim();
                    if ((s.length() > 0) && (s.charAt(0) != '#')) {
                        return (FrameworkFactory) Class.forName(s)
                                .getDeclaredConstructor().newInstance();
                    }
                }
            } finally {
                if (br != null) br.close();
            }
        }

        throw new Exception("Could not find framework factory.");
    }

}
