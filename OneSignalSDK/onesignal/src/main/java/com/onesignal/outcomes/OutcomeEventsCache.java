package com.onesignal.outcomes;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;

import com.onesignal.CachedUniqueOutcomeNotification;
import com.onesignal.OSLogger;
import com.onesignal.OSSessionManager;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDb;
import com.onesignal.outcomes.model.OutcomeEventParams;
import com.onesignal.outcomes.model.OutcomeSource;
import com.onesignal.outcomes.model.OutcomeSourceBody;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

class OutcomeEventsCache {

    private OSLogger logger;
    private OneSignalDb dbHelper;

    OutcomeEventsCache(OSLogger logger, OneSignalDb dbHelper) {
        this.logger = logger;
        this.dbHelper = dbHelper;
    }

    /**
     * Delete event from the DB
     */
    @WorkerThread
    synchronized void deleteOldOutcomeEvent(OutcomeEventParams event) {
        SQLiteDatabase writableDb = dbHelper.getWritableDbWithRetries();

        try {
            writableDb.beginTransaction();
            writableDb.delete(OutcomeEventsTable.TABLE_NAME,
                    OutcomeEventsTable.COLUMN_NAME_TIMESTAMP + " = ?", new String[]{String.valueOf(event.getTimestamp())});
            writableDb.setTransactionSuccessful();
        } catch (Throwable t) {
            logger.log(OneSignal.LOG_LEVEL.ERROR, "Error deleting old outcome event records! ", t);
        } finally {
            if (writableDb != null) {
                try {
                    writableDb.endTransaction(); // May throw if transaction was never opened or DB is full.
                } catch (Throwable t) {
                    logger.log(OneSignal.LOG_LEVEL.ERROR, "Error closing transaction! ", t);
                }
            }
        }
    }

    /**
     * Save an outcome event to send it on the future
     * <p>
     * For offline mode and contingency of errors
     */
    @WorkerThread
    synchronized void saveOutcomeEvent(OutcomeEventParams eventParams) {
        SQLiteDatabase writableDb = dbHelper.getWritableDbWithRetries();
        String notificationIds = "[]";
        OSSessionManager.Session session = OSSessionManager.Session.UNATTRIBUTED;

        if (eventParams.getOutcomeSource() != null) {
            OutcomeSource source = eventParams.getOutcomeSource();
            if (source.getDirectBody() != null) {
                session = OSSessionManager.Session.DIRECT;
                notificationIds = source.getDirectBody().getNotificationIds().toString();
            } else if (source.getIndirectBody() != null) {
                session = OSSessionManager.Session.INDIRECT;
                notificationIds = source.getIndirectBody().getNotificationIds().toString();
            }
        }

        ContentValues values = new ContentValues();
        values.put(OutcomeEventsTable.COLUMN_NAME_NOTIFICATION_IDS, notificationIds);
        values.put(OutcomeEventsTable.COLUMN_NAME_SESSION, session.toString().toLowerCase());
        values.put(OutcomeEventsTable.COLUMN_NAME_NAME, eventParams.getOutcomeId());
        values.put(OutcomeEventsTable.COLUMN_NAME_TIMESTAMP, eventParams.getTimestamp());
        values.put(OutcomeEventsTable.COLUMN_NAME_WEIGHT, eventParams.getWeight());

        writableDb.insert(OutcomeEventsTable.TABLE_NAME, null, values);
        writableDb.close();
    }

