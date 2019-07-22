/**
 * Copyright (C) 2019 Jan Speckamp <speckij@posteo.net>
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.intueri.output;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

@Component
public class H2Output implements OutputAdapter {

    @Value("${intueri.outputs.h2output.datastorename:intueri-out.h2}")
    private String datastoreName;

    private MVMap<String, String> datastore;

    public H2Output() {
        MVStore store = new MVStore.Builder()
                .fileName(datastoreName)
                .autoCommitDisabled()
                .open();
        datastore = store.openMap("events");
    }

    @Override
    public void handleMessage(JSONObject event, JSONObject config) {
        datastore.put(event.getString("time"), event.toString());
    }

    @PreDestroy
    public void destroy() {
        datastore.store.close();
    }
}
