/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector.filter;

import org.apache.kafka.connect.source.SourceRecord;

public interface AbstractFilter {

    boolean checkRecord(SourceRecord record);

}
