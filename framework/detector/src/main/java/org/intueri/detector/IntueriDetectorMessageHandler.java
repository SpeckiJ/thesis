/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.intueri.detector.bootstrap.IntueriBootstrapper;
import org.intueri.detector.rule.RuleFactory;
import org.intueri.exception.IntueriException;
import org.intueri.util.CommandType;
import org.intueri.util.DetectorStatus;
import org.intueri.util.IntueriUtil;
import org.intueri.util.MessageType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Properties;

@Controller
@EnableScheduling
public class IntueriDetectorMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(IntueriDetectorMessageHandler.class);

    private final StreamsBuilder builder = new StreamsBuilder();

    //    private Headers idHeader;
    private ApplicationConfig config;
    private DetectorStatus applicationDetectorStatus = DetectorStatus.WAITING_FOR_CONFIG;
    private KafkaProducer<String, String> kafkaOutput;
    private String kafkaTopicName;

    private String configurationStoreKey = "CONFIG";
    private String ruleStoreKey = "RULES";
    private String idString = "id";

    @Autowired
    private RuleFactory ruleFactory;

    @Autowired
    private IntueriDatabaseConnector databaseConnector;

    @Autowired
    private IntueriBootstrapper bootstrapper;

    private JSONObject configuration;
    private JSONArray rules;
    private String configurationUUID;
    private JSONArray enabledRuleIds;

    public IntueriDetectorMessageHandler(ApplicationConfig config) {
        this.config = config;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        kafkaTopicName = "intueri-detector-" + config.getId().toString();

        builder.stream(kafkaTopicName)
                .process(new IntueriProcessorSupplier());

        Properties properties = IntueriUtil.kafkaProperties(config.getId().toString(), config.getBootstrapServer());
        kafkaOutput = new KafkaProducer<>(properties);

        KafkaStreams streams = new KafkaStreams(builder.build(), properties);
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            updateStatus(DetectorStatus.OFFLINE);
            streams.close();
        }));
    }

    /**
     * Internal wrapper for publishing a message
     *
     * @param message message to be published
     * @param type    type of the message
     */
    private void publish(String message, MessageType type) {
        if (type.equals(MessageType.STATUS)) {
            kafkaOutput.send(new ProducerRecord<>(
                    kafkaTopicName,
                    type.toString(),
                    new JSONObject()
                            .put(idString, config.getId())
                            .put("name", config.getName())
                            .put("status", message)
                            .put("config", configurationUUID)
                            .put("enabledRules", enabledRuleIds)
                            .toString()
            ));
        } else {
            kafkaOutput.send(new ProducerRecord<>(
                    kafkaTopicName,
                    type.toString(),
                    message
            ));
        }
    }

    /**
     * Updates the internal Application DetectorStatus.
     * If DetectorStatus has changed, publishes the new DetectorStatus.
     *
     * @param detectorStatus current Application DetectorStatus
     */
    public void updateStatus(DetectorStatus detectorStatus) {
        if (!this.applicationDetectorStatus.equals(detectorStatus)) {
            logger.info("Updating DetectorStatus to: " + detectorStatus.toString());
            this.applicationDetectorStatus = detectorStatus;
            this.publish(detectorStatus.toString(), MessageType.STATUS);
        }
    }

    /**
     * Publishes a event into the messaging framework.
     *
     * @param message Event to be published
     */
    public void publishEvent(String message, String outputAdapters) {
        JSONObject content = new JSONObject();
        content.put("outputs", outputAdapters);
        content.put("event", message);

        kafkaOutput.send(new ProducerRecord<>(
                kafkaTopicName + "-out",
                MessageType.EVENT.toString(),
                content.toString()
        ));
    }

    /**
     * Publishes the current application status. Automatically runs in a fixed time interval
     */
    @Scheduled(fixedRate = 1000 * 60, initialDelay = 1000 * 10)
    public void publishStatus() {
        publish(this.applicationDetectorStatus.toString(), MessageType.STATUS);
    }

    /**
     * Returns the current status of the detector;
     *
     * @return current status of the detector
     */
    public DetectorStatus getApplicationDetectorStatus() {
        return applicationDetectorStatus;
    }

    /**
     * Publishes the given schema.
     *
     * @param schema Schema to be published
     */
    public void updateSchema(JSONObject schema) {
        this.publish(schema.toString(), MessageType.SCHEMA);
    }

    class IntueriProcessorSupplier<K, V> implements ProcessorSupplier<String, String> {

        IntueriProcessorSupplier() {
        }

        @Override
        public Processor<String, String> get() {
            return new DetectorProcessor();
        }
    }

    class DetectorProcessor implements Processor<String, String> {

        DetectorProcessor() {
        }

        @Override
        public void init(ProcessorContext context) {
        }

        @Override
        public void process(String key, String value) {
            try {
                switch (MessageType.valueOf(key)) {
                    case RULES:
                        logger.info("Received new Rules.");
                        if (applicationDetectorStatus == DetectorStatus.NOT_INITIALIZED
                                || applicationDetectorStatus == DetectorStatus.PAUSED) {
                            logger.trace("Updating Rules in local StateStore." +
                                    "Resetting Application to NOT_INITIALIZED");
                            JSONArray jsonRules = new JSONArray(value);
                            rules = jsonRules;
                            enabledRuleIds = new JSONArray();
                            for (int i = 0; i < rules.length(); i++) {
                                enabledRuleIds.put(jsonRules.getJSONObject(i).getString(idString));
                            }
                            updateStatus(DetectorStatus.NOT_INITIALIZED);
                        } else {
                            logger.error("Received Rule Message while in invalid status: {}",
                                    applicationDetectorStatus);
                        }
                        break;
                    case CONFIG:
                        logger.info("Received new Configuration.");
                        if (applicationDetectorStatus == DetectorStatus.WAITING_FOR_CONFIG
                                || applicationDetectorStatus == DetectorStatus.PAUSED
                                || applicationDetectorStatus == DetectorStatus.NOT_INITIALIZED) {
                            logger.trace("Updating config in local StateStore." +
                                    "Setting status to UPDATING_SCHEMA");
                            JSONObject conf = new JSONObject(value);
                            configurationUUID = conf.getString(idString);
                            configuration = conf;
                            try {
                                updateStatus(DetectorStatus.UPDATING_SCHEMA);
                                bootstrapper.bootstrap(conf);
                                updateStatus(DetectorStatus.NOT_INITIALIZED);
                            } catch (IntueriException e) {
                                logger.error("Could not bootstrap: {}", e.getMessage());
                                updateStatus(DetectorStatus.WAITING_FOR_CONFIG);
                            }
                        } else {
                            logger.error("Received config Message while in invalid status: {}",
                                    applicationDetectorStatus);
                        }
                        break;
                    case COMMAND:
                        try {
                            switch (CommandType.valueOf(value)) {
                                case START:
                                    if (applicationDetectorStatus == DetectorStatus.PAUSED) {
                                        try {
                                            if (configuration == null) {
                                                throw new IntueriException("No config found.");
                                            }
                                            databaseConnector.startDetector(configuration);
                                            updateStatus(DetectorStatus.RUNNING);
                                        } catch (Exception e) {
                                            logger.error("Could not start detector: {}", e.getMessage());
                                        }
                                    } else {
                                        logger.warn("Could not start detector. Invalid state to start: {}",
                                                applicationDetectorStatus.toString());
                                    }
                                    break;
                                case STOP:
                                    if (applicationDetectorStatus == DetectorStatus.RUNNING) {
                                        try {
                                            databaseConnector.stopDetector();
                                            updateStatus(DetectorStatus.PAUSED);
                                        } catch (Exception e) {
                                            logger.error("Could not stop detector: {}", e.getMessage());
                                        }
                                    } else {
                                        logger.warn("Cannot stop detector. Invalid state to stop: {}",
                                                applicationDetectorStatus.toString());
                                    }
                                    break;
                                case INIT:
                                    if (applicationDetectorStatus == DetectorStatus.NOT_INITIALIZED) {
                                        try {
                                            if (rules == null) {
                                                throw new IntueriException("No rules found!");
                                            }
                                            ruleFactory.createRules(rules);
                                            updateStatus(DetectorStatus.PAUSED);
                                        } catch (Exception e) {
                                            logger.error("Error while bootstrapping: {}", e.getMessage());
                                        }
                                    } else {
                                        logger.error("Received init command while in invalid status: {}",
                                                applicationDetectorStatus);
                                    }
                                    break;
                                default:
                                    logger.error("Received unknown command: {}", value);
                                    break;
                            }
                        } catch (Exception e) {
                            logger.error("Received invalid command: {}", value);
                        }
                        break;
                    default:
                        // Ignore Message
                        break;
                }
            } catch (IllegalArgumentException e) {
                logger.error("Could not extract Message Type: {}", e.getMessage());
            }
        }

        @Override
        public void close() {

        }
    }
}
