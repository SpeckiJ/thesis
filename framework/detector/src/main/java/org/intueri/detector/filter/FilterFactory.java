/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector.filter;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class FilterFactory {

    public AbstractFilter createFilter(JSONObject config) {

        switch(config.getString("type")) {
            case "COMPARISON": {
                return new ComparisonFilter(config.getString("column"), config.getJSONObject("options"));
            }
            default: return null;
        }
    }
}
