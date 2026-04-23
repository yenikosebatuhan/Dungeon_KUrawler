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

        if (obj instanceof StaticObject || obj instanceof StaticContainer) {
            return cell.isWalkable() && !cell.hasObjects();
        }

        if (obj instanceof Item) {
            return cell.isWalkable();
        }

        return cell.isWalkable();
    }

    public boolean isValidCharacterPlacement(Character character, GridCell cell) {
        return character != null && cell != null && cell.isWalkable() && !cell.hasObjects();
    }

    public boolean canDeleteObject(GridCell cell) {
        return cell != null && (!cell.getObjects().isEmpty() || !cell.isWalkable());
    }
}
