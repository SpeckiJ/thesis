/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 * <p>
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.output;


import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Controller;

import java.util.Properties;

@Controller
public class IntueriOutputMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(IntueriOutputMessageHandler.class);

    private final ConsoleOutput consoleOutput;

    private final H2Output h2Output;
    private final StreamsBuilder builder = new StreamsBuilder();
    private final String topic = "intueri-detector-(.*)(-out)$";
    private OutputConfig config;

    @Autowired
    public IntueriOutputMessageHandler(OutputConfig config, ConsoleOutput consoleOutput, H2Output h2Output) {
        this.config = config;
        this.consoleOutput = consoleOutput;
        this.h2Output = h2Output;
    }

    public static Properties kafkaProperties(String id, String server) {
        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, id);
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return properties;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        builder.stream(topic)
                .process(new OutputProcessorSupplier());

        Properties properties = kafkaProperties(config.getApplicationId(), config.getBootstrapServer());

        KafkaStreams streams = new KafkaStreams(builder.build(), properties);
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> streams.close()));
    }

    class OutputProcessorSupplier<K, V> implements ProcessorSupplier<String, String> {

        OutputProcessorSupplier() {
        }

        @Override
        public Processor<String, String> get() {
            return new OutputProcessor();
        }
    }

    class OutputProcessor implements Processor<String, String> {

        @Override
        public void init(ProcessorContext context) {}

        @Override
        public void process(String key, String value) {
            JSONObject message = new JSONObject(value);
            JSONArray outputs = message.getJSONArray("outputs");
            OutputAdapter out;
            for (int i = 0; i < outputs.length(); i++) {
                JSONObject outputConfig = outputs.getJSONObject(i);
                switch (outputConfig.getString("type")) {
                    case "consoleOutput":
                        logger.trace("handling console output");
                        out = consoleOutput;
                        break;
                    case "databaseOutput":
                        out = h2Output;
                        logger.trace("handling database output");
                        break;
                    default:
                        logger.error("received unparseable output type:" + outputConfig);
                        return;
                }
                out.handleMessage(message.getJSONObject("event"), outputConfig);
            }
        }

        @Override
        public void close() {
        }
    }
}
