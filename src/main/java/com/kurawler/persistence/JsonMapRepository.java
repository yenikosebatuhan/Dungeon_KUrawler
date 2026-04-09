package com.kurawler.persistence;

/**
 * Stub JSON-based implementation of MapRepository.
 * TODO: Implement actual JSON serialization (e.g., with Gson or Jackson).
 */
public class JsonMapRepository implements MapRepository {

    @Override
    public void save(MapData data, String filename) {
        // TODO: Serialize MapData to JSON and write to file
        System.out.println("[Persistence] Saving map to " + filename + " (stub).");
        System.out.println("  Map size: " + data.getWidth() + "x" + data.getHeight());
        System.out.println("  Objects: " + data.getPlacements().size());
    }

    @Override
    public MapData load(String filename) {
        // TODO: Read JSON file and deserialize to MapData
        System.out.println("[Persistence] Loading map from " + filename + " (stub).");
        return new MapData(10, 10);
    }
}
