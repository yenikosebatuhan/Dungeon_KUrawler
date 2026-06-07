package com.kurawler.persistence;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gson-backed implementation of {@link MapRepository} for lightweight {@link MapData} files.
 * Full game sessions and editor maps use {@link MapSerializer} / {@link MapDeserializer}.
 */
public class JsonMapRepository implements MapRepository {

    private final Gson gson = JsonPersistence.gson();

    @Override
    public void save(MapData data, String filename) {
        try {
            Path path = Paths.get(filename);
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, gson.toJson(data));
            System.out.println("[Persistence] Saved map data to " + filename);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save map data to " + filename, e);
        }
    }

    @Override
    public MapData load(String filename) {
        try {
            String json = Files.readString(Paths.get(filename));
            MapData data = gson.fromJson(json, MapData.class);
            if (data == null) {
                throw new IllegalStateException("Empty map data file: " + filename);
            }
            if (data.getPlacements() == null) {
                data.setPlacements(new java.util.ArrayList<>());
            }
            System.out.println("[Persistence] Loaded map data from " + filename);
            return data;
        } catch (NoSuchFileException e) {
            throw new IllegalStateException("Map data file not found: " + filename, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load map data from " + filename, e);
        }
    }
}
