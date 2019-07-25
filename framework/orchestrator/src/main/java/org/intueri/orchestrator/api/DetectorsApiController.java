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
import org.intueri.util.DetectorStatus;
import org.intueri.util.MessageType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;
import java.util.List;


@Controller
public class DetectorsApiController implements DetectorsApi {

    private static final Logger logger = LoggerFactory.getLogger(DetectorsApiController.class);

    private final String topicPrefix = "intueri-detector-";

    private IntueriOrchestratorMessageHandler messageHandler;

    private IntueriValidationAPI detectorValidator;

    public DetectorsApiController(@Qualifier("detectorValidator") IntueriValidationAPI validator,
                                  IntueriOrchestratorMessageHandler messageHandler) {
        this.detectorValidator = validator;
        this.messageHandler = messageHandler;
    }

    public ResponseEntity<Void> detectorsIdPut(@Valid @RequestBody String body, @PathVariable("id") String id) {
        try {
            if (detectorValidator.validate(body)) {
                String old = messageHandler.getStore().get(topicPrefix + id);
                if (old != null) {
                    JSONObject oldDetector = new JSONObject(old);
                    JSONObject newProperty = new JSONObject(body);
                    final String configString = "config";
                    final String enabledRulesString = "enabledRules";
                    final String commandString = "command";
                    final String availableRulesString = "availableRules";
                    final String statusString = "status";

                    switch (newProperty.keys().next()) {
                        case configString:
                            // Check if config has changed and configUpdate can be applied and trigger it
                            if (oldDetector.getString(statusString).equals(DetectorStatus.WAITING_FOR_CONFIG.toString())
                                    || oldDetector.getString(statusString).equals(DetectorStatus.PAUSED.toString())) {
                                // Check if config exists
                                String rawConfig = messageHandler.getStore().get(configString
                                        + "-"
                                        + newProperty.getString(configString));
                                if (rawConfig != null) {
                                    messageHandler.publish(
                                            topicPrefix + id,
                                            MessageType.CONFIG,
                                            rawConfig
                                    );
                                    return new ResponseEntity<>(HttpStatus.OK);
                                } else {
                                    logger.error("Received config update for detector: {} but could not find config: {}",
                                            topicPrefix + id,
                                            newProperty.getString(configString));
                                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

                                }
                            } else {
                                logger.error("Received config update for detector: {} while detector is in invalid state: {}",
                                        topicPrefix + id,
                                        oldDetector.getString(statusString));
                                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                            }
                        case enabledRulesString:
                            // Check if rules have changed and RuleUpdate can be applied and trigger it
                            if (oldDetector.getString(statusString).equals(DetectorStatus.NOT_INITIALIZED.toString())
                                    || oldDetector.getString(statusString).equals(DetectorStatus.PAUSED.toString())) {
                                // Check if rules have changed and ruleUpdate can be applied
                                JSONArray newEnabledRuleIds = newProperty.getJSONArray(enabledRulesString);
                                JSONArray newEnabledRules = new JSONArray();
                                JSONArray oldAvailableRuleIds = oldDetector.optJSONArray(availableRulesString);
                                if (oldAvailableRuleIds == null) {
                                    logger.warn("No available rules found. Not triggering rule update.");
                                    return new ResponseEntity<>(HttpStatus.OK);
                                }
                                List<Object> availableRules = oldDetector.optJSONArray(availableRulesString).toList();
                                for (int i = 0; i < newEnabledRuleIds.length(); i++) {
                                    String enabledRuleId = newEnabledRuleIds.getString(i);
                                    if (availableRules.contains(enabledRuleId)) {
                                        newEnabledRules.put(new JSONObject(messageHandler.getStore().get("rule-" + enabledRuleId)));
                                    } else {
                                        logger.error("Received rule update for detector: {} but rule {} is " +
                                                "not available for this detector", topicPrefix + id, enabledRuleId);
                                        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                                    }
                                }
                                messageHandler.publish(
                                        topicPrefix + id,
                                        MessageType.RULES,
                                        newEnabledRules.toString()
                                );
                                return new ResponseEntity<>(HttpStatus.OK);
                            } else {
                                logger.error("Received rule update for detector: {} while detector is in invalid state: {}",
                                        topicPrefix + id,
                                        oldDetector.getString(statusString));
                                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                            }
                        case commandString:
                            messageHandler.publish(
                                    topicPrefix + id,
                                    MessageType.COMMAND,
                                    newProperty.getString(commandString)
                            );
                            return new ResponseEntity<>(HttpStatus.OK);
                        default:
                            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                    }
                } else {
                    logger.error("Received config/enabledRule update for unknown detector: {}", topicPrefix + id);
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IntueriValidationException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> getDetectors() {
        JSONArray out = new JSONArray();
        KeyValueIterator<String, String> iterator = messageHandler.getStore().range(topicPrefix, topicPrefix + "{");
        return ApiUtils.geEntitiesFromKVStore(out, iterator);
    }


}
