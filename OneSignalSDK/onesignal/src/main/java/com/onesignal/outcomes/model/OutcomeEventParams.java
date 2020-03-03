package com.onesignal.outcomes.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class OutcomeEventParams {

    private static final String OUTCOME_ID = "id";
    private static final String OUTCOME_SOURCE = "sources";
    private static final String WEIGHT = "weight";
    private static final String TIMESTAMP = "timestamp";

    @NonNull
    private String outcomeId;
    @Nullable
    private OutcomeSource outcomeSource;
    // This field is optional, defaults to zero
    private float weight = 0;
    // This field is optional.
    private long timestamp;

    public OutcomeEventParams(@NonNull String outcomeId, @Nullable OutcomeSource outcomeSource, float weight, long timestamp) {
        this.outcomeId = outcomeId;
        this.outcomeSource = outcomeSource;
        this.weight = weight;
        this.timestamp = timestamp;
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();

        try {
            json.put(OUTCOME_ID, outcomeId);
            if (outcomeSource != null)
                json.put(OUTCOME_SOURCE, outcomeSource.toJSONObject());
            json.put(WEIGHT, weight);
            json.put(TIMESTAMP, timestamp);
        } catch (JSONException exception) {
//            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Generating OutcomeSourceBody toJSONObject ", exception);
        }

        return json;
    }

    public String getOutcomeId() {
        return outcomeId;
    }

    public OutcomeSource getOutcomeSource() {
        return outcomeSource;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
