package com.onesignal.outcomes;

import com.onesignal.OneSignalAPIClient;
import com.onesignal.OneSignalApiResponseHandler;

import org.json.JSONObject;

class OutcomeEventsV1Service extends OutcomeEventsClient {

    OutcomeEventsV1Service(OneSignalAPIClient client) {
        super(client);
    }

    /***
     * API endpoint /api/v1/outcomes/outcomes/measure
     */
    public void sendOutcomeEvent(JSONObject object, OneSignalApiResponseHandler responseHandler) {
        client.post("outcomes/measure", object, responseHandler);
    }
}
