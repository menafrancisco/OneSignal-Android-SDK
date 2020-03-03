package com.onesignal;

import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.onesignal.outcomes.OutcomeEventsFactory;
import com.onesignal.outcomes.model.OutcomeEventParams;
import com.onesignal.outcomes.model.OutcomeSource;
import com.onesignal.outcomes.model.OutcomeSourceBody;

import org.json.JSONArray;

import java.util.List;
import java.util.Set;

class OutcomeEventsController {

    private static final String OS_SAVE_OUTCOMES = "OS_SAVE_OUTCOMES";
    private static final String OS_SEND_SAVED_OUTCOMES = "OS_SEND_SAVED_OUTCOMES";
    private static final String OS_SAVE_UNIQUE_OUTCOME_NOTIFICATIONS = "OS_SAVE_UNIQUE_OUTCOME_NOTIFICATIONS";

    // Keeps track of unique outcome events sent for UNATTRIBUTED sessions on a per session level
    private Set<String> unattributedUniqueOutcomeEventsSentSet;

    @NonNull
    private final OutcomeEventsFactory outcomeEventsFactory;
    @NonNull
    private final OSSessionManager osSessionManager;

    public OutcomeEventsController(@NonNull OSSessionManager osSessionManager, @NonNull OutcomeEventsFactory outcomeEventsFactory) {
        this.osSessionManager = osSessionManager;
        this.outcomeEventsFactory = outcomeEventsFactory;

        initUniqueOutcomeEventsSentSets();
    }

    /**
     * Init the sets used for tracking attributed and unattributed unique outcome events
     */
    private void initUniqueOutcomeEventsSentSets() {
        // Get all cached UNATTRIBUTED unique outcomes
        unattributedUniqueOutcomeEventsSentSet = OSUtils.newConcurrentSet();
        Set<String> tempUnattributedUniqueOutcomeEventsSentSet = OneSignalPrefs.getStringSet(
                OneSignalPrefs.PREFS_ONESIGNAL,
                OneSignalPrefs.PREFS_OS_UNATTRIBUTED_UNIQUE_OUTCOME_EVENTS_SENT,
                null
        );
        if (tempUnattributedUniqueOutcomeEventsSentSet != null)
            unattributedUniqueOutcomeEventsSentSet.addAll(tempUnattributedUniqueOutcomeEventsSentSet);
    }

    /**
     * Clean unattributed unique outcome events sent so they can be sent after a new session
     */
    void cleanOutcomes() {
        unattributedUniqueOutcomeEventsSentSet = OSUtils.newConcurrentSet();
        saveUnattributedUniqueOutcomeEvents();
    }

    /**
     * Send all the outcomes that from some reason failed
     */
    void sendSavedOutcomes() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setPriority(Process.THREAD_PRIORITY_BACKGROUND);

