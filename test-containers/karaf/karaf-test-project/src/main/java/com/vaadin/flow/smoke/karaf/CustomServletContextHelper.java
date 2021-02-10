/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Developer License
 * 4.0 (CVDLv4).
 *
 *
 * For the full License, see <https://vaadin.com/license/cvdl-4.0>.
 */
package com.vaadin.flow.smoke.karaf;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(service = ServletContextHelper.class, immediate = true, property = {
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=customContext",
        HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH
                + "=/custom-context" })
public class CustomServletContextHelper extends ServletContextHelper {

}
