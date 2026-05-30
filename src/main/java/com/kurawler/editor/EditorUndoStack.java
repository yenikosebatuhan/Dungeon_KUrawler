package com.kurawler.editor;

import com.kurawler.model.Map;
import com.kurawler.persistence.MapDeserializer;
import com.kurawler.persistence.MapSerializer;
import java.util.ArrayDeque;
import java.util.Deque;

/** Keeps map JSON snapshots for editor undo (PBI-16). */
public class EditorUndoStack {

    private static final int MAX_DEPTH = 40;

    private final MapSerializer serializer = new MapSerializer();
    private final MapDeserializer deserializer = new MapDeserializer();
    private final Deque<String> snapshots = new ArrayDeque<>();

    public void push(Map map) {
        if (map == null) {
            return;
        }
        snapshots.push(serializer.mapToJson(map));
        while (snapshots.size() > MAX_DEPTH) {
            snapshots.removeLast();
        }
    }

    public Map popRestore() {
        if (snapshots.isEmpty()) {
            return null;
        }
        return deserializer.loadMapFromJson(snapshots.pop());
    }

    public boolean canUndo() {
        return !snapshots.isEmpty();
    }

    public void clear() {
        snapshots.clear();
    }
}
