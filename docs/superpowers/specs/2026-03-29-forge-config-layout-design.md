# Forge Config Layout Design

**Date:** 2026-03-29

## Goal

Reorganize the Forge mod config screen so ordinary players see configuration grouped by module first, then by user-facing function within each module. The screen should stop exposing low-level category paths like `patternGenerator.ui.patternGen` directly.

## Current Problems

- The config GUI mixes module toggles with module internals.
- Pattern Generator settings are exposed through implementation-oriented category names.
- The top-level screen shows both module management and a separate Pattern Generator settings root, which feels duplicated.
- Technical settings are visible with the same prominence as player-facing settings.

## Approved Structure

### Top Level

The config GUI should show module entries directly:

- Pattern Generator
- Super Wireless Kit

This requires the GUI to build its top-level list from module categories rather than every root config category.

### Pattern Generator Groups

Inside the Pattern Generator module, settings should be grouped as:

- Basic Settings
- Conflict Handling
- Request Protection
- Interface
- Storage
- Compatibility & Advanced

### Super Wireless Kit Groups

Super Wireless Kit currently only needs:

- Basic Settings

This keeps the module structure consistent and leaves room for future growth.

## Mapping From Existing Settings

### Pattern Generator

- `enabled` -> Basic Settings
- `batchSize`, `maxFilteredRecipes`, `maxConflictGroups` -> Conflict Handling
- `windowMs` -> Request Protection
- Pattern Generator GUI sizing and Recipe Picker GUI sizing -> Interface
- `directoryName`, `recipeCacheDirectoryName` -> Storage
- `encodedPatternId` -> Compatibility & Advanced

### Super Wireless Kit

- `enabled` -> Basic Settings

## Naming Rules

- Prefer player-facing labels over implementation names.
- Rename “Duplicate Suppression” to “Request Protection”.
- Keep “Compatibility & Advanced” as the obvious place for technical options.
- Keep “Basic Settings” as the first group inside each module.

## Implementation Notes

- Move Pattern Generator categories from `patternGenerator.*` to `modules.patternGenerator.*`.
- Move module enabled properties into `modules.<module>.basic.enabled`.
- Keep the module categories themselves as the visible top-level entries in the GUI.
- Update language keys to match the new category hierarchy.
- Add tests that lock down the new category paths and top-level GUI selection behavior.

## Non-Goals

- No new gameplay settings.
- No behavior changes outside config organization and labels.
- No redesign of unrelated GUIs.
