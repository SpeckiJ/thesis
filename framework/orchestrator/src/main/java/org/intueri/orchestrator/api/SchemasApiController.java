/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.orchestrator.api;

import org.apache.kafka.streams.state.KeyValueIterator;
import org.intueri.orchestrator.IntueriOrchestratorMessageHandler;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class SchemasApiController implements SchemasApi {

    private IntueriOrchestratorMessageHandler messageHandler;

    @Autowired
    public SchemasApiController(IntueriOrchestratorMessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public ResponseEntity<String> getSchemas() {
        KeyValueIterator<String, String> range = messageHandler.getStore().range("schema", "schema1");
        JSONArray out = new JSONArray();
        return ApiUtils.geEntitiesFromKVStore(out, range);
    }
}
