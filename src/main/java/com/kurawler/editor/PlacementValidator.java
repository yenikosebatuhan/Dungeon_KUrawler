package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.character.Character;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.MapPortal;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;

public class PlacementValidator {

    public boolean isValidPlacement(GameObject obj, GridCell cell) {
        if (obj == null || cell == null) {
            return false;
        }

        if (obj instanceof MapPortal) {
            // Portals can replace any terrain, even cells with items.
            return cell.getOccupyingCharacter() == null;
        }

        if (obj instanceof StaticContainer) {
            // Containers need a walkable floor and no other container/item already there.
            StaticObject floor = cell.getStaticObject();
            boolean walkableFloor = floor == null
                    || (floor.isPassable() && !floor.isSearchable());
            return walkableFloor
                    && cell.getOccupyingCharacter() == null
                    && !cell.hasObjects();
        }

        if (obj instanceof StaticObject) {
            // Terrain tiles (wall, floor, decoration) always replace the current static.
            // They must not overwrite a living character, but items/containers stay untouched.
            return cell.getOccupyingCharacter() == null;
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
