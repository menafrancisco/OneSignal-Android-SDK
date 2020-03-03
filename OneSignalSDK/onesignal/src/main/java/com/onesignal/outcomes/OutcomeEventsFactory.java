package com.onesignal.outcomes;

import com.onesignal.OneSignalAPIClient;
import com.onesignal.OneSignalDb;
import com.onesignal.OSLogger;
import com.onesignal.OneSignalOutcomeEventsRepository;

public class OutcomeEventsFactory {

    private final OSLogger logger;
    private final OutcomeEventsCache outcomeEventsCache;
    private final OneSignalAPIClient apiClient;
    private OneSignalOutcomeEventsRepository repository;

    public OutcomeEventsFactory(OSLogger logger, OneSignalAPIClient apiClient, OneSignalDb dbHelper) {
        this.logger = logger;
        this.apiClient = apiClient;

        outcomeEventsCache = new OutcomeEventsCache(logger, dbHelper);
    }

    public OneSignalOutcomeEventsRepository getRepository() {
        if (repository == null)
            createRepository(false);
        return repository;
    }

    private void createRepository(boolean isVersion2) {
        if (isVersion2) {
            repository = new OutcomeEventsV2Repository(logger, outcomeEventsCache, new OutcomeEventsV2Service(apiClient));
        } else {
            repository = new OutcomeEventsV1Repository(logger, outcomeEventsCache, new OutcomeEventsV1Service(apiClient));
        }
    }
}
