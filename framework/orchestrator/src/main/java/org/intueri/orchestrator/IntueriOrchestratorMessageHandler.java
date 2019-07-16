/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.orchestrator;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.*;
import org.intueri.orchestrator.validation.IntueriValidationAPI;
import org.intueri.orchestrator.validation.SimpleValidator;
import org.intueri.util.IntueriUtil;
import org.intueri.util.MessageType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Controller
public class IntueriOrchestratorMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(IntueriOrchestratorMessageHandler.class);

    private final String ruleString = "rule";
    private final String configString = "config";
    private final String detectorString = "intueri-detector";
    private final String schemaString = "schema";
    private final String statusString = "status";
    private final String availableRulesString = "availableRules";
    private final String enabledRulesString = "enabledRules";

    private final String id = "id";

    private final String rulePrefix = ruleString + "-";
    private final String configPrefix = configString + "-";
    private final String detectorPrefix = detectorString + "-";
    private final String schemaPrefix = schemaString + "-";

    private final StreamsBuilder builder = new StreamsBuilder();

    private KafkaProducer<String, String> kafkaOutput;

    private ReadOnlyKeyValueStore<String, String> store;
    private String storeName;

    private ApplicationConfig config;


    IntueriOrchestratorMessageHandler(ApplicationConfig config) {
        this.config = config;
    }

    @EventListener(ContextRefreshedEvent.class)
    private void init() {
        storeName = "intueri-store";

        final StoreBuilder<KeyValueStore<String, String>> storeBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(storeName),
                Serdes.String(),
                Serdes.String()
        );
        builder.addStateStore(storeBuilder);
        builder.stream(Pattern.compile("intueri-detector-.*"))
                .process(new ManagementProcessorSupplier(), storeName);

        builder.stream(Pattern.compile("intueri-storage"))
                .process(new StorageProcessorSupplier(), storeName);

        Properties properties = IntueriUtil.kafkaProperties(
                config.getApplicationId(),
                config.getBootstrapServer()
        );
        kafkaOutput = new KafkaProducer<>(properties);
        KafkaStreams streams = new KafkaStreams(builder.build(), properties);
        streams.cleanUp();

        streams.setStateListener((newState, oldState) -> {
            if (oldState == KafkaStreams.State.REBALANCING && newState == KafkaStreams.State.RUNNING) {
                store = streams.store(storeName, QueryableStoreTypes.keyValueStore());
            }
        });
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    public void publish(String topic, MessageType key, String message) {
        kafkaOutput.send(new ProducerRecord<>(
                topic,
                key.toString(),
                message)
        );
    }

    public ReadOnlyKeyValueStore<String, String> getStore() {
        return store;
    }

    class ManagementProcessorSupplier<K, V> implements ProcessorSupplier<String, String> {

        ManagementProcessorSupplier() {
        }

        @Override
        public Processor<String, String> get() {
            return new IntueriManagementProcessor();
        }
    }

    class StorageProcessorSupplier<K, V> implements ProcessorSupplier<String, String> {

        StorageProcessorSupplier() {
        }

        @Override
        public Processor<String, String> get() {
            return new IntueriStorageProcessor();
        }
    }

    class IntueriManagementProcessor implements Processor<String, String> {

        private String topic;

        private UUID detectorId;

        private KeyValueStore<String, String> store;

        private ProcessorContext context;

        @Override
        public void init(ProcessorContext context) {
            this.store = (KeyValueStore) context.getStateStore(storeName);
            this.context = context;
        }

        @Override
        public void process(String key, String value) {
            this.topic = context.topic();
            // Seperate general topics from detector-specific ones
            if (topic.length() > 20) {
                this.detectorId = UUID.fromString(topic.substring(8));
            }
            try {
                switch (MessageType.valueOf(key)) {
                    case SCHEMA:
                        logger.trace("Received schema update from detector: {}", detectorId);
                        JSONObject schema = new JSONObject(value);

                        // Check rules that are applicable to this schema
                        IntueriValidationAPI validator = new SimpleValidator(value);
                        KeyValueIterator<String, String> iterator = store.all();
                        Set<String> availableRules = new HashSet<>();
                        String schemaId = UUID.randomUUID().toString();
                        while (iterator.hasNext()) {
                            KeyValue<String, String> next = iterator.next();
                            if (next.key.substring(0, rulePrefix.length()).equals(rulePrefix)
                                    && validator.validate(next.value)) {
                                availableRules.add(next.key.substring(rulePrefix.length()));
                            } else if (next.key.substring(0, schemaPrefix.length()).equals(schemaPrefix)) {
                                // Deduplicate schemas
                                if (next.value.equals(value)) {
                                    schemaId = new JSONObject(next.value).getString(id);
                                }
                            }
                        }
                        iterator.close();
                        schema.put(id, schemaId);
                        final String schemaKey = schemaPrefix + schemaId;
                        logger.trace("Received new schema for storage: {}", schemaKey);
                        store.put(schemaKey, schema.toString());
                        // Update reference to schema in detector
                        String storedDetector = store.get(topic);
                        if (storedDetector != null) {
                            logger.trace("Updating schema & availableRules for detector {} in storage.", detectorId);
                            JSONObject detector = new JSONObject(storedDetector);
                            detector.put(schemaString, schema.getString(id));
                            detector.put(availableRulesString, availableRules);
                            store.put(topic, detector.toString());
                        } else {
                            logger.error("Received schema for unknown detector: {}", detectorId);
                        }
                        break;
                    case STATUS:
                        logger.trace("Received Status update from detector: {} with new status: {}",
                                detectorId, value);
                        String stored = store.get(topic);
                        // Update detector if it already exists
                        if (stored != null) {
                            logger.trace("Updating status for detector {} in local storage.", detectorId);
                            JSONObject detector = new JSONObject(stored);
                            JSONObject newStatus = new JSONObject(value);
                            detector.put(statusString, newStatus.getString(statusString));
                            store.put(topic, detector.toString());
                        } else {
                            // Create new detector if it does not exist
                            JSONObject jsonValue = new JSONObject(value);
                            store.put(topic, value);
                        }
                        break;
                    default:
                        logger.trace("Ignoring Message with type: {} on topic: {}", key, topic);
                }
            } catch (Exception e) {
                logger.error("Error encountered while processing Message. Could not parse key: {}", e.getMessage());
            }
        }

        @Override
        public void close() {
        }
    }

    private class IntueriStorageProcessor implements Processor<String, String> {

        private KeyValueStore<String, String> store;

        @Override
        public void init(ProcessorContext context) {
            this.store = (KeyValueStore) context.getStateStore(storeName);
        }

        @Override
        public void process(String key, String value) {
            try {
                switch (MessageType.valueOf(key)) {
                    case RULES:
                        JSONObject rule = new JSONObject(value);
                        final String ruleKey = rulePrefix + rule.getString(id);
                        logger.trace("Received new rule for storage: {}", ruleKey);
                        store.put(ruleKey, value);

                        // Update detectors with new rule if rule is applicable
                        KeyValueIterator<String, String> it = store.range(detectorString, detectorString + "1");
                        while (it.hasNext()) {
                            KeyValue<String, String> next = it.next();
                            // Iterate over detectors only
                            if (next.key.substring(0, detectorPrefix.length()).equals(detectorPrefix)) {
                                JSONObject detector = new JSONObject(next.value);
                                logger.trace("Checking rule applicability for detector: {}", detector.get(id));
                                String rawSchema = store.get(schemaPrefix + detector.getString(schemaString));
                                IntueriValidationAPI validation = new SimpleValidator(rawSchema);
                                if (validation.validate(value)) {
                                    logger.trace("Adding Rule to availableRules for detector: {}.", detector.get(id));
                                    detector.getJSONArray(availableRulesString)
                                            .put(rule.getString(id));
                                    store.put(next.key, detector.toString());
                                }
                            }
                        }
                        it.close();
                        break;
                    case CONFIG:
                        JSONObject conf = new JSONObject(value);
                        if (conf.optString(id).equals("")) {
                            conf.put(id, UUID.randomUUID().toString());
                        }
                        final String configKey = configPrefix + conf.getString(id);
                        logger.trace("Received new config for storage: {}", configKey);
                        store.put(configKey, conf.toString());
                        break;
                    default:
                        logger.warn("Ignoring message on topic intueri-storage with type: {}", key);
                        break;
                }
            } catch (Exception e) {
                logger.error("Error encountered while processing Message. Could not identify key: {}", e.getMessage());
            }
        }

        @Override
        public void close() {
        }
    }
}
