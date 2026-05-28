# KUrawler — Product Backlog (1-Week Sprint)

> **Goal:** Every use case works correctly, all UIs are polished, and at least 3 new bonus features are shipped.
> **Branch convention:** `fix/<id>-short-title` or `feat/<id>-short-title` → PR to `main`
> **Priority:** 🔴 Critical → 🟠 High → 🟡 Medium → 🟢 Bonus

---

## How to pick up a task

1. Comment on the GitHub Issue with `@<your-username> taking this`.
2. Create a branch: `git checkout -b fix/PBI-07-shadow-clone-direction`.
3. Implement, verify manually + run relevant tests.
4. Open a PR and link the issue: `Closes #<issue-number>`.

---

## Epic 1 — Use Case Correctness

All use cases from the project specification must behave exactly as described.

---

### PBI-01 · Move Hero (UC-01)
**Priority:** 🔴 Critical  
**Assignee suggestion:** Ikbal  
**Branch:** `fix/PBI-01-hero-movement`

**Description:**  
Verify that all four movement directions (WASD / arrow keys) work correctly on all map types: hand-crafted, procedural, and loaded from JSON.

**Acceptance Criteria:**
- [ ] Hero moves UP, DOWN, LEFT, RIGHT with both WASD and arrow keys.
- [ ] Hero cannot walk through walls or static objects.
- [ ] Hero cannot leave map boundaries (OOB is blocked).
- [ ] Energy decrements by 1 on every successful move.
- [ ] When energy reaches 0, movement is blocked and a warning message appears in the HUD.
- [ ] Smooth lerp animation plays; hero does not teleport.
- [ ] Dust particle effect triggers on each step.

**UI Check:**
- [ ] Hero sprite faces the correct direction during movement.
- [ ] No visual glitches or overlapping sprites at boundaries.

---

### PBI-02 · Attack Enemy (UC-02)
**Priority:** 🔴 Critical  
**Assignee suggestion:** Caner  
**Branch:** `fix/PBI-02-attack-enemy`

**Description:**  
Hero attacks the enemy on the adjacent cell using the space bar or left-click.

**Acceptance Criteria:**
- [ ] `AttackAction → DamageEffect` pipeline executes correctly.
- [ ] Damage follows formula: `Max(1, (STR/2 + ATK) − DEF)`. If hero has equipped weapon, weapon ATK is added.
- [ ] Enemy HP decreases by the correct amount.
- [ ] Enemy is removed from map and cell when HP ≤ 0.
- [ ] Hero cannot attack empty cells or non-enemy objects.
- [ ] Red hit-splatter particle effect plays on enemy hit.
- [ ] Enemy drops ring when it is a Sorcerer (ring appears on the cell).

**UI Check:**
- [ ] HP bar on enemy (if rendered) updates immediately.
- [ ] Enemy removal leaves the cell visually clean (no ghost sprite).

---

### PBI-03 · Take Item (UC-03)
**Priority:** 🔴 Critical  
**Assignee suggestion:** Efekan  
**Branch:** `fix/PBI-03-take-item`

**Description:**  
Hero presses E (or clicks) on an adjacent cell containing a `Item` subclass to pick it up.

**Acceptance Criteria:**
- [ ] `TakeAction → AddToInventoryEffect` executes correctly.
- [ ] Item is removed from map cell and added to the hero's inventory.
- [ ] If inventory is full (8/8), item is NOT picked up and a "Inventory full" message is shown.
- [ ] repOk() passes after every take action.
- [ ] Works for: Weapon, Consumable, Key, Scroll, Ring, Wearable.

**UI Check:**
- [ ] Item disappears from map tile immediately on pickup.
- [ ] Inventory popup shows the new item in the correct slot.
- [ ] HUD bag icon shows updated item count (if count is displayed).

---

### PBI-04 · Eat Consumable (UC-04)
**Priority:** 🔴 Critical  
**Assignee suggestion:** Efekan  
**Branch:** `fix/PBI-04-eat-consumable`

