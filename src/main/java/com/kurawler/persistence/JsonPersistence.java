package com.kurawler.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared Gson instance for Phase 2 save/load and map persistence. */
public final class JsonPersistence {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type ROOT_OBJECT_TYPE = new TypeToken<LinkedHashMap<String, Object>>() { }.getType();

    private JsonPersistence() {
    }

    public static Gson gson() {
        return GSON;
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static Map<String, Object> parseObject(String json) {
        return GSON.fromJson(json, ROOT_OBJECT_TYPE);
    }
}
