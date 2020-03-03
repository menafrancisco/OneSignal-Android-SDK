package com.onesignal.outcomes;

import com.onesignal.OneSignalAPIClient;
import com.onesignal.OneSignalApiResponseHandler;
import com.onesignal.OutcomeEventsService;

import org.json.JSONObject;

abstract class OutcomeEventsClient implements OutcomeEventsService {

    final OneSignalAPIClient client;

    OutcomeEventsClient(OneSignalAPIClient client) {
        this.client = client;
    }

    public abstract void sendOutcomeEvent(JSONObject object, OneSignalApiResponseHandler responseHandler);

}