**Description:**  
Hero uses a Consumable from inventory (Health Potion, Mana Elixir, Energy Apple).

**Acceptance Criteria:**
- [ ] `EatAction → HealEffect` restores the correct amount of HP/Mana/Energy.
- [ ] HP, Mana, Energy never exceed their respective maximums.
- [ ] Consumable is removed from inventory after use.
- [ ] If hero is at full HP, eating a health potion still removes it (consumed with no extra effect is acceptable, or show "Already full" message).
- [ ] repOk() passes after eat action.

**UI Check:**
- [ ] Stat bars in HUD update immediately after eating.
- [ ] Consumed item disappears from inventory popup.

---

### PBI-05 · Equip / Unequip Weapon (UC-05)
**Priority:** 🔴 Critical  
**Assignee suggestion:** Furkan  
**Branch:** `fix/PBI-05-equip-weapon`

**Description:**  
Hero equips a Weapon from inventory. Weapon stays in inventory slot but is marked as equipped. Hero's ATK increases.

**Acceptance Criteria:**
- [ ] `EquipAction` sets `hero.equippedWeapon`; weapon slot in inventory NOT cleared.
- [ ] Hero ATK stat visibly increases in HUD stats.
- [ ] Equipping a second weapon replaces the first (old weapon stays in inventory, unequipped).
- [ ] Unequipping restores ATK to base value.
- [ ] Cannot equip an item that is not in inventory.

**UI Check:**
- [ ] Equipped weapon is highlighted (gold border or "E" badge) in inventory popup.
- [ ] ATK value in the stats panel updates immediately.

---

### PBI-06 · Wear / Unequip Armor & Ring (UC-07)
**Priority:** 🔴 Critical  
**Assignee suggestion:** Furkan  
**Branch:** `fix/PBI-06-wear-armor-ring`

**Description:**  
Hero wears a Wearable (armor or ring) from inventory. DEF stat increases accordingly.

**Acceptance Criteria:**
- [ ] `WearAction → WearEffect` sets `hero.equippedWearable`; DEF += `wearable.getDefenseBonus()`.
- [ ] Wearing a second wearable replaces the first; DEF is recalculated, not stacked.
- [ ] Unequip restores DEF to base value.
- [ ] Ring picked up from dead Sorcerer can be worn.
- [ ] repOk() passes after wear/unequip.

**UI Check:**
- [ ] DEF value in HUD stats updates immediately.
- [ ] Worn item shows highlighted badge in inventory popup.

---

### PBI-07 · Shadow Clone (UC-20) — BUG-01 & BUG-02
**Priority:** 🔴 Critical  
**Assignee suggestion:** Batuhan  
**Branch:** `fix/PBI-07-shadow-clone-bugs`

**Description:**  
Fix the two documented bugs in `ShadowClone`. Tests `cloneMovesOppositeToHero*` and `cloneTakesNoDamage*` must pass after this fix.

**BUG-01 — Wrong movement direction:**
- [ ] `onHeroMoved(newRow, newCol, deltaRow, deltaCol)` must move clone to `(row - deltaRow, col - deltaCol)`.
- [ ] Currently moves clone in the SAME direction as hero (`+delta` instead of `-delta`).
- [ ] After fix: `cloneMovesOppositeToHero{Right,Down,Left,Up}` all PASS.

**BUG-02 — Clone should be invulnerable:**
- [ ] Clone must NOT take any damage when hero takes damage.
- [ ] Currently: clone receives `damage / 2`.
- [ ] After fix: `cloneTakesNoDamage{WhenHeroIsHit,OnLargeHit}` both PASS.

**Additional clone behaviour checks:**
- [ ] Clone is blocked by walls and OOB correctly.
- [ ] Clone expires after lifetime; expired clone is removed from map.
- [ ] Clone is ethereal: hero can walk through it.

**UI Check:**
- [ ] Clone sprite is visually distinct from hero (semi-transparent or different tint).
- [ ] Clone removal does not leave ghost sprite on tile.

---

