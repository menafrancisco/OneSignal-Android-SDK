package com.onesignal.outcomes.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OutcomeSourceBody {

    private static final String NOTIFICATION_IDS = "notification_ids";
    private static final String IAM_IDS = "in_app_message_ids";

    private JSONArray notificationIds;
    private JSONArray inAppMessagesIds;

    public OutcomeSourceBody() {
        this(new JSONArray(), new JSONArray());
    }

    public OutcomeSourceBody(JSONArray notificationIds, JSONArray inAppMessagesIds) {
        this.notificationIds = notificationIds;
        this.inAppMessagesIds = inAppMessagesIds;
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();

        try {
            json.put(NOTIFICATION_IDS, notificationIds);
            json.put(IAM_IDS, inAppMessagesIds);
        } catch (JSONException exception) {
//            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Generating OutcomeSourceBody toJSONObject ", exception);
        }

        return json;
    }

    public JSONArray getNotificationIds() {
        return notificationIds;
    }

    public void setNotificationIds(JSONArray notificationIds) {
        this.notificationIds = notificationIds;
    }

    public JSONArray getInAppMessagesIds() {
        return inAppMessagesIds;
    }

    public void setInAppMessagesIds(JSONArray inAppMessagesIds) {
        this.inAppMessagesIds = inAppMessagesIds;
    }
}
