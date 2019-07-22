/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector.bootstrap;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

public final class IntueriDatabaseTable {

    /**
     * Stores the name of the table
     */
    private String tablename;

    /**
     * Stores all column names which have a datatype that is comparable (aka has defined order)
     */
    private Set<String> comparableFields;

    /**
     * Stores all column names which are foreign Keys to another Table
     */
    private Set<String> foreignKeys;

    public IntueriDatabaseTable(@NotNull String tableName, Set<String> comparableFields, Set<String> foreignKeys) {

        this.comparableFields = comparableFields;
        this.foreignKeys = foreignKeys;
        this.tablename = tableName;
    }

    public String[] getComparableFields() {
        if (comparableFields != null) {
            return comparableFields.stream().sorted().toArray(String[]::new);
        } else {
            return new String[]{};
        }
    }

    public Set<String> getForeignKeys() {
        if (foreignKeys != null) {
            return foreignKeys;
        } else {
            return new HashSet<>();
        }
    }

    public String getTablename() {
        return tablename;
    }

    //(TODO(specki): Expand to all fields once they are implemented
    public boolean hasRules() {
        return !(getComparableFields().length == 0) || !getForeignKeys().isEmpty();
    }
}
