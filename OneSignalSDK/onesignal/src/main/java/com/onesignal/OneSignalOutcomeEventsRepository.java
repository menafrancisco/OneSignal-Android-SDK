package com.onesignal;

import com.onesignal.outcomes.model.OutcomeEventParams;

import org.json.JSONArray;

import java.util.List;

public interface OneSignalOutcomeEventsRepository {

    List<OutcomeEventParams> getSavedOutcomeEvents();

    void saveOutcomeEvent(OutcomeEventParams event);

    void removeEvent(OutcomeEventParams outcomeEvent);

    void requestMeasureOutcomeEvent(String appId, int deviceType, OutcomeEventParams event, OneSignalApiResponseHandler responseHandler);

    void saveUniqueOutcomeNotifications(JSONArray notificationIds, String name);

    JSONArray getNotCachedUniqueOutcomeNotifications(String name, JSONArray notificationIds);

}
