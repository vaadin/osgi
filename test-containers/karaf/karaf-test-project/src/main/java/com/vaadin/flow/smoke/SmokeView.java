/**
 * Copyright (C) 2020 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.smoke;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route("smoke-view")
public class SmokeView extends Div {

    public SmokeView() {
        NativeButton button = new NativeButton("Click me");
        button.setId("action");
        button.addClickListener(event -> handleClick());
        add(button);
    }

    private void handleClick() {
        Div div = new Div();
        div.setText("clicked");
        div.setId("info");
        add(div);
    }
}
