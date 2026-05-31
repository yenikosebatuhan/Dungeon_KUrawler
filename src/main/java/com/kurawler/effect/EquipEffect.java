package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Weapon;

/**
 * Effect: equips or unequips weapons for the hero (toggle).
 * Arrows require a bow; equipping a bow auto-loads arrows from inventory when available.
 */
public class EquipEffect implements Effect {

    private final Weapon weapon;
    private String lastMessage = "";

    public EquipEffect(Weapon weapon) {
        this.weapon = weapon;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    @Override
    public void apply(GameSession session) {
        Hero hero = session.getHero();
        if (hero == null || weapon == null) {
            return;
        }
        lastMessage = hero.toggleEquipWeapon(weapon);
    }

    @Override
    public String describe() {
        return "Equip/unequip " + (weapon != null ? weapon.getName() : "nothing");
    }
}