### PBI-08 · Read Scroll (UC-10)
**Priority:** 🟠 High  
**Assignee suggestion:** Batuhan  
**Branch:** `fix/PBI-08-read-scroll`

**Description:**  
Hero reads a Scroll from inventory. `ReadAction → ReadScrollEffect` spawns the Shadow Clone.

**Acceptance Criteria:**
- [ ] Reading a `ShadowCloneScroll` spawns one ShadowClone adjacent to hero.
- [ ] Scroll is removed from inventory after reading.
- [ ] Clone is registered as a `HeroObserver`.
- [ ] Cannot read a non-scroll item via this action.

**UI Check:**
- [ ] Clone sprite appears on map immediately.
- [ ] Scroll disappears from inventory popup.

---

### PBI-09 · Search Object (UC-08)
**Priority:** 🟠 High  
**Assignee suggestion:** Ikbal  
**Branch:** `fix/PBI-09-search-object`

**Description:**  
Hero searches a searchable `StaticObject` (e.g., bookshelf). Items hidden inside are revealed.

**Acceptance Criteria:**
- [ ] `SearchAction → SearchObjectEffect` marks object as searched.
- [ ] Hidden items become visible/accessible on the cell after search.
- [ ] Object cannot be searched a second time.
- [ ] Visual indicator (dashed border) disappears after search.

**UI Check:**
- [ ] Dashed search-marker border is visible before search, gone after.
- [ ] Revealed item appears on tile correctly.

---

### PBI-10 · Break Object (UC-11)
**Priority:** 🟠 High  
**Assignee suggestion:** Caner  
**Branch:** `fix/PBI-10-break-object`

**Description:**  
Hero attacks a breakable `StaticObject` (barrel, crate). Object is destroyed; items may drop.

**Acceptance Criteria:**
- [ ] `BreakAction → BreakObjectEffect` removes object from map.
- [ ] Dropped items land on the same cell.
- [ ] Non-breakable objects (walls, doors) are immune.

**UI Check:**
- [ ] Object sprite is removed from tile immediately.
- [ ] Dropped items render correctly on the tile.

---

### PBI-11 · Open Container & Take From Container (UC-12 / UC-13)
**Priority:** 🟠 High  
**Assignee suggestion:** Efekan  
**Branch:** `fix/PBI-11-container-interaction`

**Description:**  
Hero opens a `StaticContainer` (chest) or `PortableContainer` (bag). A popup shows items inside. Hero can take items one by one.

**Acceptance Criteria:**
- [ ] Clicking/pressing E on a container opens a bag-style popup (game pauses).
- [ ] `TakeFromContainerAction` moves item from container to hero inventory.
- [ ] If hero inventory is full, item cannot be taken (show message).
- [ ] Container popup closes when all items taken or player dismisses.
- [ ] Container shows as empty / open sprite after all items taken.

**UI Check:**
- [ ] Popup matches the bag/chest style (dark themed, gold borders).
- [ ] Items inside container show correct sprites.
- [ ] "Inventory Full" message is visible inside the popup.

---

### PBI-12 · Discard Item (UC-09)
**Priority:** 🟠 High  
**Assignee suggestion:** Efekan  
**Branch:** `fix/PBI-12-discard-item`

**Description:**  
Hero discards an item from inventory. Item is removed from inventory and dropped on hero's current cell.

**Acceptance Criteria:**
- [ ] `DiscardAction → RemoveFromCellEffect` removes item from inventory.
- [ ] Discarded item appears on hero's current cell on the map.
- [ ] repOk() passes after discard.

**UI Check:**
- [ ] Item disappears from inventory popup immediately.
- [ ] Item sprite appears on the map tile.

---

### PBI-13 · Save & Load Game (UC-18 / UC-19)
**Priority:** 🔴 Critical  
**Assignee suggestion:** Ata  
**Branch:** `fix/PBI-13-save-load`

**Description:**  
Hero saves current game state and can load it from the main menu.

