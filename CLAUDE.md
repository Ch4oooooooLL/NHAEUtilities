# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

See also `AGENTS.md` for build commands, deploy steps, texture mapping, GUI patterns, lang file rules, and commit conventions. This file covers architecture that spans multiple modules.

## Module lifecycle system

All modules implement `ModuleDefinition` (interface with `id()`, `isEnabled()`, and FML lifecycle methods). `ModuleRegistry` dispatches FML lifecycle events (`preInit` → `init` → `postInit` → `serverStarting`) to enabled modules. The registry freezes enabled modules at first dispatch — subsequent calls see the same snapshot. `CommonProxy.createModuleRegistry()` registers the 3 built-in modules: `PatternGeneratorModule`, `PatternRoutingModule`, `SuperWirelessKitModule`.

Each module's enabled state is toggled via `CoreConfig.generalConfig.modules` (a Forge config string set). Disabled modules still declare their config properties but receive no lifecycle events.

## Mixin architecture

Three active mixin configs, two registered in the jar manifest:

| Config | Scope | Key targets |
|--------|-------|------------|
| `mixins.nhaeutilities.patternrouting.json` | Pattern routing | `ContainerPatternTerm`, GT multi-block base, AE2 crafting input hatches, NEI transfer packets |
| `mixins.nhaeutilities.patternrouting.late.json` | Pattern routing (late) | NEI pattern terminal handler and packets — loaded by NEI at runtime via `PatternRoutingLateMixinLoader`, not in jar manifest |
| `mixins.nhaeutilities.superwirelesskit.json` | Super wireless kit | `GridNode` for virtual grid connections |

All mixin classes live in `com.github.nhaeutilities.modules.<module>.mixin` matching their config's `package` field. Accessors are in `com.github.nhaeutilities.accessor.<module>`.

## Network

Each module that needs networking declares its own `SimpleNetworkWrapper` channel. Pattern routing uses a dedicated channel (`modules/patternrouting/network/`) separate from the main mod channel. Packet registration happens during module `preInit`.

## Shared recipe cache

`SharedRecipeCacheService` in `modules/shared/recipecache/` provides GT recipe caching used by both PatternGenerator (for batch encoding) and PatternRouting (for recipe-map analysis). The cache produces raw and semantic snapshots, persisted via NBT. Storage helpers live in the pattern generator module's `storage/` package but the service itself is shared.

## GUI architecture

GUIs use **ModularUI** (not vanilla MC GUI). Key patterns:
- Windows are built via `ModularWindow.builder()`
- Buttons are `ButtonWidget` instances paired with separate `TextWidget` labels sharing the same i18n key
- Client-side screens extend `PatternRoutingAnalysisClientScreen` for analysis results
- GUI handlers in `modules/<module>/gui/` register via `NetworkRegistry.INSTANCE.registerGuiHandler()`

## Config system

`CoreConfig` manages the shared Forge `Configuration` file (`<instance>/config/nhaeutilities.cfg`). All modules (including disabled ones) call `loadConfig()` to declare properties, making them visible in the config GUI. `ConfigGuiLayout` defines the config GUI structure. `ConfigChangeHandler` responds to in-game config changes.
