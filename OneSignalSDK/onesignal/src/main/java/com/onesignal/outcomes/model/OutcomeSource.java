package com.onesignal.outcomes.model;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class OutcomeSource {

    private static final String DIRECT = "direct";
    private static final String INDIRECT = "indirect";

    @Nullable
    private OutcomeSourceBody directBody;
    @Nullable
    private OutcomeSourceBody indirectBody;

    public OutcomeSource(@Nullable OutcomeSourceBody directBody, @Nullable OutcomeSourceBody indirectBody) {
        this.directBody = directBody;
        this.indirectBody = indirectBody;
    }

    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();

        try {
            if (directBody != null)
                json.put(DIRECT, directBody.toJSONObject());
            if (indirectBody != null)
                json.put(INDIRECT, indirectBody.toJSONObject());
        } catch (JSONException exception) {
//            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Generating OutcomeSourceBody toJSONObject ", exception);
        }

        return json;
    }

    public OutcomeSourceBody getDirectBody() {
        return directBody;
    }

    public OutcomeSourceBody getIndirectBody() {
        return indirectBody;
    }
}
