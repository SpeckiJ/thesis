/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.util;

import java.util.Set;
import java.util.UUID;

public class IntueriSchemaImpl implements IntueriSchema {

    private UUID id;

    private String schema;

    private Set<UUID> availableRules;

    public IntueriSchemaImpl(String schema) {
        this.id = UUID.randomUUID();
        this.schema = schema;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getSchema() {
        return schema;
    }

    @Override
    public Set<UUID> getAvailableRules() {
        return availableRules;
    }

    public void setAvailableRules(Set<UUID> availableRules) {
        this.availableRules = availableRules;
    }

    @Override
    public String toString() {
        return toJSON();
    }
}
