package com.kurawler.persistence;

import java.util.List;
import java.util.Map;

/** Coerces Gson-parsed JSON values (often Double) into domain types. */
final class JsonTypeHelper {

    private JsonTypeHelper() {
    }

    static int asInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    static long asLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    static boolean asBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asObject(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object value) {
        return (List<Object>) value;
    }
}
