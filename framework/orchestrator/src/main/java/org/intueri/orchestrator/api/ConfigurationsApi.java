/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
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

public interface ConfigurationsApi {

    @RequestMapping(value = "/configurations",
            produces = {"application/json"},
            method = RequestMethod.GET)
    ResponseEntity<String> getConfigurations();

    @RequestMapping(value = "/configurations/{id}",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.PUT)
    ResponseEntity<Void> configurationsIdPut(@Valid @RequestBody String body, @PathVariable("id") String id);

    @RequestMapping(value = "/configurations",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.POST)
    ResponseEntity<Void> configurationsPost(@Valid @RequestBody String body);

    @RequestMapping(value = "/configurations/{id}",
            produces = {"application/json"},
            method = RequestMethod.DELETE)
    ResponseEntity<Void> configurationsIdDelete(@PathVariable("id") String id);

}
