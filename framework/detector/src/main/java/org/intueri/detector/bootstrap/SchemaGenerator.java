/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector.bootstrap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

@Component
public class SchemaGenerator {

    private static final String baseJSON = "{\"allOf\":[{\"$ref\":\"#/definitions/ruleBase\"},{\"properties\":{\"relation\":{},\"filters\":{\"type\":\"array\",\"items\":{\"oneOf\":[]}}}}]}";
    private static final String comparisonSchemaBase = "{\"allOf\":[{\"$ref\":\"#/definitions/comparisonOperatorBase\"},{\"properties\":{\"column\":{\"type\":\"string\"}},\"required\":[\"column\"]}]}";

    @Autowired
    private ResourceLoader loader;

    private final Logger logger = LoggerFactory.getLogger(SchemaGenerator.class);

    /**
     * Creates a JSON Schema for validating all rules that can be created on given tables
     *
     * @param tables database tables that can be used by any rule
     * @return JSONSchema for validating rules
     * @throws IOException when the Schema Template could not be read
     */
    public JSONObject createSchema(Set<IntueriDatabaseTable> tables) throws IOException {
        JSONObject root = new JSONObject(readTemplateFile());
        JSONArray ruleSchemata = new JSONArray();

        tables.stream()
                .sorted(Comparator.comparing(IntueriDatabaseTable::getTablename))
                .map(this::createRuleEntry)
                .filter(Objects::nonNull)
                .forEach(ruleSchemata::put);
        root.put("oneOf", ruleSchemata);
        return root;
    }

    /**
     * Creates a schema for all rules using this table
     *
     * @param table to be parsed
     */
    private JSONObject createRuleEntry(IntueriDatabaseTable table) {
        if (table.hasRules()) {
            JSONObject obj = new JSONObject(baseJSON);
            JSONObject properties = obj.getJSONArray("allOf")
                    .getJSONObject(1)
                    .getJSONObject("properties");

            // Set tablename
            properties.getJSONObject("relation")
                    .put("const", table.getTablename());

            // Set comparisonFilter
            if (table.getComparableFields().length != 0) {
                logger.info("Adding ComparisonRuleSchema for table: " + table.getTablename());
                JSONObject comparisonRule = new JSONObject(comparisonSchemaBase);
                JSONArray fields = new JSONArray(table.getComparableFields());
                JSONArray ruleBase = properties.getJSONObject("filters")
                        .getJSONObject("items")
                        .getJSONArray("oneOf")
                        .put(comparisonRule);
                ruleBase.getJSONObject(ruleBase.length() - 1)
                        .getJSONArray("allOf")
                        .getJSONObject(1)
                        .getJSONObject("properties")
                        .getJSONObject("column")
                        .put("enum", fields);
            }
            return obj;
        } else {
            logger.trace("Skipping schema generation as it has not applicable columns for table: " + table.getTablename());
            return null;
        }
    }

    /**
     * Reads the Schema Template from the classpath
     * @return JSON Schema Template
     * @throws IOException if Schema could not be loaded
     */
    private String readTemplateFile() throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(loader.getResource("classpath:static/RuleSchemaTemplate.json").getInputStream())
        );
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        return stringBuilder.toString();
    }
}
