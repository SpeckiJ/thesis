/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.output;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class ConsoleOutput implements OutputAdapter {

    @Override
    public void handleMessage(JSONObject event, JSONObject config) {
        System.out.println(event.toString(2));
    }
}
