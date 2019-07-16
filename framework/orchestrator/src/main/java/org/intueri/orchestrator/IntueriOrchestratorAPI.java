/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
//package org.intueri.orchestrator;
//
//import IntueriDetector;
//import IntueriPersistenceException;
//import org.intueri.orchestrator.persistence.IntueriPersistenceAPI;
//import org.intueri.orchestrator.validation.IntueriValidationAPI;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Collection;
//import java.util.HashSet;
//import java.util.Set;
//import java.util.UUID;
//
//@RestController
//public class IntueriOrchestratorAPI {
//
//    private final Logger logger = LoggerFactory.getLogger(IntueriOrchestratorAPI.class);
//
//    @Autowired
//    private IntueriPersistenceAPI persistence;
//
//    @Autowired
//    private IntueriValidationAPI configValidator;
//
//    @GetMapping(
//            path = "/api/detectors",
//            produces = MediaType.APPLICATION_JSON_VALUE)
//    private ResponseEntity<String> getDetectors() {
//        try {
//            Collection<IntueriDetector> allDetectors = persistence.getAllDetectors();
//            JSONArray output = new JSONArray();
//            allDetectors.forEach(intueriDetector -> output.put(intueriDetector.toJSON()));
//            return ResponseEntity.ok(output.toString(4));
//        } catch (IntueriPersistenceException e) {
//            e.printStackTrace();
//            logger.error(e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
//        }
//    }
//
//    @PostMapping(
//            path = "/api/detectors/{id}",
//            produces = MediaType.APPLICATION_JSON_VALUE,
//            consumes = MediaType.APPLICATION_JSON_VALUE,
//            params = "id"
//    )
//    private ResponseEntity<String> configureDetector(@RequestParam("id") String detectorId, @RequestBody String body) {
//        JSONObject json = new JSONObject(body);
//        try {
//            IntueriDetector detector = persistence.getDetector(detectorId);
//            detector.setConfigId(UUID.fromString(json.getString("configId")));
//            Set<UUID> enabled = new HashSet<>();
//            json.getJSONArray("enabledRuleIds").forEach(id -> enabled.add(UUID.fromString((String) id)));
//            detector.setEnabledRuleIds(enabled);
//            persistence.updateDetector(detector);
//            return ResponseEntity.ok().build();
//        } catch (IntueriPersistenceException | JSONException | ClassCastException e) {
//            e.printStackTrace();
//            logger.error(e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
//        }
//    }
//
//    @PostMapping(
//            path = "/api/rules",
//            consumes = MediaType.APPLICATION_JSON_VALUE
//    )
//    private ResponseEntity<UUID> postRule(@RequestBody String raw) {
//        try {
//            return ResponseEntity.ok(persistence.getRule(raw).getId());
//        } catch (IntueriPersistenceException e) {
//            logger.error(e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    @PostMapping(
//            path = "/api/config",
//            consumes = MediaType.APPLICATION_JSON_VALUE
//    )
//    private ResponseEntity<UUID> postConfiguration(@RequestBody String raw) {
//        if (configValidator.validate(raw)) {
//            try {
//                return ResponseEntity.ok(persistence.getConfig(raw).getId());
//            } catch (IntueriPersistenceException e) {
//                logger.error(e.getMessage());
//                e.printStackTrace();
//                return ResponseEntity.badRequest().build();
//            }
//        } else {
//            return ResponseEntity.badRequest().build();
//        }
//    }
//}
//
