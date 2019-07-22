/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.util;

import org.json.JSONObject;

import java.util.UUID;

public class IntueriRuleImpl implements IntueriRule {

    private UUID id;

    private String rule;

    public IntueriRuleImpl(UUID uuid, String rule) {
        this.id = uuid;
        this.rule = rule;
    }

    public IntueriRuleImpl(String raw) {
        JSONObject obj = new JSONObject(raw);
        this.id = UUID.fromString(obj.getString("id"));
        this.rule = obj.getString("rule");
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    @Override
    public String toString() {
        return toJSON();
    }
}
