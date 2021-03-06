/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.output;

import org.json.JSONObject;

public interface OutputAdapter {

    void handleMessage(String event, JSONObject config);
}
