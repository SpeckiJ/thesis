/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.orchestrator.api;

import org.apache.kafka.streams.state.KeyValueIterator;
import org.intueri.exception.IntueriValidationException;
import org.intueri.orchestrator.IntueriOrchestratorMessageHandler;
import org.intueri.orchestrator.validation.IntueriValidationAPI;
import org.intueri.util.MessageType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

@Controller
public class ConfigurationsApiController implements ConfigurationsApi {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationsApiController.class);
    private final IntueriOrchestratorMessageHandler messageHandler;
    private IntueriValidationAPI configValidator;

    @Autowired
    public ConfigurationsApiController(@Qualifier("configValidator") IntueriValidationAPI validator,
                                       IntueriOrchestratorMessageHandler messageHandler) {
        this.configValidator = validator;
        this.messageHandler = messageHandler;
    }

    @Override
    public ResponseEntity<String> getConfigurations() {
        JSONArray out = new JSONArray();
        KeyValueIterator<String, String> iterator = messageHandler.getStore().range("config", "config1");
        return ApiUtils.geEntitiesFromKVStore(out, iterator);
    }

    @Override
    public ResponseEntity<String> configurationsIdPut(@Valid @RequestBody String body, @PathVariable("id") String id) {
        try {
            if (configValidator.validate(body)) {
                String existing = messageHandler.getStore().get("config-" + id);
                if (existing != null && !existing.equals(body)) {
                    messageHandler.publish(ApiUtils.storageTopic,
                            MessageType.CONFIG,
                            body);
                    return ResponseEntity.ok(body);
                } else {
                    log.error("Could not update detector. No detector with id {} found.", id);
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                log.error("Could not parse config. Config has invalid format.");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IntueriValidationException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Void> configurationsPost(@Valid @RequestBody String body) {
        try {
            if (configValidator.validate(body)) {
                messageHandler.publish(ApiUtils.storageTopic,
                        MessageType.CONFIG,
                        body);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                log.error("could not add config. config has invalid format.");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IntueriValidationException e) {
            log.error("could not validate config: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Void> configurationsIdDelete(@PathVariable("id") String id) {
        messageHandler.publish(ApiUtils.storageTopic,
                MessageType.CONFIG,
                new JSONObject().put("id", id).toString()
        );
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
