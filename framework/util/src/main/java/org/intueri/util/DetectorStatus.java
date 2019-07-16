/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.util;

/**
 * Stores current State (bootstrapping, error, undefined)
 */
public enum DetectorStatus {
    /**
     * Application has started. Waiting for Config to connect to datastore
     */
    WAITING_FOR_CONFIG,
    /**
     * Application has connected to datastore. Currently updating internal representation of database schema and
     * creating Rule Schemas.
     */
    UPDATING_SCHEMA,
    /**
     * Application is waiting to bootstrap
     */
    NOT_INITIALIZED,
    /**
     * Application is bootstrapping
     */
    BOOTSTRAPPING,
    /**
     * Application is ready to be started
     */
    PAUSED,
    /**
     * Detector is running
     */
    RUNNING,
    /**
     * Encountered an unrecoverable error.
     */
    ERROR,
    /**
     * Detector is not started
     */
    OFFLINE
}
