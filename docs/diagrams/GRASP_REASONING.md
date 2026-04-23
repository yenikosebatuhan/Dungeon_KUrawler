# GRASP Reasoning for Interaction Diagrams

## UC-1: Move Player
- **Controller**: `GameController.moveHero()` handles the system event from the UI — the UI layer contains zero game logic.
- **Creator**: `GameController` creates `MoveAction` because it has the initializing data (deltaRow, deltaCol). `MoveAction` creates `MoveEffect` because it is the Information Expert that validates the target cell.

## UC-2: Attack Enemy
- **Controller**: `GameController.executeAction()` orchestrates the attack pipeline.
- **Creator**: `CombatEngine` creates `AttackAction` because it possesses both the attacker and defender references. `AttackAction` creates `DamageEffect` because it computes the damage amount (Information Expert).
- **Information Expert**: `Hero.calculateDamage()` factors in equipped weapon bonus — Hero knows its own stats best.

## UC-3: Pick Up Item
- **Controller**: `GameController` auto-picks up items after movement, keeping pickup logic out of the UI.
- **Creator**: `TakeAction` creates `RemoveFromCellEffect` and `AddToInventoryEffect` because it is the expert that checks cell contents and inventory capacity.
- **Low Coupling**: Each Effect modifies the model independently — `RemoveFromCellEffect` touches only `GridCell`, `AddToInventoryEffect` touches only `Inventory`.

## UC-4: Use Item (Heal)
- **Controller**: `GameController.executeAction()` handles the consume-item system event.
- **Creator**: `EatAction` creates `HealEffect` because it reads the consumable's heal amount (Information Expert).
- **Information Expert**: `Inventory.removeItem()` manages its own contents. `Character.heal()` caps HP at `maxHealth`.

## UC-5: Enemy AI Turn
- **Controller**: `GameController` executes AI-generated actions the same way it executes player actions.
- **Polymorphism**: `MoveAction` works identically for Hero and Enemy — the same Action-Effect pipeline is reused.
- **Indirection**: `GameEngine` delegates AI decisions to `EnemyAI` and action execution to `GameController`, keeping the game loop cohesive.
- **High Cohesion**: AI logic (`EnemyAI`), game loop (`GameEngine`), and action execution (`GameController`) are cleanly separated.
- **Low Coupling**: `EnemyAI` only produces `Action` objects; it never directly modifies game state.
