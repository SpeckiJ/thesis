/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 * <p>
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
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.Consumed;
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
    private final String lastContactString = "lastContact";
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
    private String globalStorage;

    private OrchestratorConfig config;

    IntueriOrchestratorMessageHandler(OrchestratorConfig config) {
        this.config = config;
    }

    @EventListener(ContextRefreshedEvent.class)
    private void init() {
        globalStorage = "intueri-store";

        final StoreBuilder<KeyValueStore<String, String>> storeBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(globalStorage),
                Serdes.String(),
                Serdes.String()
        );
        builder.addGlobalStore(storeBuilder,
                "intueri-storage",
                Consumed.with(Serdes.String(), Serdes.String()),
                new StorageProcessorSupplier());

        builder.stream(Pattern.compile("intueri-detector-.*"))
                .process(new ManagementProcessorSupplier());

        Properties properties = kafkaProperties(
                config.getApplicationId(),
                config.getBootstrapServer()
        );
        kafkaOutput = new KafkaProducer<>(properties);
        KafkaStreams streams = new KafkaStreams(builder.build(), properties);

        streams.setStateListener((newState, oldState) -> {
            if (oldState == KafkaStreams.State.REBALANCING && newState == KafkaStreams.State.RUNNING) {
                while (true) {
                    try {
                        store = streams.store(globalStorage, QueryableStoreTypes.keyValueStore());
                        return;
                    } catch (InvalidStateStoreException e) {
                        logger.warn("Could not find State Store. Waiting and retrying.");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            logger.warn("Thread.sleep was interrupted: {}", e.getMessage());
                        }
                    }
                }
            }
        });
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    public static Properties kafkaProperties(String id, String server) {
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, id);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        final String serializer = "org.apache.kafka.common.serialization.StringSerializer";
        properties.put("key.serializer", serializer);
        properties.put("value.serializer", serializer);
        return properties;
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
            this.store = (KeyValueStore) context.getStateStore(globalStorage);
            this.context = context;
        }

        @Override
        public void process(String key, String value) {
            this.topic = context.topic();
            this.detectorId = UUID.fromString(topic.substring(detectorPrefix.length()));
            try {
                switch (MessageType.valueOf(key)) {
                    case SCHEMA:
                        logger.trace("Received schema update from detector: {}", detectorId);
                        JSONObject schema = null;
                        String schemaId = null;

                        // Check rules that are applicable to this schema
                        KeyValueIterator<String, String> schemaIterator = store.range(schemaPrefix, "schema1");
                        Set<String> availableRules = new HashSet<>();

                        // Check if schema already exists
                        while (schemaIterator.hasNext()) {
                            KeyValue<String, String> next = schemaIterator.next();
                            if (next.value.equals(value)) {
                                schema = new JSONObject(next.value);
                                schemaId = next.key;
                                schemaIterator.close();
                                break;
                            }
                        }
                        // Check all rules for applicability if schema is new
                        if (schema == null) {
                            KeyValueIterator<String, String> ruleIterator = store.range(rulePrefix, "rule1");
                            IntueriValidationAPI validator = new SimpleValidator(value);
                            while (ruleIterator.hasNext()) {
                                KeyValue<String, String> next = ruleIterator.next();
                                if (validator.validate(next.value)) {
                                    availableRules.add(next.key.substring(rulePrefix.length()));
                                }
                            }
                            schema = new JSONObject(value);
                            schemaId = UUID.randomUUID().toString();
                            schema.put(id, schemaId);
                            final String schemaKey = schemaPrefix + schemaId;
                            logger.trace("Received new schema for storage: {}", schemaKey);
                            store.put(schemaKey, schema.toString());
                            ruleIterator.close();
                        }

                        // Update detector
                        String storedDetector = store.get(topic);
                        if (storedDetector != null) {
                            logger.trace("Updating schema/availableRules for detector {} in storage.", detectorId);
                            JSONObject detector = new JSONObject(storedDetector);
                            detector.put(schemaString, schema.getString(id));
                            detector.put(availableRulesString, availableRules);
                            detector.put(lastContactString, System.currentTimeMillis());
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
                            JSONObject oldStatus = new JSONObject(stored);
                            JSONObject newStatus = new JSONObject(value);
                            newStatus.put(lastContactString, System.currentTimeMillis());
                            newStatus.put(availableRulesString, oldStatus.getJSONArray(availableRulesString));
                            store.put(topic, newStatus.toString());
                        } else {
                            // Create new detector if it does not exist
                            try {
                                // Validate value is json
                                new JSONObject(value);
                                store.put(topic, value);
                            } catch (Exception e) {
                                logger.error("Could not parse value to JSON: {}", e.getMessage());
                            }
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
            this.store = (KeyValueStore) context.getStateStore(globalStorage);
        }

        @Override
        public void process(String key, String value) {
            try {
                switch (MessageType.valueOf(key)) {
                    case RULES:
                        JSONObject rule = new JSONObject(value);
                        final String ruleKey = rulePrefix + rule.getString(id);
                        logger.trace("Received new rule for storage: {}", ruleKey);
                        if (store.get(ruleKey) == null) {
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
                        }
                        break;
                    case CONFIG:
                        JSONObject conf = new JSONObject(value);
                        if (conf.optString(id).equals("")) {
                            conf.put(id, UUID.randomUUID().toString());
                        }
                        final String configKey = configPrefix + conf.getString(id);
                        logger.trace("Received new config for storage: {}", configKey);
                        if (store.get(configKey) == null) {
                            store.put(configKey, conf.toString());
                        }
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
