# NHAEUtilities

[![MC Version](https://img.shields.io/badge/Minecraft-1.7.10-brightgreen)](https://github.com/NHAEUtilities/NHAEUtilities)
[![Forge](https://img.shields.io/badge/Forge-10.13.4.1614-red)](https://files.minecraftforge.net/)
[![Version](https://img.shields.io/badge/version-0.1.0-blue)](https://github.com/NHAEUtilities/NHAEUtilities)

A GTNH (GregTech: New Horizons) utility mod providing three functional modules: Pattern Generator, Pattern Routing, and Super Wireless Kit.

[中文](README.md)

## Table of Contents

- [Dependencies](#dependencies)
- [Installation](#installation)
- [Modules](#modules)
  - [Pattern Generator](#1-pattern-generator)
  - [Pattern Routing](#2-pattern-routing)
  - [Super Wireless Kit](#3-super-wireless-kit)
- [Building](#building)
- [Configuration](#configuration)
- [License](#license)

## Dependencies

| Mod | Minimum Version |
|-----|-----------------|
| GregTech | 5.09.51.482 |
| Applied Energistics 2 | rv3-beta-690 |
| Not Enough Items | 2.8.44 |

## Installation

1. Download the latest JAR from [Releases](https://github.com/NHAEUtilities/NHAEUtilities/releases)
2. Place the JAR into your Minecraft instance's `mods/` directory
3. Launch the game — modules can be toggled independently in the config

## Modules

### 1. Pattern Generator

**Item:** Pattern Generator

Batch-encode GregTech machine recipes into AE2 processing patterns, with advanced filtering, ore dictionary replacement, and conflict resolution.

#### Features

- **Batch Encoding** — Build/refresh the cache, then export GT machine recipes as processing patterns in bulk
- **Smart Filtering** — Regex-based filtering, ore dictionary replacement rules, and voltage tier filtering
- **Explicit Syntax** — Filter fields use `[ID]` / `(ore dict regex)` / `{display name regex}`; use `*` to match all
- **Conflict Resolution** — When multiple recipes produce the same output, resolve via GUI
- **Virtual Storage** — Generated patterns stored in internal storage, not directly in inventory
- **Consumption Tracking** — Auto-consumes blank patterns from a bound ME network or player inventory

#### Usage

| Action | Effect |
|--------|--------|
| Right-click air | Open the generator GUI |
| Shift + right-click air | Open the virtual storage GUI |
| Shift + right-click block | Inspect block / export patterns to container |
| Bind to security terminal | Bind to an ME network for auto-consumption of blank patterns |

#### Commands

```
/patterngen list                        # List all recipe maps
/patterngen count <id> [filters...]     # Preview matching recipe count
/patterngen generate <id> [filters...]  # Generate patterns
```

---

### 2. Pattern Routing

**Item:** Recipe Map Analyzer

Automatically route AE2 encoded patterns into matching GregTech multiblock crafting input hatches, with recipe analysis capabilities.

#### Features

- **Auto Routing** — Encoded patterns are automatically routed to the correct multiblock controller's input hatches upon creation
- **Slot Refresh** — Input bus slots are reassigned when a multiblock structure forms
- **Slot Auto-Config** — Empty input bus slots are auto-configured upon receiving their first pattern
- **Manual Item Extraction** — Automatically extracts required manual/circuit items from the AE network when configuring empty slots
- **Recipe Map Analysis** — Analyzes recipe maps, displays duplicate and single-occurrence input types, and calculates the minimum number of patterns needed

#### Usage

| Action | Effect |
|--------|--------|
| Right-click air | Open the recipe analysis GUI |
| Shift + right-click GT machine | Detect and save the recipe map |

#### Commands

```
/nau repairrouting    # Repair blank routing metadata in loaded multiblock controllers
```

---

### 3. Super Wireless Kit

**Item:** Super Wireless Kit

Wirelessly connect arbitrary AE2 channel devices to an ME controller — no cables or quantum bridges needed.

#### Features

- **Dual Mode** — QUEUE (record targets) and BIND (connect targets to controller)
- **Batch Collection** — Sneak + left-click a device to recursively collect adjacent channel devices
- **Virtual Grid Connection** — Creates virtual GridConnection between the controller node and target devices
- **Persistent Binding** — Bindings are saved to world data and auto-reconnect on chunk reload / node refresh
- **Permission Checks** — Respects AE2 SecurityPermissions.BUILD

#### Usage

| Mode | Action | Effect |
|------|--------|--------|
| QUEUE | Shift + right-click device | Add target to queue |
| QUEUE | Sneak + left-click device | Batch-collect adjacent channel devices recursively |
| BIND | Shift + right-click controller | Execute binding — connect all queued targets |
| Any | Right-click air | Toggle between QUEUE / BIND mode |

---

## Building

```bash
# Format check + compile + test + package
./gradlew build

# Fix formatting violations
./gradlew spotlessApply
```

## Configuration

Module toggles and debug settings are managed via Forge configuration:
- In-game: `Mods → NHAEUtilities → Config`
- File path: `<instance>/config/nhaeutilities.cfg`

## License

This project has no license specified at this time.
