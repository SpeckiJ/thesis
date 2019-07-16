/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.orchestrator.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public interface SchemasApi {

    @RequestMapping(value = "/schemas",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<String> getSchemas();

}
