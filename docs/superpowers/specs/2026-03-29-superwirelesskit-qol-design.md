# SuperWirelessKit QoL Design

## Summary

This change refines the `SuperWirelessKit` workflow in three ways:

1. Show queue and retry counts separately in the tooltip when the player holds `Shift`.
2. Preserve failed or overflow bindings on the item so the player can continue binding them to the next ME controller.
3. Add recursive batch capture with `Shift + Left Click`, starting from one channel-requiring AE device and walking directly adjacent AE devices that also require channels.

## User-Facing Behavior

### Tooltip

- Default hover keeps the compact summary.
- Holding `Shift` reveals:
  - queued target count
  - pending retry binding count

### Binding Retry Rules

- Clicking a controller attempts to bind all currently pending targets for that controller action.
- Any target that cannot be connected in this pass remains on the item.
- The item may be used on another controller later to continue binding the remaining targets.
- While pending retry bindings exist, the item must reject any new single-target or batch capture requests.

### Batch Capture

- `Shift + Left Click` on a valid AE target starts recursive capture.
- Traversal follows the six directly adjacent blocks from each discovered target.
- Only AE targets that require channels are added.
- Already queued targets and already pending bindings are skipped.
- If the item currently has pending retry bindings, batch capture is rejected.

## Design

### State Model

The current split between `queuedTargets` and `pendingBindings` remains:

- `queuedTargets`: targets captured before choosing a controller
- `pendingBindings`: drafted bindings that failed or were deferred during the latest binding attempts

Additional helper queries will make this explicit:

- whether new targets may be added
- counts for queued targets and pending bindings
- duplicate detection across both lists

### Input Handling

`Shift + Left Click` cannot be implemented through the existing item right-click path, so a dedicated interaction/event handler will dispatch batch capture when:

- the player is sneaking
- the held item is `SuperWirelessKit`
- the targeted block resolves to a valid AE channel-requiring target

The existing right-click behavior remains unchanged for queueing and binding.

### Batch Discovery

A new recursive collector will:

- resolve the clicked target
- maintain a visited set by target identity
- inspect all six neighboring blocks
- resolve neighboring parts/tiles that require channels
- continue traversal from newly accepted targets

Traversal is block-adjacency based, not whole-network based.

### Binding Flow

Binding continues to promote queued targets into drafted binding records, then reconcile them against the selected controller.

Required tightening:

- success removes only the completed record
- failed/deferred records remain in `pendingBindings`
- adding new targets is blocked until `pendingBindings` becomes empty

## Affected Areas

- `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/item/ItemSuperWirelessKit.java`
- `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/tool/SuperWirelessKitStackState.java`
- `src/main/java/com/github/nhaeutilities/modules/superwirelesskit/tool/SuperWirelessKitTargetResolver.java`
- new batch interaction/event handler under `superwirelesskit`
- language resources for tooltip and rejection/capture messages
- tests for stack state, batch collection, and interaction gating

## Testing

Implementation will follow TDD with at least:

- tooltip/state tests for separate queued and pending counts
- state tests proving pending bindings block new captures
- binding tests proving failed leftovers remain on the item
- batch discovery tests for recursive neighbor collection and duplicate skipping
- regression verification with full `./gradlew.bat test --no-daemon`
