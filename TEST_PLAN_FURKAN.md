# Test Plan: Stat System & Equipment Tests
**Branch:** `Furkan_Test`  
**Team Member:** Furkan  
**Date:** 2026-05-22

## Overview
This test plan covers the Stat System and Equipment mechanics in Dungeon KUrawler, including:
- HP (Health) updates and clamping
- Mana usage and restoration
- Energy usage and restoration  
- STR (Strength) stat changes
- DEF (Defense) changes via equipment (armor and rings)
- Consumable effects on HP/Mana/Energy

## Test Files
| File | Purpose | Test Count |
|------|---------|-----------|
| `StatSystemTest.java` | Character stat system (HP, Mana, Energy, STR, DEF) | 14 tests |
| `EquipmentEffectsTest.java` | Ring and armor equip/unequip effects | 8 tests |
| `HeroADTTest.java` | Hero ADT validation with repOk() and invariants | 7 tests |
| `CombatResolverTest.java` | Combat damage calculation with DEF | 6 tests |
| **Total** | | **35 tests** |

## Test Categories

### 1. Health (HP) System Tests
**File:** `StatSystemTest.java`

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `takeDamage_reducesHealthByGivenAmount` | Hero takes 5 damage | Health decreases by 5 |
| `takeDamage_clampedAtZero_neverNegative` | Hero takes 9999 damage | Health clamped to 0 |
| `takeDamage_zeroDamage_doesNotChangeHealth` | Hero takes 0 damage | Health unchanged |
| `heal_increasesHealthByGivenAmount` | Hero heals 5 HP after damage | Health increases by 5 |
| `heal_clampedAtMaxHealth_neverExceedsMax` | Hero heals 9999 HP | Health clamped to maxHealth |
| `isAlive_returnsFalse_afterLethalDamage` | Hero takes lethal damage | `isAlive()` returns false |
| `isAlive_returnsTrue_whenHealthIsPositive` | Hero with positive health | `isAlive()` returns true |

### 2. Mana System Tests
**File:** `StatSystemTest.java`

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `useMana_returnsFalse_whenManaInsufficient` | Hero uses mana with mana=0 | Returns false, mana unchanged |
| `useMana_returnsTrueAndDecreasesMana_whenSufficient` | Hero uses 20 mana (has 50) | Returns true, mana=30 |
| `restoreMana_increasesManaByClamped` | Hero restores 20 mana (mana=10) | Mana increases to 30 |
| `restoreMana_clampedAtMaxMana` | Hero restores 9999 mana | Mana clamped to maxMana |

### 3. Energy System Tests
**File:** `StatSystemTest.java`

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `useEnergy_returnsFalse_whenEnergyInsufficient` | Hero uses energy with energy=0 | Returns false, energy unchanged |
| `useEnergy_returnsTrueAndDecreasesEnergy_whenSufficient` | Hero uses 20 energy (has 50) | Returns true, energy=30 |
| `restoreEnergy_increasesEnergyByClamped` | Hero restores 20 energy (energy=10) | Energy increases to 30 |
| `restoreEnergy_clampedAtMaxEnergy` | Hero restores 9999 energy | Energy clamped to maxEnergy |

### 4. STR / DEF Stat Changes Tests
**File:** `StatSystemTest.java`

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `setStrength_updatesStrengthCorrectly` | Set STR to 15 | `getStrength()` returns 15 |
| `setDefensePower_updatesDefensePowerCorrectly` | Set DEF to 7 | `getDefensePower()` returns 7 |

### 5. Equipment Effects Tests (Ring & Armor)
**File:** `EquipmentEffectsTest.java`

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `equipWearable_increasesDefensePowerByBonus` | Equip armor (+3 DEF) | DEF increases by 3 |
| `unequipWearable_restoresDefensePowerToOriginal` | Unequip armor | DEF returns to base |
| `equipDifferentWearable_swapsBonusCorrectly` | Equip armor1 (+2), then armor2 (+5) | DEF = base + 5 (swapped, not stacked) |
| `equipSameWearableTwice_doesNotStackBonus` | Equip same armor twice | DEF unchanged on second equip |
| `equipSorcererRing_increasesDefByOne` | Equip sorcerer ring (+1 DEF) | DEF increases by 1 |
| `unequipRing_restoresOriginalDef` | Unequip sorcerer ring | DEF returns to base |
| `equipRingAfterArmor_replacesArmorBonus` | Equip armor (+4), then ring (+2) | DEF = base + 2 (ring bonus, not stacked) |

### 6. Hero ADT Tests
**File:** `HeroADTTest.java`

| Test | Description | Expected Result |
|------|-------------|-----------------|
| `repOk_freshHero_holdsRI` | Fresh hero created | `repOk()` returns true |
| `repOk_afterPartialDamage_holdsRI` | Hero takes 5 damage | `repOk()` returns true |
| `repOk_afterLethalDamage_healthZeroStillValid` | Hero dies (health=0) | `repOk()` returns true (0 is valid) |
| `repOk_afterEquipWearable_holdsRI` | Hero equips armor | `repOk()` returns true |
| `repOk_afterEquipAndUnequipRing_holdsRI` | Hero equips then unequips ring | `repOk()` returns true |
| `repOk_afterUsingConsumable_holdsRI` | Hero uses potion | `repOk()` returns true |
| `calculateDamage_withWeapon_equalsStrengthPlusWeaponDamage` | Hero has sword (damage=4), STR=10 | Damage = 14 |
| `calculateDamage_withoutWeapon_equalsStrengthOnly` | Hero unarmed, STR=10 | Damage = 10 |

### 7. Combat Damage Tests
**File:** `CombatResolverTest.java`

Tests combat damage calculation with DEF reduction.

## Method Specifications

### Hero.equipWearable(Wearable wearable)
```
REQUIRES: wearable != null
MODIFIES: this.defensePower, this.equippedWearable
EFFECTS:  If wearable == equippedWearable, does nothing.
          Otherwise, if equippedWearable != null, subtracts
          equippedWearable.getDefenseBonus() from defensePower first.
          Then sets equippedWearable = wearable and adds
          wearable.getDefenseBonus() to defensePower.
```

### Hero.repOk()
```
Representation Invariant (RI):
- name != null and non-empty
- 0 <= health <= maxHealth
- maxHealth > 0
- 0 <= mana <= maxMana
- 0 <= energy <= maxEnergy
- maxMana >= 0, maxEnergy >= 0
- strength >= 0, attackPower >= 0, defensePower >= 0
- inventory != null
```

## Test Execution

### Prerequisites
- Java 11+
- JUnit 5 (Jupiter)
- Project compiled and dependencies resolved

### Running Tests
```bash
mvn test -Dtest=StatSystemTest
mvn test -Dtest=EquipmentEffectsTest
mvn test -Dtest=HeroADTTest
mvn test -Dtest=CombatResolverTest
# Or run all:
mvn test
```

### Expected Results
All 35 tests should pass ✓

## Git Information

**Branch Name:** `Furkan_Test`  
**Branch Type:** Feature/Test branch  
**Origin:** `origin/Furkan_Test`  
**Based On:** `main`  
**Commit Message:** `test: Add Stat System & Equipment Tests for HP/Mana/Energy and DEF changes`

## Submission Checklist
- [x] Tests written for non-trivial methods (minimum 3 tests per method)
- [x] Method specifications added (requires/modifies/effects clauses)
- [x] ADT tests with repOk() and RI validation
- [x] Tests committed to separate branch
- [x] Branch pushed to GitHub
- [x] Test plan document created
- [ ] Demo git usage in meeting
- [ ] All tests pass
