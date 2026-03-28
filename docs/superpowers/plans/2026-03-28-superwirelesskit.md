# SuperWirelessKit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a same-dimension SuperWirelessKit module that directly binds remote AE2 tile/part devices to a clicked ME Controller face using persisted bindings, mixin-backed runtime links, and no global polling.

**Architecture:** The implementation adds a new module alongside `pattern-generator`, enables mixins for AE2 node lifecycle and energy accounting, persists binding records in a world registry, and uses a runtime manager to own virtual `GridConnection` lifecycle. Tool UX follows AE2Stuff queue/binding semantics, while controller-face occupancy and channel acceptance are enforced conservatively.

**Tech Stack:** Java 17 source level via GTNH RFG, Minecraft Forge 1.7.10, Applied Energistics 2 Unofficial rv3-beta-690/695-compatible internals, Sponge Mixin, JUnit 4.

---

### Task 1: Enable mixins and scaffold the module

**Files:**
- Modify: `gradle.properties`
- Modify: `src/main/java/com/github/nhaeutilities/core/config/CoreConfig.java`
- Modify: `src/main/java/com/github/nhaeutilities/proxy/CommonProxy.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/SuperWirelessKitModule.java`
- Create: `src/main/resources/mixins.nhaeutilities.json`
- Modify: `src/main/resources/assets/nhaeutilities/lang/en_US.lang`
- Modify: `src/main/resources/assets/nhaeutilities/lang/zh_CN.lang`
- Test: `src/test/java/com/github/nhaeutilities/core/module/ModuleRegistryTest.java`

- [ ] Write failing tests or assertions for module registration/config gating where practical.
- [ ] Verify the exact AE2 target classes/method signatures against the repo dependency before writing mixins.
- [ ] Enable mixin build settings in `gradle.properties`.
- [ ] Register the new module in `CommonProxy` and expose its core config flag.
- [ ] Add the mixin config resource and empty package skeleton.
- [ ] Run `./gradlew.bat test` and confirm the project still builds with mixins enabled.
- [ ] Commit: `feat: scaffold super wireless kit module`

### Task 2: Implement binding model and persistence

**Files:**
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/data/BindingRecord.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/data/BindingTargetKind.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/data/BindingTargetRef.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/data/ControllerEndpointRef.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/data/SuperWirelessSavedData.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/service/BindingRegistry.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/service/BindingFingerprint.java`
- Test: `src/test/java/com/github/nhaeutilities/modules/superwirelesskit/data/SuperWirelessSavedDataTest.java`
- Test: `src/test/java/com/github/nhaeutilities/modules/superwirelesskit/service/BindingRegistryTest.java`

- [ ] Write failing tests for NBT serialization, duplicate detection, controller-face occupancy with a hard `32`-binding-per-face limit, and fingerprint round-trip.
- [ ] Implement immutable-ish binding record/value classes.
- [ ] Persist binder AE2 player id plus UUID for reconnect-time temporary owner override.
- [ ] Implement world persistence backed by `WorldSavedData`.
- [ ] Implement registry APIs for add/remove/query/prune.
- [ ] Run targeted tests, then `./gradlew.bat test`.
- [ ] Commit: `feat: add super wireless binding registry`

### Task 3: Implement target resolution and tool UX

**Files:**
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/item/ItemSuperWirelessKit.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/item/ModItems.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/service/TargetResolver.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/service/BindingPermissionService.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/service/NodeOwnershipOverride.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/util/ItemQueueStore.java`
- Modify: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/SuperWirelessKitModule.java`
- Modify: `src/main/java/com/github/nhaeutilities/proxy/CommonProxy.java`
- Test: `src/test/java/com/github/nhaeutilities/modules/superwirelesskit/util/ItemQueueStoreTest.java`
- Test: `src/test/java/com/github/nhaeutilities/modules/superwirelesskit/service/TargetResolverTest.java`
- Test: `src/test/java/com/github/nhaeutilities/modules/superwirelesskit/service/NodeOwnershipOverrideTest.java`

- [ ] Write failing tests for queue storage and target reference parsing.
- [ ] Port AE2Stuff-like queue/binding mode NBT behavior into `ItemQueueStore`.
- [ ] Implement controller click capture and queued target capture for tiles and parts.
- [ ] Implement server-side permission checks plus explicit same-dimension rejection.
- [ ] Implement temporary owner override with guaranteed restoration after connect attempt.
- [ ] Register the item and crafting recipe.
- [ ] Run targeted tests, then `./gradlew.bat test`.
- [ ] Commit: `feat: add super wireless kit item workflow`

### Task 4: Implement runtime link manager and AE2 lifecycle hooks

**Files:**
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/runtime/ActiveLink.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/runtime/RuntimeLinkManager.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/runtime/LinkEnergyCalculator.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/runtime/BindingWorldEvents.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/mixin/GridNodeMixin.java`
- Create: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/mixin/EnergyGridCacheMixin.java`
- Test: `src/test/java/com/github/nhaeutilities/modules/superwirelesskit/runtime/RuntimeLinkManagerTest.java`
- Test: `src/test/java/com/github/nhaeutilities/modules/superwirelesskit/runtime/LinkEnergyCalculatorTest.java`
- Test: `src/test/java/com/github/nhaeutilities/modules/superwirelesskit/runtime/BindingWorldEventsTest.java`

- [ ] Write failing tests for occupancy accounting, invalid loaded target pruning, world/chunk load refresh, and penalty calculation.
- [ ] Implement active-link indexing and teardown behavior.
- [ ] Implement tentative connect/rollback logic for binding acceptance.
- [ ] Inject into `GridNode.updateState()` and `GridNode.destroy()` to refresh/destroy active links.
- [ ] Inject into AE2 energy accounting so bound targets report additional idle draw.
- [ ] Add world/chunk load hooks that reschedule refresh of persisted bindings after restart or chunk reload.
- [ ] Run targeted tests, then `./gradlew.bat test`.
- [ ] Commit: `feat: add runtime super wireless links`

### Task 5: Integrate bind action, cleanup semantics, and user feedback

**Files:**
- Modify: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/item/ItemSuperWirelessKit.java`
- Modify: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/service/BindingRegistry.java`
- Modify: `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/runtime/RuntimeLinkManager.java`
- Modify: `src/main/resources/assets/nhaeutilities/lang/en_US.lang`
- Modify: `src/main/resources/assets/nhaeutilities/lang/zh_CN.lang`
- Create: `src/test/java/com/github/nhaeutilities/modules/superwirelesskit/item/ItemSuperWirelessKitBindingTest.java`

- [ ] Write failing tests for bind rejection reasons, explicit cross-dimension rejection, invalid binding cleanup, and queued partial-success behavior.
- [ ] Wire the item bind action to use registry + runtime manager tentative link creation.
- [ ] Add chat/status messaging for success and failure reasons.
- [ ] Ensure loaded invalid targets are pruned while unloaded chunks are preserved.
- [ ] Ensure one failed queued target does not abort the rest of the batch.
- [ ] Run targeted tests, then `./gradlew.bat test`.
- [ ] Commit: `feat: finalize super wireless binding flow`

### Task 6: Full verification and repository hygiene

**Files:**
- Modify: `README.md` if module listing needs updating
- Modify: `src/main/resources/mcmod.info` if exposed metadata changes

- [ ] Run full `./gradlew.bat test`.
- [ ] Run `./gradlew.bat build`.
- [ ] Manually inspect git status for unwanted tracked worktree/doc noise.
- [ ] Update README/module docs only if the implemented surface is user-visible and stable.
- [ ] Commit: `docs: document super wireless kit module` if docs changed.
