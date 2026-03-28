# SuperWirelessKit Design

**Date:** 2026-03-28

## Goal

Provide a same-dimension AE2 utility that lets players bind remote channel-requiring AE2 devices directly to a clicked face of an `ME Controller` without placing intermediary wireless connector blocks.

## Product Scope

### In Scope

- Same-dimension only
- Controller-only main station selection
- AE2Stuff-style `queue` and `binding` modes on the tool
- Targets may be AE2 tile devices or `IPartHost`-backed parts
- Persisted binding registry
- No global high-frequency tick loop
- Automatic cleanup of invalid loaded targets
- Per-binding energy penalty based on controller/target Euclidean distance

### Out of Scope

- Cross-dimension links
- Binding to arbitrary network anchors other than `TileController`
- Supporting every possible `IGridNode`; v1 is for channel-requiring endpoint devices
- Client-heavy visualization beyond chat/tooltips/localization

## Core Design

The implementation is split into four layers:

1. `Tool Layer`
   - `queue` mode records device targets.
   - `binding` mode records a clicked controller face and binds queued targets onto that controller face.

2. `Binding Registry`
   - Stores static relationships only.
   - Each binding record contains target identity, controller identity, clicked controller face, binder player identity, explicit dimension ids, and target fingerprints for cleanup/debugging.

3. `Runtime Link Manager`
   - Resolves persisted bindings into live AE2 nodes.
   - Owns active `GridConnection` instances, controller-face occupancy, and computed energy penalties.
   - Revalidates bindings when relevant nodes rebuild or disappear.

4. `AE2 Integration Layer`
   - Uses mixins to hook AE2 node lifecycle and AE energy accounting.
   - Avoids a global polling loop.

## Binding Model

### Controller Endpoint

The clicked controller face is treated as a logical endpoint with its own occupancy bucket. This preserves the player's mental model of "attached to this face" without creating a real intermediary block.

### Target Endpoint

Targets are normalized into one of:

- `TileTargetRef`: world position of a tile device
- `PartTargetRef`: host position + part side

The registry must store target type and side, not just block coordinates, otherwise part bindings become ambiguous after host changes.

### Persisted Identity Fields

Each binding record persists:

- controller dimension id
- controller position
- controller clicked face
- target dimension id
- target position
- target kind
- target side when applicable
- binder AE2 player id and binder UUID
- lightweight fingerprints used to reject stale reattachment

Suggested fingerprints:

- controller tile class name
- tile target block registry id + tile class name
- part target host block registry id + part class name

Fingerprints are not security primitives. They exist only to prevent stale records from silently binding to the wrong object after restart or rebuild at the same coordinates.

### Controller Face Capacity

Each controller face maintains a logical binding list. V1 uses a conservative maximum of `32` persisted bindings per face. Binding attempts beyond that are rejected before any runtime link is created.

This is an intentional user-facing rule, not a claim that AE2 itself exposes a remote-face API.

## Binding Workflow

1. Player switches tool into `binding` mode.
2. Player clicks a `TileController` face.
3. Tool records `controllerPos + clickedFace`.
4. Tool iterates queued targets.
5. For each target:
   - resolve target node
   - verify same dimension
   - verify target still exists
   - reject duplicate binding
   - reject controller-face over-capacity
   - verify permissions
   - tentatively create runtime link
   - verify channel allocation succeeded
   - persist binding only if the tentative link is valid

If one queued target fails, the tool reports the reason and proceeds to the next queued target.

## Permission and Security Rules

The tool must verify `BUILD` permission on the controller network.

If the target is already on a grid, the tool also verifies `BUILD` permission on that grid.

Because `GridConnection` ultimately calls `Platform.securityCheck`, runtime link creation must also respect AE2 node ownership semantics. The module therefore stores the binder player identity and uses a **temporary owner override during connection creation only**.

Connect procedure:

1. capture the original target node player id
2. temporarily set the target node player id to the binder player id
3. create the `GridConnection`
4. restore the original target node player id in a `finally` block

This avoids persisting foreign node ownership changes into the target device itself.

If the target already belongs to a different secure network and the security keys are incompatible, binding is rejected.

## Runtime Link Lifecycle

### Active Links

An active link contains:

- binding id
- resolved controller node
- resolved target node
- live `GridConnection`
- computed energy penalty

