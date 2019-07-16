/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.output;


import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Controller;

@Controller
@EnableBinding(Sink.class)
public class IntueriOutputMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(IntueriOutputMessageHandler.class);

    @Autowired
    private ConsoleOutput consoleOutput;

    @Autowired
    private H2Output h2Output;

    @StreamListener(Sink.INPUT)
    private void handleMessages(Message<?> msg) {
        JSONArray outputs = ((JSONObject) msg.getPayload()).getJSONArray("outputs");
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
            out.handleMessage(((JSONObject) msg.getPayload()).getJSONObject("event"), outputConfig);
        }
    }
}
