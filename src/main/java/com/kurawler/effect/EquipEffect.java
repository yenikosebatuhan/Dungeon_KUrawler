package com.kurawler.effect;

import com.kurawler.model.GameSession;
import com.kurawler.model.character.Hero;
import com.kurawler.model.object.Weapon;

/**
 * Effect: equips or unequips a Weapon for the hero (toggle).
 * If the weapon is already equipped, it is unequipped (restoring base ATK).
 * If a different weapon is equipped, the new one replaces it.
 *
 * @requires session != null && session.getHero() != null && weapon != null
 * @modifies hero.equippedWeapon
 * @effects If weapon == hero.equippedWeapon, sets equippedWeapon to null.
 *          Otherwise sets equippedWeapon to weapon.
 *          Attack damage is recalculated dynamically on next attack
 *          (getStrength() + equippedWeapon.getDamage()).
 */
public class EquipEffect implements Effect {

    private final Weapon weapon;

    /**
     * @requires weapon != null
     * @modifies this.weapon
     * @effects Stores the weapon to equip/unequip for use in apply().
     */
    public EquipEffect(Weapon weapon) {
        this.weapon = weapon;
    }

    /**
     * @requires session != null && session.getHero() != null
     * @modifies hero.equippedWeapon
     * @effects If weapon equals hero.equippedWeapon, unequips it (sets to null).
     *          Otherwise equips weapon. Attack damage is recalculated on the next attack.
     */
    @Override
    public void apply(GameSession session) {
        Hero hero = session.getHero();
        if (hero == null || weapon == null) return;
        if (!hero.getInventory().hasItem(weapon)) return;

        if (hero.getEquippedWeapon() == weapon) {
            hero.setEquippedWeapon(null);
        } else {
            hero.setEquippedWeapon(weapon);
        }
    }

    @Override
    public String describe() {
        return "Equip/unequip " + (weapon != null ? weapon.getName() : "nothing");
    }
}
