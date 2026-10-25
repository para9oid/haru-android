package com.example.daylight;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Moves the previous app's private settings forward without losing upgrade data. */
final class MitrioPreferences {
    static final String STORE = "mitrio_private";
    private static final String LEGACY_STORE = "haru_private";
    private static final String MIGRATION_FLAG = "legacy_preferences_copied_v1";

    static synchronized SharedPreferences get(Context context) {
        SharedPreferences current = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
        if (current.getBoolean(MIGRATION_FLAG, false)) return current;

        SharedPreferences legacy = context.getSharedPreferences(LEGACY_STORE, Context.MODE_PRIVATE);
        Map<String, ?> values = legacy.getAll();
        SharedPreferences.Editor editor = current.edit();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (current.contains(key) || value == null) continue;
            if (value instanceof String) editor.putString(key, (String) value);
            else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
            else if (value instanceof Integer) editor.putInt(key, (Integer) value);
            else if (value instanceof Long) editor.putLong(key, (Long) value);
            else if (value instanceof Float) editor.putFloat(key, (Float) value);
            else if (value instanceof Set<?>) {
                HashSet<String> strings = new HashSet<>();
                for (Object item : (Set<?>) value) if (item instanceof String) strings.add((String) item);
                editor.putStringSet(key, strings);
            }
        }
        editor.putBoolean(MIGRATION_FLAG, true).commit();
        return current;
    }

    private MitrioPreferences() {}
}
