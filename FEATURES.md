# Feature inventory

Source baseline: `master` at `e768075` (Minecraft 1.21.10).

## Automatic snapshots

- Captures a player's main inventory, armor, offhand, Ender Chest, XP level, world, coordinates, and timestamp on login and logout.
- Captures the same state on death together with the damage-source name.
- Performs database work through a typed Pekko actor and closes the database/actor system during server shutdown.

## Storage and configuration

- Creates and maintains separate login, logout, death, and pre-restore history tables through ORMLite.
- Supports an embedded per-world H2 database and a configured external MySQL-compatible connection.
- Generates `config/borukva_inventory_backup.json` with database settings and a configurable per-player history limit (default 100).
- Removes the oldest records when a player's configured history limit is reached and migrates older H2 table layouts when needed.
- Serializes complete item stacks, including data components, to NBT text and validates older/missing component structures while reading them.

## Administration and browsing

- Provides `/binvbackup <player>` with `borukva.rollback` permission (operator level 4 fallback) and online-player suggestions.
- Supports looking up both online and previously seen offline players.
- Opens a server-side GUI that separates login, logout, death, and pre-restore histories.
- Provides paginated history screens with record time and, where applicable, location/world and death reason.
- Displays a selected snapshot's equipment, offhand, main inventory, XP level, and Ender Chest contents.

## Recovery and export

- Restores inventory/equipment/offhand/XP snapshots to online players.
- Restores inventory/equipment/offhand/XP snapshots directly to offline player data files.
- Restores Ender Chest snapshots to online or offline players.
- Creates a pre-restore snapshot before either kind of destructive recovery so the previous state can be recovered.
- Exports a snapshot into one or two named chest items, preserving contained item stacks and components.

## Trinkets (optional)

- When Trinkets Updated is installed, every snapshot also stores equipped trinkets, including cosmetic slots, with their slot id and index; older records without trinket data leave trinket slots untouched on restore.
- Adds the `trinkets` column to existing tables on startup (H2 and MySQL).
- Shows a snapshot's trinkets from the inventory screen and includes them in chest exports.
- Restores trinkets to online players directly; items whose slot no longer exists (or cosmetic items when cosmetic slots are disabled) go to the player's inventory.
- Queues trinkets restored to offline players and puts them on at the next login.
