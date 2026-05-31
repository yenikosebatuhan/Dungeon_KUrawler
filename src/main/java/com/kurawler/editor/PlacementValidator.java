package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.character.Character;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;

public class PlacementValidator {

    public boolean isValidPlacement(GameObject obj, GridCell cell) {
        if (obj == null || cell == null) {
            return false;
        }

        if (obj instanceof StaticContainer) {
            StaticObject floor = cell.getStaticObject();
            boolean walkableFloor = floor == null
                    || (floor.isPassable() && !floor.isSearchable());
            return walkableFloor
                    && cell.getOccupyingCharacter() == null
                    && !cell.hasObjects();
        }

        if (obj instanceof StaticObject) {
            // Terrain & props replace whatever terrain is already in the cell — the map
            // is paved with floor by default, so a wall/prop must be able to overwrite it.
            // We only refuse to cover an existing item or character.
            return cell.getOccupyingCharacter() == null
                    && !cell.hasObjects();
        }

        if (obj instanceof Item) {
            return !cell.isBlocked();
        }

        return !cell.isBlocked();
    }

    public boolean isValidCharacterPlacement(Character character, GridCell cell) {
        return character != null && cell != null && !cell.isBlocked();
    }

    public boolean canDeleteObject(GridCell cell) {
        return cell != null
                && (cell.getStaticObject() != null
                || cell.getOccupyingCharacter() != null
                || cell.hasObjects());
    }
}
