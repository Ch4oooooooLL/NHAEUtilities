# Forge Config Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize the Forge config screen so it is module-first and uses player-facing groups inside each module.

**Architecture:** The GUI will stop listing every root configuration category and will instead surface module categories directly. Pattern Generator and Super Wireless Kit settings will be nested under `modules.<module>.*`, with Pattern Generator subgroups aligned to the approved player-facing structure. Translations and tests will be updated in lockstep so category paths and labels stay stable.

**Tech Stack:** Java 8, Forge/FML config GUI, JUnit 4, lang `.lang` resources

---

### Task 1: Lock down the new config hierarchy with failing tests

**Files:**
- Create: `src/test/java/com/github/nhaeutilities/core/config/CoreConfigStructureTest.java`
- Modify: `src/test/java/com/github/nhaeutilities/modules/patterngenerator/config/ForgeConfigTest.java`
- Test: `src/test/java/com/github/nhaeutilities/modules/patterngenerator/util/LangKeyCompletenessTest.java`

- [ ] **Step 1: Write the failing tests**

Add tests that expect:
- module enabled properties under `modules.patternGenerator.basic` and `modules.superWirelessKit.basic`
- Pattern Generator subgroup categories under `modules.patternGenerator.*`
- updated language keys for new category names

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "*CoreConfigStructureTest" --tests "*ForgeConfigTest" --tests "*LangKeyCompletenessTest"`

Expected: failures because current categories still use the old layout and old language keys.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/github/nhaeutilities/core/config/CoreConfigStructureTest.java src/test/java/com/github/nhaeutilities/modules/patterngenerator/config/ForgeConfigTest.java src/test/java/com/github/nhaeutilities/modules/patterngenerator/util/LangKeyCompletenessTest.java
git commit -m "test: lock down forge config layout"
```

### Task 2: Implement the new module-first config structure

**Files:**
- Modify: `src/main/java/com/github/nhaeutilities/core/config/CoreConfig.java`
- Modify: `src/main/java/com/github/nhaeutilities/modules/patterngenerator/config/ForgeConfig.java`
- Modify: `src/main/java/com/github/nhaeutilities/client/gui/NHAEUtilitiesConfigGui.java`

- [ ] **Step 1: Write the minimal implementation**

Update:
- core module enable paths to `modules.<module>.basic.enabled`
- Pattern Generator category constants to `modules.patternGenerator.*`
- config GUI top-level element builder to show module categories directly

- [ ] **Step 2: Run focused tests**

Run: `./gradlew test --tests "*CoreConfigStructureTest" --tests "*ForgeConfigTest"`

Expected: PASS.

- [ ] **Step 3: Refactor only if needed**

Keep category path helpers or constants readable without changing behavior.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/github/nhaeutilities/core/config/CoreConfig.java src/main/java/com/github/nhaeutilities/modules/patterngenerator/config/ForgeConfig.java src/main/java/com/github/nhaeutilities/client/gui/NHAEUtilitiesConfigGui.java
git commit -m "feat: reorganize forge config categories"
```

### Task 3: Update labels for the new player-facing groups

**Files:**
- Modify: `src/main/resources/assets/nhaeutilities/lang/en_US.lang`
- Modify: `src/main/resources/assets/nhaeutilities/lang/zh_CN.lang`
- Modify: `src/test/java/com/github/nhaeutilities/modules/patterngenerator/util/LangKeyCompletenessTest.java`

- [ ] **Step 1: Update translation keys**

Add or replace keys for:
- `modules.patternGenerator.basic`
- `modules.patternGenerator.conflict`
- `modules.patternGenerator.requestProtection`
- `modules.patternGenerator.ui`
- `modules.patternGenerator.storage`
- `modules.patternGenerator.advanced`
- `modules.superWirelessKit.basic`

- [ ] **Step 2: Run translation tests**

Run: `./gradlew test --tests "*LangKeyCompletenessTest"`

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/assets/nhaeutilities/lang/en_US.lang src/main/resources/assets/nhaeutilities/lang/zh_CN.lang src/test/java/com/github/nhaeutilities/modules/patterngenerator/util/LangKeyCompletenessTest.java
git commit -m "feat: rename forge config groups for players"
```

### Task 4: Run final verification

**Files:**
- Verify only

- [ ] **Step 1: Run the focused suite**

Run: `./gradlew test --tests "*CoreConfigStructureTest" --tests "*ForgeConfigTest" --tests "*LangKeyCompletenessTest"`

Expected: all selected tests pass.

- [ ] **Step 2: Run the broader project test suite if time allows**

Run: `./gradlew test`

Expected: pass, or clearly report unrelated failures if they already exist.
