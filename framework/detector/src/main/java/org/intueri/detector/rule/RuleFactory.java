/**
 * Copyright (C) 2019 ${author} <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.detector.rule;

import org.intueri.detector.IntueriDetectorMessageHandler;
import org.intueri.detector.filter.FilterFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Initializes all Output connectors etc.
 */
@Component
public class RuleFactory {

    @Autowired
    private FilterFactory filterFactory;

    @Autowired
    private IntueriDetectorMessageHandler messageHandler;

    private Rule[] rules;

    public void createRules(String raw) {
        Set<Rule> ruleSet = new HashSet<>();
        JSONArray rootNode = new JSONArray(raw);
        for (int i = 0; i < rootNode.length(); i++) {
            ruleSet.add(createRule(new JSONObject(rootNode.getString(i))));
        }
        this.rules = ruleSet.toArray(new Rule[]{});
    }

    public Rule createRule(JSONObject rule) {
        return new BasicRule(rule, filterFactory, messageHandler);
    }

    public Rule[] getRules() {
        return rules;
    }
}
