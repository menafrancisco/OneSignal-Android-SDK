package com.onesignal.outcomes;

import com.onesignal.OneSignalAPIClient;
import com.onesignal.OneSignalApiResponseHandler;

import org.json.JSONObject;

class OutcomeEventsV2Service extends OutcomeEventsClient {

    OutcomeEventsV2Service(OneSignalAPIClient client) {
        super(client);
    }

    /***
     * API endpoint /api/v1/outcomes/outcomes/measure
     * TODO define new endpoint for v2
     */
    public void sendOutcomeEvent(JSONObject object, OneSignalApiResponseHandler responseHandler) {
        client.post("outcomes/measure", object, responseHandler);
    }
}
