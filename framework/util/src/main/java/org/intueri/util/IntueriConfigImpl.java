/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.util;

import org.json.JSONObject;

import java.util.UUID;

public class IntueriConfigImpl implements IntueriConfig {

    private UUID id;

    private String config;

    public IntueriConfigImpl(UUID uuid) {
        this.id = uuid;
    }

    public IntueriConfigImpl(String raw) {
        JSONObject obj = new JSONObject(raw);
        this.id = UUID.fromString(obj.getString("id"));
        this.config = obj.getString("config");
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return toJSON();
    }
}
