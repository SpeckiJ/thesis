/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector.filter;

import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.json.JSONObject;

enum Operator {
    EQ, NE, GT, GE, LT, LE;

    boolean evaluate(Number obj, Number value) {
        switch (this) {
            case EQ:
                return obj.equals(value);
            case NE:
                return !obj.equals(value);
            case GT:
                return obj.doubleValue() > value.doubleValue();
            case GE:
                return obj.doubleValue() >= value.doubleValue();
            case LT:
                return obj.doubleValue() < value.doubleValue();
            case LE:
                return obj.doubleValue() <= value.doubleValue();
            default:
                throw new AssertionError("Unknown Operator " + this);
        }
    }
}

public class ComparisonFilter implements AbstractFilter {

    private String fieldName;

    private Number value;

    private Operator operator;

    public ComparisonFilter(String fieldname, JSONObject options) {
        this.fieldName = fieldname;
        this.operator = Operator.valueOf(options.getString("operator"));
        this.value = options.getDouble("value");
    }

    @Override
    public boolean checkRecord(SourceRecord record) {
        Struct struct = (Struct) record.value();
        return operator.evaluate((Number) ((Struct) struct.get("after")).get(fieldName), value);
    }
}
