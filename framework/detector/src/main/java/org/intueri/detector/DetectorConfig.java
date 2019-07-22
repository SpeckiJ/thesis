/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.UUID;

@Configuration
@PropertySource("classpath:application.yml")
@ConfigurationProperties(prefix = "intueri")
public class DetectorConfig {

    private UUID id;

    private String name;

    private String bootstrapServer;

    public UUID getId() {
        return (id != null) ? id : UUID.randomUUID();
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return (name != null) ? name : "Error: no name specified!";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBootstrapServer() {
        return (bootstrapServer != null) ? bootstrapServer : "kafka:9092";
    }

    public void setBootstrapServer(String bootstrapServer) {
        this.bootstrapServer = bootstrapServer;
    }
}
