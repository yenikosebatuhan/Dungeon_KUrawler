# Dungeon KUrawler — COMP 302 Group 7

A JavaFX dungeon-crawler built with a layered MVC architecture, Action-Effect pipeline, and Observer pattern.

## Quick Start

```bash
# Requires JDK 21+ and JavaFX SDK 21 (bundled in lib/)
bash run.sh
```

## Features

| Feature | Description |
|---------|-------------|
| **New Game** | Hand-crafted 16×16 dungeon with rooms, corridors, items, and enemies |
| **Random Dungeon** | Procedurally generated 20×20 dungeon — different every run |
| **Level Editor** | Visual tile editor: place walls, items, enemies; save/load maps |
| **Team Match** | 4v4 AI arena — watch two teams fight to elimination |
| **Save / Load** | Named save files with timestamp, stored in `saves/` |
| **Inventory** | 2×4 grid — click items to EQUIP, WEAR, EAT, READ, or DISCARD |
| **Combat** | Weapon-aware `Space` attacks in facing direction + mana projectile (`F`) + click interactions |
| **Enemies** | Knights chase; Sorcerers teleport and fire projectiles with line-of-sight |
| **Shadow Clone** | Mirrors hero movement via Observer pattern |
| **Achievements** | Badge system tracking milestones |
| **Help Screen** | In-game guide accessible from main menu |
| **Audio** | Optional WAV sound effects (place files in `src/main/resources/assets/audio/`) |
| **Pause / Timer** | Pause button + elapsed play time displayed in sidebar |

## Controls

| Key | Action |
|-----|--------|
| W / ↑ | Move up |
| A / ← | Move left |
| S / ↓ | Move down |
| D / → | Move right |
| Space | Attack in facing direction with the equipped weapon |
| E | Interact with adjacent target; use first consumable as fallback |
| F | Shoot mana projectile; equipped staff strengthens the spell |
| ESC | Open in-game menu |
| Mouse click | Interact with adjacent tiles (3×3 range) |

## Architecture

- **Action-Effect Pipeline** — Actions produce Effects; only Effects mutate state
- **MVC** — `GameSession` (Model), `GameView` (View), `GameController` (Controller)
- **Observer** — Hero notifies ShadowClone and AchievementManager
- **Strategy** — Enemy AI varies by type (Knight vs Sorcerer)
- **Memento** — Save/Load via JSON serialization

## Project Structure

```
src/main/java/com/kurawler/
├── action/          # User actions (Move, Attack, Take, Eat, Equip, Wear, Break, Search, Read)
├── controller/      # GRASP Controller
├── editor/          # Level editor backend (MapEditor, ObjectPalette, PlacementValidator)
├── effect/          # State-mutating effects
├── engine/          # GameLoop, EnemySpawner, CombatResolver, AudioManager
├── model/           # Domain model (GameSession, Map, Inventory, characters, objects)
├── persistence/     # JSON serialization / deserialization
└── ui/              # JavaFX screens (GameView, EditorView, MainMenu, HelpScreen)
```
