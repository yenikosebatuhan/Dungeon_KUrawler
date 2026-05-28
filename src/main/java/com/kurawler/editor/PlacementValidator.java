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
            StaticObject existing = cell.getStaticObject();
            StaticObject placing = (StaticObject) obj;
            if (placing.isPassable()) {
                return cell.getOccupyingCharacter() == null
                        && !cell.hasObjects()
                        && (existing == null || existing.isPassable());
            }
            return existing == null
                    && cell.getOccupyingCharacter() == null
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
