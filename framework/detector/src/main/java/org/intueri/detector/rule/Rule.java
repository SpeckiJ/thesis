/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector.rule;

import org.apache.kafka.connect.source.SourceRecord;

public interface Rule {

    void handleRecord(SourceRecord source);

}
