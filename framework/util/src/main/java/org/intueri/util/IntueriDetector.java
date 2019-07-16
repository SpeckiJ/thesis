/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.util;

import java.util.Set;
import java.util.UUID;

public interface IntueriDetector {

    UUID getId();

    String getName();

    UUID getSchemaId();

    IntueriDetector setSchemaId(UUID schemaId);

    DetectorStatus getDetectorStatus();

    IntueriDetector setStatus(DetectorStatus detectorStatus);

    UUID getConfigId();

    IntueriDetector setConfigId(UUID configId);

    Set<UUID> getEnabledRuleIds();

    IntueriDetector setEnabledRuleIds(Set<UUID> ruleIds);

    default String toJSON() {
        StringBuilder sb = new StringBuilder();
        sb.append('{')
                .append("\"id\":\"")
                .append(getId().toString())
                .append("\",")
                .append("\"name\":")
                .append(getName())
                .append("\",")
                .append("\"schemaId\":")
                .append(getSchemaId())
                .append("\",")
                .append("\"status\":")
                .append(getDetectorStatus().toString())
                .append("\",")
                .append("\"configId\":")
                .append(getConfigId())
                .append("\",")
                .append("\"enabledRules\":")
                .append(getEnabledRuleIds().toArray(new String[0]).toString())
                .append("\"}");
        return sb.toString();
    }
}
