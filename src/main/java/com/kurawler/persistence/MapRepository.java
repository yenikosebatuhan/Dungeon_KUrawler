package com.kurawler.persistence;

/**
 * Interface for saving/loading map data.
 * Abstracts the persistence mechanism (JSON, file, database, etc.).
 */
public interface MapRepository {

    void save(MapData data, String filename);

    MapData load(String filename);
}
