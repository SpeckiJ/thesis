/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.util;

import java.util.UUID;

public interface IntueriRule {

    UUID getId();

    String getRule();

    default String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append('{')
                .append("\"id\":\"")
                .append(getId().toString())
                .append("\",")
                .append("\"rule\":\"")
                .append(getRule())
                .append("\"}");
        return sb.toString();
    }
}
