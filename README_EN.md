# NHAEUtilities

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.7.10-62b47a?style=flat-square&logo=mojang" alt="MC">
  <img src="https://img.shields.io/badge/Forge-10.13.4.1614-f16436?style=flat-square" alt="Forge">
  <img src="https://img.shields.io/badge/Version-0.3-blue?style=flat-square" alt="Version">
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Java-21-e76f00?style=flat-square&logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/GTNH-Compatible-ffaa00?style=flat-square" alt="GTNH">
</p>

<p align="center"><strong>A utility mod for GregTech: New Horizons</strong> — Pattern Generation · Pattern Routing · Wireless ME</p>

---

## At a Glance

| Module | Item | Core Capability |
|--------|------|-----------------|
| **Pattern Generator** | Pattern Generator | Batch-encode GT machine recipes as AE2 processing patterns with filtering and conflict resolution |
| **Pattern Routing** | Recipe Map Analyzer | Auto-route AE2 patterns to GT multiblock input hatches, analyze recipe maps |
| **Super Wireless Kit** | Super Wireless Kit | Wirelessly connect AE2 channel devices to an ME controller — no cables needed |

[中文](README.md)

---

## Table of Contents

- [Quick Start](#quick-start)
- [Pattern Generator](#pattern-generator)
- [Pattern Routing](#pattern-routing)
- [Super Wireless Kit](#super-wireless-kit)
- [Building](#building)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)

---

## Quick Start

**Dependencies**

| Mod | Minimum Version |
|:----|:----------------|
| GregTech | 5.09.51.482 |
| Applied Energistics 2 | rv3-beta-690 |
| Not Enough Items | 2.8.44 |

**Installation**

1. Download the latest JAR from [Releases](https://github.com/Ch4oooooooLL/NHAEUtilities/releases)
2. Place it in your Minecraft instance's `mods/` folder
3. Launch — each module can be toggled independently in the config

> **Tip** — On first use or after mod/config changes, click **Cache** in the Pattern Generator interface to build the recipe cache.

---

## Pattern Generator

Batch-encode GregTech machine recipes into AE2 processing patterns.

### Features

- **Batch Encoding** — One-click export of GT machine recipes after cache build
- **Smart Filtering** — Regex filtering, ore dictionary replacement, voltage tier selection
- **Explicit Syntax** — Fields use `[ID]` / `(ore dict regex)` / `{display name regex}`; `*` matches all
- **Conflict Resolution** — Manual GUI selection when multiple recipes produce the same output
- **Virtual Storage** — Patterns stored internally, not cluttering inventory
- **Consumption Tracking** — Auto-consumes blank patterns from bound ME network or inventory

### Usage

| Interaction | Effect |
|:------------|:-------|
| Right-click air | Open the **main config terminal** |
| Shift + right-click air | Open the **storage manager** |
| Shift + right-click block | Inspect GT machine recipe / export patterns to container |
| Shift + right-click security terminal | Bind ME network for blank pattern consumption |

<details>
<summary>Commands</summary>

```
/patterngen list                        # List all recipe maps
/patterngen count <id> [filters...]     # Preview matching recipe count
/patterngen generate <id> [filters...]  # Generate patterns
```
</details>

---

## Pattern Routing

Recipe Map Analyzer automatically routes AE2 patterns to matching GregTech multiblock input hatches, with recipe analysis capabilities.

### Features

- **Auto Routing** — Patterns are automatically dispatched to matching multiblock controller input hatches
- **Slot Refresh** — Buses are reassigned when a multiblock structure forms
- **Slot Auto-Config** — Empty input bus slots are auto-configured on receiving their first pattern
- **Manual Item Extraction** — Auto-extracts programming circuits and manual items from the AE network
- **Recipe Analysis** — Analyzes recipe maps, displays duplicate and single-occurrence input types, calculates minimal pattern assembly count
- **Filter Rules** — Blacklist and manual match rules for precise routing control

### Usage

| Interaction | Effect |
|:------------|:-------|
| Right-click air | Open the **recipe analysis GUI** |
| Shift + right-click GT machine | Save the machine's recipe map for analysis and routing |

<details>
<summary>Commands</summary>

```
/nau repairrouting    # Repair blank routing metadata in loaded multiblock controllers
```
</details>

---

## Super Wireless Kit

Wirelessly connect AE2 channel devices to an ME controller.

### Features

- **Dual Mode** — QUEUE (record targets) / BIND (connect targets to controller)
- **Batch Collection** — Sneak + left-click to recursively collect adjacent channel devices
- **Virtual Grid Connection** — Creates virtual GridConnection between controller node and targets
- **Persistent Binding** — Bindings saved to world data, auto-reconnect on chunk reload / node refresh
- **Permission Checks** — Respects AE2 SecurityPermissions.BUILD

### Usage

| Mode | Interaction | Effect |
|:-----|:------------|:-------|
| QUEUE | Shift + right-click device | Add to connection queue |
| QUEUE | Sneak + left-click device | Batch-collect adjacent channel devices |
| BIND | Shift + right-click controller | Connect all queued targets wirelessly |
| Any | Right-click air | Toggle QUEUE / BIND |

---

## Building

```bash
JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk ./gradlew build          # format + compile + test + jar
JAVA_HOME=/usr/lib/jvm/java-21-temurin-jdk ./gradlew spotlessApply  # fix formatting violations
```

> **Note** — Java 21 is required. Java 25 is incompatible with Gradle 8.12.

---

## Configuration

Module toggles and debug options are managed via Forge configuration:

- In-game: `Mods → NHAEUtilities → Config`
- File: `<instance>/config/nhaeutilities.cfg`

---

## Contributing

Issues and pull requests are welcome. Please follow the repository commit style:

```
feat: short description
fix: short description
```

---

## License

[MIT](LICENSE)
