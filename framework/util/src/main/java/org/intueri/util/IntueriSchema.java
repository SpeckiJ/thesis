/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.util;

import java.util.Set;
import java.util.UUID;

public interface IntueriSchema {

    UUID getId();

    String getSchema();

    Set<UUID> getAvailableRules();

    default String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append('{')
                .append("\"id\":\"")
                .append(getId().toString())
                .append("\",")
                .append("\"schema\":\"")
                .append(getSchema())
                .append("\",")
                .append("\"availableRules\":\"")
                .append(getAvailableRules().toArray(new String[0]).toString())
                .append("\"}");
        return sb.toString();
    }

}
