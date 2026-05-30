package com.kurawler.action;

import com.kurawler.effect.EquipEffect;
import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.object.Weapon;
import java.util.Collections;
import java.util.List;

/**
 * Action: Hero equips or unequips a Weapon (toggle).
 *
 * @requires weapon != null
 * @modifies none (delegates all state changes to EquipEffect)
 * @effects If weapon is in the hero's inventory, returns a singleton list
 *          containing a new EquipEffect; otherwise returns an empty list.
 */
public class EquipAction implements Action {

    private final Weapon weapon;

    public EquipAction(Weapon weapon) {
        this.weapon = weapon;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        if (!session.getHero().getInventory().hasItem(weapon)) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new EquipEffect(weapon));
    }
}