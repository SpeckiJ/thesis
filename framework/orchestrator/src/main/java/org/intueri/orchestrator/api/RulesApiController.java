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
public class RulesApiController implements RulesApi {

    private static final Logger log = LoggerFactory.getLogger(RulesApiController.class);

    private final String topic = "intueri-storage";

    private IntueriOrchestratorMessageHandler messageHandler;

    private IntueriValidationAPI ruleValidator;

    @Autowired
    public RulesApiController(IntueriOrchestratorMessageHandler messageHandler,
                              @Qualifier("ruleValidator") IntueriValidationAPI validator) {
        this.messageHandler = messageHandler;
        this.ruleValidator = validator;
    }

    @Override
    public ResponseEntity<String> getRules() {
        JSONArray out = new JSONArray();
        KeyValueIterator<String, String> iterator = messageHandler.getStore().range("rule", "rule1");
        return ApiUtils.geEntitiesFromKVStore(out, iterator);
    }

    @Override
    public ResponseEntity<String> rulesPut(@Valid @RequestBody String body, @PathVariable("id") String id) {
        try {
            if (ruleValidator.validate(body)) {
                String existing = messageHandler.getStore().get("rule-" + id);
                if (existing != null && !existing.equals(body)) {
                    messageHandler.publish(
                            topic,
                            MessageType.RULES,
                            body
                    );
                    return ResponseEntity.ok(body);
                } else {
                    log.error("Could not update detector. No detector with id {} found.", id);
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                log.error("Could not parse rule. rule has invalid format.");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IntueriValidationException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Void> rulesPost(@Valid @RequestBody String body) {
        try {
            if (ruleValidator.validate(body)) {
                messageHandler.publish(
                        topic,
                        MessageType.RULES,
                        body
                );
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                log.error("could not add rule. rule has invalid format.");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IntueriValidationException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Void> rulesDelete(@PathVariable("id") String id) {
        messageHandler.publish(
                topic,
                MessageType.RULES,
                new JSONObject().put("id", id).toString()
        );
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
