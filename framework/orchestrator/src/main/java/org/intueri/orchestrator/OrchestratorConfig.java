/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.orchestrator;

import org.intueri.exception.IntueriException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.UUID;

@Configuration
@PropertySource("classpath:application.yml")
@ConfigurationProperties(prefix = "intueri")
public class OrchestratorConfig {

    private String bootstrapServer = "kafka:9092";

    private String applicationId = UUID.randomUUID().toString();

    public String getBootstrapServer() {
        return bootstrapServer;
    }

    public void setBootstrapServer(String bootstrapServer) {
        this.bootstrapServer = bootstrapServer;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) throws IntueriException {

        try {
            this.applicationId = UUID.fromString(applicationId).toString();
        } catch (IllegalArgumentException e) {
            throw new IntueriException("Invalid applicationId supplied. Application Id is not a valid UUID");
        }
    }
}
