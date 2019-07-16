/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.orchestrator.validation;

import org.intueri.exception.IntueriValidationException;

public interface IntueriValidationAPI {

    /**
     * Checks whether given element is valid json and conforms to schema. Does NOT! check schema for validity.
     * @param element to be validated
     * @return true if rule is valid json and conforms to schema
     */
    boolean validate(String element) throws IntueriValidationException;

    /**
     * Checks whether given schema is valid JSON. Only checks Syntax and not Semantic!
     * @param schema to be validated
     * @return true if schema is valid json
     */
    // boolean validateSchema(String schema);
}
