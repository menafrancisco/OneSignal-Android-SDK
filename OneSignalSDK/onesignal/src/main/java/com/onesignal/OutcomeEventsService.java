package com.onesignal;

import org.json.JSONObject;

public interface OutcomeEventsService {

    void sendOutcomeEvent(JSONObject object, OneSignalApiResponseHandler responseHandler);

}
