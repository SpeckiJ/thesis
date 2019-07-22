/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.orchestrator.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;

public interface RulesApi {

    @RequestMapping(value = "/rules",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<String> getRules();

    @RequestMapping(value = "/rules/{id}",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.PUT)
    ResponseEntity<String> rulesPut(@Valid @RequestBody String body, @PathVariable("id") String id);

    @RequestMapping(value = "/rules",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.POST)
    ResponseEntity<Void> rulesPost(@Valid @RequestBody String body);

    @RequestMapping(value = "/rules/{id}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    ResponseEntity<Void> rulesDelete(@PathVariable("id") String id);

}