**Acceptance Criteria:**
- [ ] Save dialog appears on `Save` button in main menu or in-game menu.
- [ ] `MapSerializer.saveGameState()` writes JSON containing: hero stats, inventory, map objects, enemies, timer, game mode, achievements, targetObjective.
- [ ] Load dialog lists saved files; selecting one restores full game state.
- [ ] After load: hero position, HP, inventory, floor timer, and map objects are identical to save point.
- [ ] Corrupt / missing save file is handled gracefully (error message, no crash).
- [ ] Multiple save slots work independently.

**UI Check:**
- [ ] Save dialog has a text field for save name and a confirm button.
- [ ] Load dialog lists saves with timestamps.
- [ ] Loading screen or transition is shown while deserializing.

---

### PBI-14 · Team Match Mode (UC-22)
**Priority:** 🔴 Critical  
**Assignee suggestion:** Batuhan  
**Branch:** `fix/PBI-14-team-match`

**Description:**  
Full verification of Team Match game mode end-to-end.

**Acceptance Criteria:**
- [ ] `TeamMatchSession.initializeMatch()` creates Team A (RED: 1 Sorcerer + 3 Knights) and Team B (BLUE: Hero + 1 Sorcerer + 2 Knights).
- [ ] Map is validated (minimum size) before match starts.
- [ ] Weapons are placed on the map; Knights can pick them up when stepping on the same cell.
- [ ] `TeamMatchGameLoop` ticks enemy AI for both teams.
- [ ] Win condition: all members of one team eliminated → correct result screen shown.
- [ ] Hero death → "Game Over" regardless of team status.
- [ ] Game loop stops cleanly after win condition.

**UI Check:**
- [ ] Team A members have RED tint / label; Team B members have BLUE tint / label.
- [ ] Win result screen shows the correct team name prominently.
- [ ] HUD correctly shows hero stats during team match.

---

### PBI-15 · Achievements (UC-21)
**Priority:** 🟠 High  
**Assignee suggestion:** Batuhan  
**Branch:** `fix/PBI-15-achievements`

**Description:**  
Achievements are unlocked during gameplay and displayed in the achievements popup.

**Acceptance Criteria:**
- [ ] `AchievementManager` triggers correctly for each achievement condition (first kill, first item, 100 steps, etc.).
- [ ] Toast notification appears briefly on-screen when achievement unlocked.
- [ ] Achievements popup (opened from HUD) shows all achievements with locked/unlocked state.
- [ ] Achievements persist across save/load.

**UI Check:**
- [ ] Toast has distinct dungeon-themed styling (gold border, dark background).
- [ ] Popup lists achievements with icons or checkmarks.

---

### PBI-16 · Level Editor (UC-16)
**Priority:** 🟡 Medium  
**Assignee suggestion:** Ata  
**Branch:** `fix/PBI-16-level-editor`

**Description:**  
Level editor allows placing tiles, objects, items, and enemies on a grid, then saving the map.

**Acceptance Criteria:**
- [ ] All tile types are accessible from the object palette.
- [ ] Placement validation runs correctly (no illegal placements).
- [ ] Map can be saved via `MapSerializer.saveMapToFile()`.
- [ ] Saved map can be loaded and played via "New Game (custom map)".
- [ ] Editor has undo/clear functionality.

**UI Check:**
- [ ] Palette panel shows correct sprites for each object type.
- [ ] Selected tile is highlighted in the palette.
- [ ] Grid lines are visible and aligned with sprites.

---

### PBI-17 · HUD Clickable Zones & Stat Display
**Priority:** 🟠 High  
**Assignee suggestion:** Batuhan  
**Branch:** `fix/PBI-17-hud-polish`

**Description:**  
HUD must correctly display all hero stats and all clickable zones must respond properly.

**Acceptance Criteria:**
- [ ] HP, Mana, Energy bars render with correct fill percentages.
- [ ] STR, DEF, ATK values update when equipping/unequipping items.
- [ ] Clicking bag area opens inventory popup (game pauses).
- [ ] Clicking achievements area opens achievements popup (game pauses).
- [ ] Clicking pause button toggles pause correctly.
- [ ] Clicking menu button opens in-game menu.
- [ ] HUD click zones do not overlap or trigger wrong actions.

