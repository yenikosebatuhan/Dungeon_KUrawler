package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Weapon;
import java.util.Collections;
import java.util.List;

/**
 * Action: Hero equips or unequips a Weapon.
 */
public class EquipAction implements Action {

    private final Weapon weapon;

    public EquipAction(Weapon weapon) {
        this.weapon = weapon;
    }

    @Override
    public List<Effect> resolve(GameSession session) {
        Hero hero = session.getHero();
        
        if (!hero.getInventory().hasItem(weapon)) {
            return Collections.emptyList();
        }

        return Collections.singletonList(new Effect() {
            @Override
            public void apply(GameSession s) {
                Hero h = s.getHero();
                if (h.getEquippedWeapon() == weapon) {
                    h.setEquippedWeapon(null);
                    System.out.println("[Effect] " + weapon.getName() + " unequipped.");
                } else {
                    h.setEquippedWeapon(weapon);
                    System.out.println("[Effect] " + weapon.getName() + " equipped.");
                }
            }

            @Override
            public String describe() {
                return "Equips or unequips " + weapon.getName();
            }
        });
    }
}