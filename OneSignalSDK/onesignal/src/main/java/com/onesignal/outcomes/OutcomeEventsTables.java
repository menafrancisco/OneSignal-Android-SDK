package com.onesignal.outcomes;

import android.provider.BaseColumns;

class OutcomeEventsTable implements BaseColumns {
    public static final String TABLE_NAME = "outcome";
    static final String COLUMN_NAME_NOTIFICATION_IDS = "notification_ids"; // OneSignal Notification Ids
    static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    static final String COLUMN_NAME_NAME = "name";
    static final String COLUMN_NAME_SESSION = "session";
    static final String COLUMN_NAME_WEIGHT = "weight";
}

class CachedUniqueOutcomeNotificationTable implements BaseColumns {
    public static final String TABLE_NAME = "cached_unique_outcome_notification";
    static final String COLUMN_NAME_NOTIFICATION_ID = "notification_id"; // OneSignal Notification Id
    static final String COLUMN_NAME_NAME = "name";
}