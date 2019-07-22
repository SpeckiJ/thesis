/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.orchestrator.validation;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleValidator implements IntueriValidationAPI {

    private final Logger logger = LoggerFactory.getLogger(SimpleValidator.class);

    private Schema schema;

    public SimpleValidator(String schema) throws JSONException {
        JSONObject rawSchema = new JSONObject(schema);
        this.schema = SchemaLoader.load(rawSchema);
    }

    @Override
    public boolean validate(String rawElement) {
        try {
            JSONObject element = new JSONObject(rawElement);
            logger.trace("Trying to validate");
            schema.validate(element);
            return true;
        } catch (ValidationException e) {
            logger.error("Error validating against Schema: " + e.getMessage());
            e.getCausingExceptions().stream()
                    .map(ValidationException::getMessage)
                    .forEach(logger::error);
            return false;
        } catch (JSONException e) {
            logger.error("Error parsing to Json: " + e.getMessage());
            return false;
        }
    }
}
