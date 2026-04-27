package com.styledesk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PreferenceStore {

    private static final Map<String, List<String>> store =
        new ConcurrentHashMap<>();

    public static void save(String userId, String preference) {
        store.computeIfAbsent(userId, k -> Collections.synchronizedList(
            new ArrayList<>())).add(preference);
    }

    public static List<String> get(String userId) {
        return store.getOrDefault(userId, List.of());
    }

    public static Map<String, List<String>> getAll() {
        return Collections.unmodifiableMap(store);
    }
}