                List<OutcomeEventParams> outcomeEvents = outcomeEventsFactory.getRepository().getSavedOutcomeEvents();
                for (OutcomeEventParams event : outcomeEvents) {
                    sendSavedOutcomeEvent(event);
                }
            }
        }, OS_SEND_SAVED_OUTCOMES).start();
    }

    private void sendSavedOutcomeEvent(@NonNull final OutcomeEventParams event) {
        int deviceType = new OSUtils().getDeviceType();
        String appId = OneSignal.appId;

        OneSignalApiResponseHandler responseHandler = new OneSignalApiResponseHandler() {
            @Override
            public void onSuccess(String response) {
                outcomeEventsFactory.getRepository().removeEvent(event);
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
            }
        };

        outcomeEventsFactory.getRepository().requestMeasureOutcomeEvent(appId, deviceType, event, responseHandler);
    }

    void sendOutcomeEvent(@NonNull final String name, @Nullable final OneSignal.OutcomeCallback callback) {
        final JSONArray notificationIds = osSessionManager.getSessionResult().notificationIds;
        sendOutcomeEvent(name, notificationIds, 0, callback);
    }

    void sendUniqueOutcomeEvent(@NonNull final String name, @Nullable OneSignal.OutcomeCallback callback) {
        final JSONArray notificationIds = osSessionManager.getSessionResult().notificationIds;

        // Special handling for unique outcomes in the attributed and unattributed scenarios
        if (osSessionManager.getSession().isAttributed()) {
            // Make sure unique notificationIds exist before trying to make measure request
            final JSONArray uniqueNotificationIds = getUniqueNotificationIds(name, notificationIds);
            if (uniqueNotificationIds == null) {
                OneSignal.Log(OneSignal.LOG_LEVEL.DEBUG,
                        "Measure endpoint will not send because unique outcome already sent for: " +
                                "\nSession: " + osSessionManager.getSession().toString() +
                                "\nOutcome name: " + name +
                                "\nnotificationIds: " + notificationIds);

                // Return null within the callback to determine not a failure, but not a success in terms of the request made
                if (callback != null)
                    callback.onSuccess(null);

                return;
            }

            sendOutcomeEvent(name, uniqueNotificationIds, 0, callback);

        } else if (osSessionManager.getSession().isUnattributed()) {
            // Make sure unique outcome has not been sent for current unattributed session
            if (unattributedUniqueOutcomeEventsSentSet.contains(name)) {
                OneSignal.Log(OneSignal.LOG_LEVEL.DEBUG,
                        "Measure endpoint will not send because unique outcome already sent for: " +
                                "\nSession: " + osSessionManager.getSession().toString() +
                                "\nOutcome name: " + name);

                // Return null within the callback to determine not a failure, but not a success in terms of the request made
                if (callback != null)
                    callback.onSuccess(null);

                return;
            }

            unattributedUniqueOutcomeEventsSentSet.add(name);
            sendOutcomeEvent(name, null, 0, callback);
        }
    }

    void sendOutcomeEventWithValue(@NonNull String name, float weight, @Nullable final OneSignal.OutcomeCallback callback) {
        final JSONArray notificationIds = osSessionManager.getSessionResult().notificationIds;
        sendOutcomeEvent(name, notificationIds, weight, callback);
    }

    private void sendOutcomeEvent(@NonNull final String name, @Nullable final JSONArray notificationIds, final float weight, @Nullable final OneSignal.OutcomeCallback callback) {
        OSSessionManager.SessionResult sessionResult = osSessionManager.getSessionResult();

        final OSSessionManager.Session session = sessionResult.session;
        final String appId = OneSignal.appId;
        final long timestampSeconds = System.currentTimeMillis() / 1000;
        final int deviceType = new OSUtils().getDeviceType();

        OutcomeSourceBody sourceBody = new OutcomeSourceBody();
        sourceBody.setNotificationIds(notificationIds);
        OutcomeSource source = null;

        switch (session) {
            case DIRECT:
                source = new OutcomeSource(sourceBody, null);
                break;
            case INDIRECT:
                source = new OutcomeSource(null, sourceBody);
                break;
            case UNATTRIBUTED:
                break;
            case DISABLED:
                OneSignal.Log(OneSignal.LOG_LEVEL.VERBOSE, "Outcomes for current session are disabled");
                return; // finish method
        }

        final OutcomeEventParams eventParams = new OutcomeEventParams(name, source, weight, timestampSeconds);

        OneSignalApiResponseHandler responseHandler = new OneSignalApiResponseHandler() {
            @Override
            public void onSuccess(String response) {
                if (session.isAttributed())
                    saveAttributedUniqueOutcomeNotifications(notificationIds, name);
                else
                    saveUnattributedUniqueOutcomeEvents();

                // The only case where an actual success has occurred and the OutcomeEvent should be sent back
                if (callback != null)
                    callback.onSuccess(eventParams);
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Thread.currentThread().setPriority(Process.THREAD_PRIORITY_BACKGROUND);
                        outcomeEventsFactory.getRepository().saveOutcomeEvent(eventParams);
                    }
                }, OS_SAVE_OUTCOMES).start();

                OneSignal.onesignalLog(OneSignal.LOG_LEVEL.WARN,
                        "Sending outcome with name: " + name + " failed with status code: " + statusCode + " and response: " + response +
                                "\nOutcome event was cached and will be reattempted on app cold start");

                // Return null within the callback to determine not a failure, but not a success in terms of the request made
                if (callback != null)
                    callback.onSuccess(null);
            }
        };

        outcomeEventsFactory.getRepository().requestMeasureOutcomeEvent(appId, deviceType, eventParams, responseHandler);
    }

    /**
     * Save the ATTRIBUTED JSONArray of notification ids with unique outcome names to SQL
     */
    private void saveAttributedUniqueOutcomeNotifications(final JSONArray notificationIds, final String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setPriority(Process.THREAD_PRIORITY_BACKGROUND);
                outcomeEventsFactory.getRepository().saveUniqueOutcomeNotifications(notificationIds, name);
            }
        }, OS_SAVE_UNIQUE_OUTCOME_NOTIFICATIONS).start();
    }

    /**
     * Save the current set of UNATTRIBUTED unique outcome names to SharedPrefs
     */
    private void saveUnattributedUniqueOutcomeEvents() {
        OneSignalPrefs.saveStringSet(
                OneSignalPrefs.PREFS_ONESIGNAL,
                OneSignalPrefs.PREFS_OS_UNATTRIBUTED_UNIQUE_OUTCOME_EVENTS_SENT,
                // Post success, store unattributed unique outcome event names
                unattributedUniqueOutcomeEventsSentSet);
    }

    /**
     * Get the unique notifications that have not been cached/sent before with the current unique outcome name
     */
    private JSONArray getUniqueNotificationIds(String name, JSONArray notificationIds) {
        JSONArray uniqueNotificationIds = outcomeEventsFactory.getRepository().getNotCachedUniqueOutcomeNotifications(name, notificationIds);
        if (uniqueNotificationIds.length() == 0)
            return null;

        return uniqueNotificationIds;
    }

}
