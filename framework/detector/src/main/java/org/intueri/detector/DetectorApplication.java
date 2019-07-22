/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@SuppressWarnings("uncommentedmain")
//@EnableConfigurationProperties(ApplicationConfig.class)
public class DetectorApplication {

    /**
     * asdf
     *
     * @param args asdf
     */
    public static void main(String[] args) {
        SpringApplication.run(DetectorApplication.class, args);
    }
}