**UI Check:**
- [ ] Bars are visually distinct (red=HP, blue=Mana, yellow=Energy).
- [ ] Gold panel borders are clean and not cut off on any window size.
- [ ] Bag icon renders correctly (no missing asset fallback).

---

### PBI-18 · Main Menu & Screen Transitions
**Priority:** 🟡 Medium  
**Assignee suggestion:** Ikbal  
**Branch:** `fix/PBI-18-main-menu`

**Description:**  
Main menu buttons all work; screen transitions are smooth.

**Acceptance Criteria:**
- [ ] "New Game" starts a fresh session on MapFactory map.
- [ ] "Random Dungeon" starts a ProceduralMapGenerator session.
- [ ] "Team Match" starts TeamMatchSession correctly.
- [ ] "Level Editor" opens EditorView.
- [ ] "Save" opens save dialog (disabled when no active session).
- [ ] "Load" opens load dialog and resumes saved session.
- [ ] "Help" opens HelpScreen.
- [ ] "Exit" closes the application cleanly.

**UI Check:**
- [ ] Stone-tablet button hover and press effects work.
- [ ] Dungeon background tiles render without gaps.
- [ ] No layout overflow on standard (1280×800) window size.

---

### PBI-19 · Help Screen
**Priority:** 🟡 Medium  
**Assignee suggestion:** Ikbal  
**Branch:** `fix/PBI-19-help-screen`

**Description:**  
Help screen must display all key bindings and game rules accurately.

**Acceptance Criteria:**
- [ ] All current key bindings are listed (WASD/arrows, E=interact, Space=attack, I=inventory, P=pause, Esc=menu).
- [ ] Section for each use case category: Movement, Combat, Inventory, Special Abilities.
- [ ] "Back" button returns to main menu.

**UI Check:**
- [ ] Dungeon theme consistent with main menu.
- [ ] Text is readable (correct contrast and font size).
- [ ] No clipping or scroll overflow.

---

### PBI-20 · Inventory Popup UI
**Priority:** 🟠 High  
**Assignee suggestion:** Efekan  
**Branch:** `fix/PBI-20-inventory-popup-ui`

**Description:**  
Inventory popup must show all 8 slots in a 2×4 grid with correct item sprites, names, and action buttons.

**Acceptance Criteria:**
- [ ] All 8 slots displayed; empty slots show a faded empty-slot graphic.
- [ ] Right-click or action button on an item shows: USE / EQUIP / WEAR / DISCARD options (contextual based on item type).
- [ ] Actions execute correctly and popup updates in real time.
- [ ] Game is paused while popup is open; resumes on close.
- [ ] Popup title shows "Inventory (N/8)".

**UI Check:**
- [ ] Item sprites are correct (no wrong sprite for item type).
- [ ] Selected item is highlighted.
- [ ] Popup appears centred on screen, not behind the HUD.

---

### PBI-21 · Game Over & Victory Screens
**Priority:** 🟡 Medium  
**Assignee suggestion:** Caner  
**Branch:** `fix/PBI-21-end-screens`

**Description:**  
Themed end screens must show final stats and options.

**Acceptance Criteria:**
- [ ] Game Over screen shows: cause of death, HP at death, time played, floors cleared, enemies defeated.
- [ ] Victory screen shows: time taken, items collected, enemies defeated.
- [ ] Both screens have "Return to Menu" and "Play Again" buttons.
- [ ] Buttons work correctly.

**UI Check:**
- [ ] Game Over uses dark red theme; Victory uses gold/bright theme.
- [ ] Stats are formatted cleanly in a table or card layout.
- [ ] Animated fade-in on screen appearance.

---

## Epic 2 — Bonus Features

To be started only after all Epic 1 PBIs are complete and merged to `main`.

---

### PBI-22 · Mini-Map Overlay (Bonus Feature 1)
**Priority:** 🟢 Bonus  
**Assignee suggestion:** Ikbal  
**Branch:** `feat/PBI-22-minimap`

