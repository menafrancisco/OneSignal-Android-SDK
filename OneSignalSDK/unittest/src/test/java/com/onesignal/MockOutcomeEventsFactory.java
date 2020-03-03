package com.onesignal;

import com.onesignal.outcomes.OutcomeEventsFactory;

public class MockOutcomeEventsFactory extends OutcomeEventsFactory {

    public MockOutcomeEventsFactory(OSLogger logger, OneSignalAPIClient apiClient, OneSignalDb dbHelper) {
        super(logger, apiClient, dbHelper);
    }
}
