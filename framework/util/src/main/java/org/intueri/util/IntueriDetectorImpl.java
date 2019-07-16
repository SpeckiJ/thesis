/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.util;

import org.json.JSONObject;

import java.util.Set;
import java.util.UUID;

public class IntueriDetectorImpl implements IntueriDetector {

    private UUID id;

    private String name;

    private UUID schemaId;

    private DetectorStatus detectorStatus;

    private UUID configId;

    private Set<UUID> enabledRules;

    public IntueriDetectorImpl(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    //TODO: expand
    public IntueriDetectorImpl(String raw) {
        JSONObject obj = new JSONObject(raw);
        this.id = UUID.fromString(obj.getString("id"));
        this.name = obj.getString("name");
        String rawSchemaId = obj.getString("schemaId");
        if (rawSchemaId != null) {
            this.schemaId = UUID.fromString(rawSchemaId);
        }
        String rawConfigId = obj.getString("configId");
        if (rawSchemaId != null) {
            this.schemaId = UUID.fromString(rawSchemaId);
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public UUID getSchemaId() {
        return schemaId;
    }

    @Override
    public IntueriDetector setSchemaId(UUID schemaId) {
        this.schemaId = schemaId;
        return this;
    }

    @Override
    public DetectorStatus getDetectorStatus() {
        return detectorStatus;
    }

    @Override
    public IntueriDetector setStatus(DetectorStatus detectorStatus) {
        this.detectorStatus = detectorStatus;
        return this;
    }

    @Override
    public UUID getConfigId() {
        return configId;
    }

    @Override
    public IntueriDetector setConfigId(UUID configId) {
        this.configId = configId;
        return this;
    }

    @Override
    public Set<UUID> getEnabledRuleIds() {
        return enabledRules;
    }

    @Override
    public IntueriDetector setEnabledRuleIds(Set<UUID> ruleIds) {
        this.enabledRules = ruleIds;
        return this;
    }

    @Override
    public String toString() {
        return toJSON();
    }
}
