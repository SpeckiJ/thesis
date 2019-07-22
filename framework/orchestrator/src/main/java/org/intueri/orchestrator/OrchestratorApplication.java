/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.orchestrator;

import org.intueri.orchestrator.validation.SimpleValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@SpringBootApplication
public class OrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }

    @Bean
    public SimpleValidator configValidator(ResourceLoader loader) throws IOException {
        return new SimpleValidator(readFromClasspath(loader, "classpath:static/configSchema.json"));
    }

    @Bean
    public SimpleValidator ruleValidator(ResourceLoader loader) throws IOException {
        return new SimpleValidator(readFromClasspath(loader, "classpath:static/generalRuleSchema.json"));
    }

    @Bean
    public SimpleValidator detectorValidator(ResourceLoader loader) throws IOException {
        return new SimpleValidator(readFromClasspath(loader, "classpath:static/detectorSchema.json"));
    }

    private String readFromClasspath(ResourceLoader loader, String location) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(loader.getResource(location).getInputStream())
        );
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        return stringBuilder.toString();
    }


}
