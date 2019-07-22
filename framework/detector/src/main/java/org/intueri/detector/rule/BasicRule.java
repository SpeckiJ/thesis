/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector.rule;

import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.intueri.detector.IntueriDetectorMessageHandler;
import org.intueri.detector.filter.AbstractFilter;
import org.intueri.detector.filter.FilterFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class BasicRule implements Rule {

    private IntueriDetectorMessageHandler messageHandler;
    private AbstractFilter[] filters;
    private String relation;
    private String output;

    public BasicRule(JSONObject raw, FilterFactory filterFactory, IntueriDetectorMessageHandler messageHandler) {
        JSONArray filters = raw.getJSONArray("filters");
        this.filters = new AbstractFilter[filters.length()];
        for (int i = 0; i < filters.length(); i++) {
            this.filters[i] = filterFactory.createFilter(filters.getJSONObject(i));
        }
        this.relation = raw.getString("relation").toLowerCase();
        this.messageHandler = messageHandler;
        this.output = raw.getJSONArray("outputs").toString();
    }

    public void handleRecord(SourceRecord source) {
        if (checkRuleApplies(source) && checkFilters(source)) {
            messageHandler.publishEvent(source.toString(), output);
            //TODO: implement
            // Actually send out notification
            // output.handleMessage();
        }
    }

    private boolean checkRuleApplies(SourceRecord record) {
        return ((Struct) ((Struct) record.value()).get("source")).get("table").equals(this.relation);
    }

    private boolean checkFilters(SourceRecord record) {
        for (AbstractFilter filter : filters) {
            if (!filter.checkRecord(record)) {
                return false;
            }
        }
        return true;
    }
}
