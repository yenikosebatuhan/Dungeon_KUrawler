package com.kurawler.model.character;

import com.kurawler.model.Inventory;
import com.kurawler.model.object.Weapon;
import java.util.Random;

/**
 * The player-controlled hero.
 * Creator: Hero creates its own Inventory.
 * Stats per spec: HP=17, Mana=80, STR=random(8-15), DEF=2, Energy=100.
 */
public class Hero extends Character {

    private final Inventory inventory;
    private Weapon equippedWeapon;

    public Hero(String name) {
        super(name,
              17,                           // maxHealth (spec)
              0,                            // attackPower (base, weapon adds)
              2,                            // defensePower (spec)
              8 + new Random().nextInt(8),  // strength: random 8-15
              80,                           // maxMana (spec)
              100);                         // maxEnergy
        this.inventory = new Inventory();
    }

    @Override
    public int calculateDamage() {
        int weaponDmg = (equippedWeapon != null) ? equippedWeapon.getDamage() : 0;
        return getStrength() + weaponDmg;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Weapon getEquippedWeapon() {
        return equippedWeapon;
    }

    public void setEquippedWeapon(Weapon weapon) {
        this.equippedWeapon = weapon;
    }
}
