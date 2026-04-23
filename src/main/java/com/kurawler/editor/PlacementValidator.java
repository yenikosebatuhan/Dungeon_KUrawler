package com.kurawler.editor;

import com.kurawler.model.GridCell;
import com.kurawler.model.object.GameObject;
import com.kurawler.model.object.Item;
import com.kurawler.model.object.StaticContainer;
import com.kurawler.model.object.StaticObject;

/**
 * Encapsulates editor placement rules.
 */
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

        return true;
    }

    public boolean canDeleteObject(GridCell cell) {
        return cell != null && cell.hasObjects();
    }

    public boolean blocksMovement(GameObject obj) {
        return obj instanceof StaticObject || obj instanceof StaticContainer;
    }

    public boolean containsBlockingObject(GridCell cell) {
        if (cell == null) {
            return false;
        }

        for (GameObject obj : cell.getObjects()) {
            if (blocksMovement(obj)) {
                return true;
            }
        }
        return false;
    }
}
