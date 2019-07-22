/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector;

import io.debezium.config.Configuration;
import io.debezium.embedded.EmbeddedEngine;
import org.apache.kafka.connect.source.SourceRecord;
import org.intueri.detector.rule.Rule;
import org.intueri.detector.rule.RuleFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class IntueriDatabaseConnector {

    private Logger logger = LoggerFactory.getLogger(IntueriDatabaseConnector.class);
    private Rule[] rules;
    private EmbeddedEngine engine;

    @Autowired
    private RuleFactory ruleFactory;

    /**
     * Starts the Intueri detector
     */
    public void startDetector(JSONObject config) {
        Configuration configuration = generateConfig(config);
        createConnector(configuration);
        createExecutor(configuration).execute(engine);
    }

    /**
     * Stops the Intueri detector
     *
     * @return true if detector was stopped. false if it was not running
     */
    public void stopDetector() {
        if (!engine.stop()) {
            logger.warn("Could not stop debezium engine. engine was not running!");
        }
    }

    /**
     * Sets up the detector
     *
     * @param config configuration to be used
     */
    private void createConnector(Configuration config) {
        this.rules = ruleFactory.getRules();
        this.engine = EmbeddedEngine
                .create()
                .using(config)
                .notifying(this::handleEvent)
                .build();
    }

    /**
     * Parses a json configuration into the native Debezium Configuration
     *
     * @param config configuration encoded as json
     * @return configuration
     */
    public Configuration generateConfig(JSONObject config) {
        Configuration.Builder builder = Configuration.create();
        config.keys().forEachRemaining((entry) -> {
                    builder.with(entry, config.get(entry));
                }
        );
        // Exclude PostGIS internal tables
        builder.changeString("table.blacklist", (old) -> old + ",public.spatial_ref_sys");
        return builder.build();
    }

    /**
     * Creates an executor
     *
     * @param config configuration of the detector
     * @return executor
     */
    private ThreadPoolTaskExecutor createExecutor(Configuration config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getInteger("engine.threads.corePoolSize", Runtime.getRuntime().availableProcessors() / 2));
        executor.setMaxPoolSize(config.getInteger("engine.threads.maxPoolSize", Runtime.getRuntime().availableProcessors()));
        executor.setThreadNamePrefix("intueri-detector-");
        executor.initialize();
        return executor;
    }

    /**
     * Forwards each database change to all rules to be checked for an event.
     *
     * @param record database change
     */
    private void handleEvent(SourceRecord record) {
        for (Rule rule : this.rules) {
            rule.handleRecord(record);
            logger.trace("parsed: " + record.toString());
        }
    }
}