The binding registry does not store live connection objects.

### Creation

The runtime link manager creates a `GridConnection` only after both endpoints are loaded and valid.

The runtime direction remains `ForgeDirection.UNKNOWN`. The clicked controller face is tracked separately for occupancy and UX. This avoids pretending the connection is a real adjacent block edge while still preserving controller-face semantics at the module level.

### Revalidation

Revalidation is triggered by:

- successful bind/unbind actions
- `GridNode.updateState()` after vanilla neighbor scanning
- `GridNode.destroy()` for active-link teardown
- chunk-load processing for loaded controllers/targets
- world-load bootstrap for persisted binding restoration

The manager does not rely on a world tick loop.

### Cleanup

Cleanup rules:

- If an endpoint unloads because its chunk unloads, destroy the active runtime link but keep the persisted binding.
- If a persisted target is loaded and no longer resolves to the expected device/part, remove that binding from the registry.
- If a loaded controller face loses capacity because of its own persisted occupancy rule, reject new binds but do not silently rewrite existing bindings.

This satisfies the requirement to stop recording invalid connections without expensive polling.

## AE2 Hook Strategy

### Required Mixins

1. `GridNode.updateState()` tail
   - Let vanilla physical scanning run first.
   - Ask the runtime link manager to refresh bindings related to this node.

2. `GridNode.destroy()` head/tail
   - Tear down any active runtime links touching this node.
   - Keep persisted bindings unless the loaded target is known invalid.

3. `EnergyGridCache.EnergyNodeChanges(MENetworkPowerIdleChange)`
   - Add per-binding remote-link penalty to the target node's idle draw.

### Required Forge/Event Hooks

- `WorldEvent.Load`
  - bootstrap persisted controller/target indexes for that world
- `ChunkEvent.Load`
  - schedule refresh for bindings whose controller or target became loaded

These hooks replace any need for a polling loop and ensure restart/chunk-reload restoration does not depend on an unrelated neighbor update.

### Rejected Hook

The original proposal of "append one `UNKNOWN` connection inside `FindConnections()` and let AE2 manage the rest" is insufficient.

`FindConnections()` only manages six physical directions and does not naturally own the lifecycle of an extra virtual `UNKNOWN` edge. The module therefore needs an explicit runtime link index.

## Energy Model

Penalty is computed per active binding using the same distance-based formula family as AE2Stuff:

- base cost
- distance multiplier
- Euclidean distance

The penalty is attributed to the target node during AE2 idle-power accounting. When a link is created, destroyed, or its penalty changes, the module posts an AE2 idle-power-change event for the target node so the energy cache recomputes cleanly.

## Target Resolution Rules

### Tile Targets

- Resolve the tile at the recorded position.
- Confirm it is still an `IGridHost`.
- Resolve the correct grid node for the device.

### Part Targets

- Resolve the host tile.
- Confirm it is still an `IPartHost`.
- Resolve the recorded side part.
- Confirm the part still exposes the expected `IGridNode`.

If resolution fails while the chunk is loaded, prune the binding.

## Failure Semantics

Binding fails immediately and is not persisted when:

- target no longer exists
- target is not a supported endpoint
- target already has a different binding
- controller face is full
- controller or target permissions fail
- AE2 security check fails
- tentative link does not result in a valid channel assignment

## Testing Strategy

### Unit Tests

- registry serialization
- persisted identity/fingerprint round-trip
- controller-face occupancy rules
- target reference equality and duplicate rejection
- energy penalty calculation
- invalid loaded target pruning

### Integration Tests

- binding a tile target
- binding a part target
- rejecting cross-dimension targets
- rejecting controller-face overflow
- queued batch bind with partial success
- temporary owner override restores the original owner after connect attempt
- runtime reconnect after node rebuild
- runtime reconnect after world/chunk load

### Manual Verification

- queue mode UX
- binding mode UX
- chunk unload/reload
- server restart persistence
- channel exhaustion behavior

## Main Risks

- AE2 internals are version-sensitive; hooks must be verified against the exact dependency version in this repo.
- Direct node-owner mutation may affect edge-case security behavior on third-party devices.
- Some unusual modded AE2 devices may expose `IGridNode` in ways that require target-specific exclusions.