**Description:**  
A small semi-transparent mini-map overlay in the top-right corner of the game canvas, showing the entire map layout and the hero's current position.

**Acceptance Criteria:**
- [ ] Mini-map renders all tiles as colour-coded pixels (wall=dark grey, floor=light grey, hero=bright yellow, enemy=red, item=cyan).
- [ ] Mini-map updates every frame.
- [ ] Toggle with `M` key.
- [ ] Mini-map scales to fit within a 150×150 px area regardless of map size.
- [ ] Hero position dot blinks to stand out.

**UI Check:**
- [ ] Semi-transparent dark background behind mini-map.
- [ ] Does not obstruct gameplay area.

---

### PBI-23 · Difficulty Selection (Bonus Feature 2)
**Priority:** 🟢 Bonus  
**Assignee suggestion:** Caner  
**Branch:** `feat/PBI-23-difficulty`

**Description:**  
Player selects Easy / Normal / Hard difficulty before starting a new game. Difficulty affects enemy stats, spawn rate, and starting hero energy.

**Acceptance Criteria:**
- [ ] Difficulty selection screen appears after clicking "New Game" or "Random Dungeon".
- [ ] Easy: enemies have −30% HP and ATK; hero starts with 150 energy.
- [ ] Normal: default values.
- [ ] Hard: enemies have +30% HP and ATK; hero starts with 75 energy; no health potions on map.
- [ ] Difficulty is saved in game state JSON and shown on end screen.

**UI Check:**
- [ ] Three styled buttons with difficulty labels and brief descriptions.
- [ ] Selected difficulty highlighted before confirming.

---

### PBI-24 · Floor Progression & Stairs (Bonus Feature 3)
**Priority:** 🟢 Bonus  
**Assignee suggestion:** Ata  
**Branch:** `feat/PBI-24-floor-progression`

**Description:**  
After the hero picks up the target key and reaches the exit, a new procedurally-generated floor loads. Enemies scale with floor number.

**Acceptance Criteria:**
- [ ] A staircase `StaticObject` appears on the map when hero has the target key in inventory.
- [ ] Stepping on the staircase loads a new `ProceduralMapGenerator` floor.
- [ ] Floor number increments and is shown in the HUD.
- [ ] Enemy HP and ATK scale by `(1 + 0.15 × floorNumber)`.
- [ ] Hero HP, inventory, and equipped items persist across floors.
- [ ] After floor 5, a "You Escaped the Dungeon!" victory screen appears.

**UI Check:**
- [ ] Staircase sprite renders correctly (use existing tile from `walls_x3.png`).
- [ ] Floor counter displayed in HUD top-right area.
- [ ] Brief transition screen ("Floor N") shown between floors.

---

## Epic 3 — Code Quality & Test Hygiene

These tasks do not affect gameplay but are required for the final submission.

---

### PBI-25 · Fix BUG-01 & BUG-02 Tests
**Priority:** 🔴 Critical  
**Assignee suggestion:** Batuhan  
**Branch:** covered by `fix/PBI-07-shadow-clone-bugs`

Tracked in Bug Registry (see Test Plan). After PBI-07 fix, these test methods must turn green:
- `cloneMovesOppositeToHero{Right, Down, Left, Up}`
- `cloneTakesNoDamage{WhenHeroIsHit, OnLargeHit}`

---

### PBI-26 · Run Full Test Suite on main
**Priority:** 🟠 High  
**Assignee suggestion:** All  
**Branch:** `fix/PBI-26-test-suite-main`

**Acceptance Criteria:**
- [ ] All 6 test branches are rebased onto latest `main`.
- [ ] `run_tests.sh` compiles and runs all test classes without errors.
- [ ] Final test counts: ≥ 113 tests, ≤ 7 intentional FAIL* (only BUG-01/02 related).
- [ ] No unexpected test failures.

---

