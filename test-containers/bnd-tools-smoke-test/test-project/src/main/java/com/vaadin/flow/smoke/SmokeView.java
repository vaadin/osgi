/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