    /**
     * Save an outcome event to send it on the future
     * <p>
     * For offline mode and contingency of errors
     */
    @WorkerThread
    synchronized List<OutcomeEventParams> getAllEventsToSend() {
        List<OutcomeEventParams> events = new ArrayList<>();
        Cursor cursor = null;

        try {
            SQLiteDatabase readableDb = dbHelper.getReadableDbWithRetries();
            cursor = readableDb.query(
                    OutcomeEventsTable.TABLE_NAME,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor.moveToFirst()) {
                do {
                    String sessionString = cursor.getString(cursor.getColumnIndex(OutcomeEventsTable.COLUMN_NAME_SESSION));
                    OSSessionManager.Session session = OSSessionManager.Session.fromString(sessionString);
                    String notificationIds = cursor.getString(cursor.getColumnIndex(OutcomeEventsTable.COLUMN_NAME_NOTIFICATION_IDS));
                    String name = cursor.getString(cursor.getColumnIndex(OutcomeEventsTable.COLUMN_NAME_NAME));
                    long timestamp = cursor.getLong(cursor.getColumnIndex(OutcomeEventsTable.COLUMN_NAME_TIMESTAMP));
                    float weight = cursor.getFloat(cursor.getColumnIndex(OutcomeEventsTable.COLUMN_NAME_WEIGHT));

                    try {
                        OutcomeSourceBody sourceBody = new OutcomeSourceBody();
                        sourceBody.setNotificationIds(new JSONArray(notificationIds));
                        OutcomeSource source = null;
                        switch (session) {
                            case DIRECT:
                                source = new OutcomeSource(sourceBody, null);
                                break;
                            case DISABLED:
                                // We should not save disable
                                break;
                            case INDIRECT:
                                source = new OutcomeSource(null, sourceBody);
                                break;
                            case UNATTRIBUTED:
                                // Keep source as null, no source mean unattributed
                                break;
                        }

                        OutcomeEventParams eventParams = new OutcomeEventParams(name, source, weight, timestamp);
                        events.add(eventParams);
                    } catch (JSONException e) {
                        logger.log(OneSignal.LOG_LEVEL.ERROR, "Generating JSONArray from notifications ids outcome:JSON Failed.", e);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return events;
    }

    /**
     * Save a JSONArray of notification ids as separate items with the unique outcome name
     */
    @WorkerThread
    synchronized void saveUniqueOutcomeNotifications(JSONArray notificationIds, String outcomeName) {
        if (notificationIds == null)
            return;

        SQLiteDatabase writableDb = dbHelper.getWritableDbWithRetries();
        try {
            for (int i = 0; i < notificationIds.length(); i++) {
                ContentValues values = new ContentValues();

                String notificationId = notificationIds.getString(i);
                values.put(CachedUniqueOutcomeNotificationTable.COLUMN_NAME_NOTIFICATION_ID, notificationId);
                values.put(CachedUniqueOutcomeNotificationTable.COLUMN_NAME_NAME, outcomeName);

                writableDb.insert(CachedUniqueOutcomeNotificationTable.TABLE_NAME, null, values);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        writableDb.close();
    }

    /**
     * Create a JSONArray of not cached notification ids from the unique outcome notifications SQL table
     */
    @WorkerThread
    synchronized JSONArray getNotCachedUniqueOutcomeNotifications(String name, JSONArray notificationIds) {
        JSONArray uniqueNotificationIds = new JSONArray();

        SQLiteDatabase readableDb = dbHelper.getReadableDbWithRetries();
        Cursor cursor = null;

        try {
            for (int i = 0; i < notificationIds.length(); i++) {
                String notificationId = notificationIds.getString(i);
                CachedUniqueOutcomeNotification notification = new CachedUniqueOutcomeNotification(notificationId, name);

                String[] columns = new String[]{};

                String where = CachedUniqueOutcomeNotificationTable.COLUMN_NAME_NOTIFICATION_ID + " = ? AND " +
                        CachedUniqueOutcomeNotificationTable.COLUMN_NAME_NAME + " = ?";

                String[] args = new String[]{notification.getNotificationId(), notification.getName()};

                cursor = readableDb.query(
                        CachedUniqueOutcomeNotificationTable.TABLE_NAME,
                        columns,
                        where,
                        args,
                        null,
                        null,
                        null,
                        "1"
                );

                // Item is not cached, add it to the JSONArray
                if (cursor.getCount() == 0)
                    uniqueNotificationIds.put(notificationId);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed())
                cursor.close();
        }

        return uniqueNotificationIds;
    }
}
