package org.intueri.orchestrator.api;

import org.apache.kafka.streams.state.KeyValueIterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.validation.constraints.NotNull;

public class ApiUtils {

    @SuppressWarnings("CheckStyle")
    protected static final String storageTopic = "intueri-storage";

    @NotNull
    static ResponseEntity<String> geEntitiesFromKVStore(JSONArray out, KeyValueIterator<String, String> iterator) {
        while (iterator.hasNext()) {
            out.put(new JSONObject(iterator.next().value));
        }
        iterator.close();
        return new ResponseEntity<>(out.toString(), HttpStatus.OK);
    }
}