### PBI-27 · Javadoc Specs on All Non-Trivial Methods
**Priority:** 🟡 Medium  
**Assignee suggestion:** All  
**Branch:** `fix/PBI-27-javadoc`

**Acceptance Criteria:**
- [ ] Every public method in `Action`, `Effect`, `model.character`, `engine` packages has a `@requires / @modifies / @effects` Javadoc block.
- [ ] No TODO or placeholder comments remain in production code.

---

## Summary Table

| ID | Title | Epic | Priority | Assignee |
|----|-------|------|----------|----------|
| PBI-01 | Move Hero | UC Correctness | 🔴 | Ikbal |
| PBI-02 | Attack Enemy | UC Correctness | 🔴 | Caner |
| PBI-03 | Take Item | UC Correctness | 🔴 | Efekan |
| PBI-04 | Eat Consumable | UC Correctness | 🔴 | Efekan |
| PBI-05 | Equip / Unequip Weapon | UC Correctness | 🔴 | Furkan |
| PBI-06 | Wear Armor & Ring | UC Correctness | 🔴 | Furkan |
| PBI-07 | Shadow Clone BUG-01 & BUG-02 | UC Correctness | 🔴 | Batuhan |
| PBI-08 | Read Scroll | UC Correctness | 🟠 | Batuhan |
| PBI-09 | Search Object | UC Correctness | 🟠 | Ikbal |
| PBI-10 | Break Object | UC Correctness | 🟠 | Caner |
| PBI-11 | Container Interaction | UC Correctness | 🟠 | Efekan |
| PBI-12 | Discard Item | UC Correctness | 🟠 | Efekan |
| PBI-13 | Save & Load Game | UC Correctness | 🔴 | Ata |
| PBI-14 | Team Match Mode | UC Correctness | 🔴 | Batuhan |
| PBI-15 | Achievements | UC Correctness | 🟠 | Batuhan |
| PBI-16 | Level Editor | UC Correctness | 🟡 | Ata |
| PBI-17 | HUD Clickable Zones | UI Polish | 🟠 | Batuhan |
| PBI-18 | Main Menu & Transitions | UI Polish | 🟡 | Ikbal |
| PBI-19 | Help Screen | UI Polish | 🟡 | Ikbal |
| PBI-20 | Inventory Popup UI | UI Polish | 🟠 | Efekan |
| PBI-21 | Game Over & Victory Screens | UI Polish | 🟡 | Caner |
| PBI-22 | Mini-Map Overlay | Bonus Feature | 🟢 | Ikbal |
| PBI-23 | Difficulty Selection | Bonus Feature | 🟢 | Caner |
| PBI-24 | Floor Progression & Stairs | Bonus Feature | 🟢 | Ata |
| PBI-25 | Fix BUG-01 & BUG-02 Tests | Code Quality | 🔴 | Batuhan |
| PBI-26 | Full Test Suite on main | Code Quality | 🟠 | All |
| PBI-27 | Javadoc Specs | Code Quality | 🟡 | All |

---

## Suggested 1-Week Schedule

| Day | Focus |
|-----|-------|
| **Day 1 (Mon)** | PBI-07 (Shadow Clone bugs), PBI-13 (Save/Load), PBI-14 (Team Match verify) |
| **Day 2 (Tue)** | PBI-01, PBI-02, PBI-03, PBI-04 (core loop: move → attack → take → eat) |
| **Day 3 (Wed)** | PBI-05, PBI-06 (equipment), PBI-08, PBI-09, PBI-10 (scroll/search/break) |
| **Day 4 (Thu)** | PBI-11, PBI-12 (containers/discard), PBI-15 (achievements), PBI-17 (HUD) |
| **Day 5 (Fri)** | PBI-18, PBI-19, PBI-20, PBI-21 (all UI screens polished) |
| **Day 6 (Sat)** | PBI-22, PBI-23, PBI-24 (3 bonus features) |
| **Day 7 (Sun)** | PBI-25, PBI-26, PBI-27 (test suite green, javadoc, final PR to main) |

---

*Last updated: May 2026 · KUrawler Team*
