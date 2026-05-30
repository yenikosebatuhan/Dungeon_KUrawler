package com.kurawler.editor;

import com.kurawler.model.*;
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

        if (obj instanceof StaticObject || obj instanceof StaticContainer) {
            return cell.isEmpty();
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
                || !cell.getGameObjects().isEmpty());
    }
}
