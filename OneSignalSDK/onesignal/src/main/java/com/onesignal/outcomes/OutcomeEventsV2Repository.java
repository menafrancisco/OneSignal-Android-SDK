package com.onesignal.outcomes;

import com.onesignal.OSLogger;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalApiResponseHandler;
import com.onesignal.OneSignalOutcomeEventsRepository;
import com.onesignal.OutcomeEventsService;
import com.onesignal.outcomes.model.OutcomeEventParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

class OutcomeEventsV2Repository implements OneSignalOutcomeEventsRepository {

    private static final String APP_ID = "app_id";
    private static final String DEVICE_TYPE = "device_type";
    private static final String DIRECT = "direct";

    private final OSLogger logger;
    private final OutcomeEventsCache outcomeEventsCache;
    private final OutcomeEventsService outcomeEventsService;

    OutcomeEventsV2Repository(OSLogger logger, OutcomeEventsCache outcomeEventsCache, OutcomeEventsService outcomeEventsService) {
        this.logger = logger;
        this.outcomeEventsCache = outcomeEventsCache;
        this.outcomeEventsService = outcomeEventsService;
    }

    public List<OutcomeEventParams> getSavedOutcomeEvents() {
        return outcomeEventsCache.getAllEventsToSend();
    }

    public void saveOutcomeEvent(OutcomeEventParams event) {
        outcomeEventsCache.saveOutcomeEvent(event);
    }

    public void removeEvent(OutcomeEventParams outcomeEvent) {
        outcomeEventsCache.deleteOldOutcomeEvent(outcomeEvent);
    }

    @Override
    public void requestMeasureOutcomeEvent(String appId, int deviceType, OutcomeEventParams event, OneSignalApiResponseHandler responseHandler) {
        JSONObject jsonObject = event.toJSONObject();
        try {
            jsonObject.put(APP_ID, appId);
            jsonObject.put(DEVICE_TYPE, deviceType);

            outcomeEventsService.sendOutcomeEvent(jsonObject, responseHandler);
        } catch (JSONException e) {
            logger.log(OneSignal.LOG_LEVEL.ERROR, "Generating indirect outcome:JSON Failed.", e);
        }
    }

    public void saveUniqueOutcomeNotifications(JSONArray notificationIds, String name) {
        outcomeEventsCache.saveUniqueOutcomeNotifications(notificationIds, name);
    }

    public JSONArray getNotCachedUniqueOutcomeNotifications(String name, JSONArray notificationIds) {
        return outcomeEventsCache.getNotCachedUniqueOutcomeNotifications(name, notificationIds);
    }
}
