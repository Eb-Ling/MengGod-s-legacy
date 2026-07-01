# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

This is a Starsector mod named "神之遗卷" (MengGod's legacy) by Mengmeng. Version 1.1.4, compatible with Starsector 0.98a-RC8.

**Dependencies**: LazyLib, GraphicsLib, MagicLib, BoxUtil (all required)

**Entry point**: `data.scripts.Mengplugin` (extends BaseModPlugin, handles game lifecycle events)

## Build Process

Build via IntelliJ IDEA artifact compilation:
- Source compiled to `jars/Meng.jar`
- Package prefix is `data` (all Java classes are under `data.*`)
- Run MengGL11Tool using `src/MengGL11Tool/jarrun.bat` for OpenGL rendering tests

## Code Architecture

### Package Structure (`src/` directory)

| Directory | Purpose |
|-----------|---------|
| `scripts/` | Main plugin (`Mengplugin.java`) + campaign scripts, bar events, rule commands |
| `hullmods/` | Hull mod implementations (`BaseHullMod` subclasses) |
| `weapons/` | Weapon effect plugins (`BeamEffectPlugin`, `EveryFrameWeaponEffectPlugin`) |
| `shipsystems/scripts/` | Ship system scripts (`BaseShipSystemScript` subclasses) |
| `shipsystems/scripts/ai/` | Ship system AI implementations |
| `skills/` | Skill implementations with level effects |
| `world/` | Sector/world generation (`MengGen.java`) |
| `methods/` | Utility classes (math helpers, rendering plugins) |
| `campaign/econ/` | Economy/market implementations |
| `Utils/` | I18n utility (`I18nUtil.java`) |

### Data Files (`../data/` directory)

| Directory | Contents |
|-----------|----------|
| `hulls/` | `.ship` files + `ship_data.csv`, `wing_data.csv` |
| `weapons/` | `.wpn` files + `weapon_data.csv`, `proj/` projectiles |
| `shipsystems/` | `.system` files + `ship_systems.csv` |
| `hullmods/` | `hull_mods.csv` |
| `world/factions/` | `.faction` files + `default_ship_roles.json` |
| `strings/` | `descriptions.csv`, `strings.json` |
| `config/` | `settings.json`, graphics/sounds config |

### Key Patterns

**Mod Plugin Lifecycle** (`Mengplugin.java`):
- `onGameLoad()` - Register bar event creators
- `onNewGame()` - Generate custom system (Nexerelin compatible)
- `configureXStream()` - XStream aliasing for save compatibility

**Hullmod Pattern** (`hullmods/*.java`):
- Extend `BaseHullMod`
- Use `advanceInCombat()` for combat effects with custom data containers
- Store per-ship state via `ship.setCustomData(KEY, data)`
- Incompatibilities checked in `applyEffectsAfterShipCreation()`

**Ship System Pattern** (`shipsystems/scripts/*.java`):
- Extend `BaseShipSystemScript`
- `apply()`/`unapply()` for stat modifications
- State tracking via instance fields (reset in `unapply()`)

**Weapon Effect Pattern** (`weapons/*.java`):
- `BeamEffectPlugin` for beam weapons
- `EveryFrameWeaponEffectPlugin` for other weapons
- Use `IntervalUtil` for timed effects

**Custom Rendering** (`methods/*.java`):
- `CombatLayeredRenderingPlugin` for OpenGL-based visual effects
- GL11 calls for custom drawing (ribbons, beams, particles)

## Faction Configuration

Custom faction: `Meng_temple` (余烬圣殿)

Key tags for content:
- `Meng_fire_bp` - Blueprint package for ships/weapons
- `Meng_fire_hullmod` - Hullmod access
- `Meng_fire_fighter` - Fighter access

## Nexerelin Compatibility

World generation respects Nexerelin:
```java
if (!haveNexerelin || SectorManager.getManager().isCorvusMode()) {
    new MengGen().generate(Global.getSector());
}
```

## Adding New Content

**New Hullmod**: Create in `src/hullmods/`, register in `data/hullmods/hull_mods.csv`, add description in `data/strings/descriptions.csv`

**New Weapon**: Create effect plugin in `src/weapons/`, define in `data/weapons/*.wpn`, add to `weapon_data.csv`, projectile in `data/weapons/proj/`

**New Ship System**: Create script in `src/shipsystems/scripts/`, define in `data/shipsystems/*.system`, AI in `src/shipsystems/scripts/ai/`

**New Ship**: Create `.ship` file in `data/hulls/`, variant in `data/variants/`, add to `ship_data.csv`

## Text Encoding

All CSV and JSON files use UTF-8. Chinese text is used for descriptions and dialogue.