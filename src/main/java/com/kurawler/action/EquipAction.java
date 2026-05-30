package com.kurawler.action;

import com.kurawler.effect.Effect;
import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Weapon;
import java.util.Collections;
import java.util.List;

public class EquipAction implements Action {

    private final Weapon weapon;

    public EquipAction(Weapon weapon) { this.weapon = weapon; }

    @Override
    public List<Effect> resolve(GameSession session) {
        Hero hero = session.getHero();
        boolean alreadyEquipped = (hero.getEquippedWeapon() == weapon);
        return Collections.singletonList(new Effect() {
            @Override public void apply(GameSession s) {
                hero.setEquippedWeapon(alreadyEquipped ? null : weapon);
            }
            @Override public String describe() {
                return (alreadyEquipped ? "Unequip " : "Equip ") + weapon.getName();
            }
        });
    }
}
