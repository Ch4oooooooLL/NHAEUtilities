# Pattern generator output slot selection design

## Goal

Allow the Pattern Generator GUI to optionally restrict pattern generation to specific recipe output slots. The user enters a 1-based comma-separated list such as `1`, `2`, or `1,2,3`. Leaving the field blank means all recipe outputs remain eligible.

This selection affects **Generate only**. Preview continues to count matching recipes based on the existing filter set and does not apply output-slot filtering.

## Current behavior

`GuiPatternGen` currently collects recipe-map and filter fields, persists them through `PacketSaveFields`, and sends them to the server through `PacketGeneratePatterns`. On the server, `PacketGeneratePatterns.Handler` loads filtered recipes and passes them through replacement/conflict handling before `PatternGenerationService.generateAndStore(...)` generates patterns from the full `RecipeEntry.outputs` array.

There is currently no way to keep only selected output positions from a multi-output recipe.

## Constraints

- The new field lives in the GUI and follows the existing text-input workflow.
- Input syntax is a comma-separated list of 1-based positive integers.
- Blank input means "all outputs".
- Invalid input must show an error and block generation.
- Preview behavior must not change.
- Validation must happen on the server before generation so malformed or forged client packets cannot bypass checks.
- If any matched recipe cannot satisfy the requested slot selection, generation fails with a user-visible error instead of silently dropping outputs.

## Recommended approach

Add one new text field to `GuiPatternGen`, persist it with the other GUI fields, and send it only on Generate requests. The server parses and validates the field once per request, then applies the selected slots to every candidate recipe before replacement grouping/conflict handling continues.

This keeps the GUI change small, preserves existing preview semantics, and centralizes correctness checks at the server boundary.

## Design

### 1. GUI

Add a new text field in `src/main/java/com/github/nhaeutilities/modules/patterngenerator/gui/GuiPatternGen.java` under the existing `Target tier` control in the Filters section.

Suggested label:
- English fallback: `Output slots`
- Chinese translation: `保留产物位`

Suggested hint text:
- English fallback: `Examples: 1 / 2 / 1,2,3. Blank means all outputs.`

The widget should reuse the same style as the existing filter text fields. The field value should be loaded from held-item NBT when the GUI opens and saved on close like the other fields.

### 2. Persistence and request flow

Add a new NBT key in `src/main/java/com/github/nhaeutilities/modules/patterngenerator/network/PacketSaveFields.java`, for example `NBT_OUTPUT_SLOTS`.

Extend `PacketSaveFields` to:
- carry the new string field,
- serialize and deserialize it,
- write it into the held item NBT.

Extend `PacketGeneratePatterns` to:
- carry the same string field,
- serialize and deserialize it,
- include it in the request fingerprint used by `PatternGenerationRequestGate`.

Do **not** add this field to `PacketPreviewRecipeCount`, because Preview must continue to ignore output-slot filtering.

### 3. Parsing rules

Server-side parsing should treat the field as follows:

- blank or whitespace-only input: no slot restriction;
- otherwise split on commas;
- each token must be non-empty after trimming;
- each token must parse as a base-10 integer;
- each integer must be `>= 1`.

The parser should reject the entire request when any token is invalid. It should not silently ignore bad tokens.

Normalization rules:
- surrounding spaces are allowed and ignored, so `1, 2,3` is valid;
- duplicate indices such as `1,1,2` are rejected instead of deduplicated;
- empty segments such as `1,,2` are rejected;
- signs and decimals such as `-1`, `+1`, `1.5` are rejected.

The parsed representation should preserve ordering only if that simplifies implementation, but filtering semantics are set membership: selecting `2,1` means keep slots 2 and 1, not reorder outputs.

### 4. Recipe-level validation and filtering

After recipes are loaded and the existing recipe-map/filter validation passes, apply output-slot selection only for Generate requests.

For each `RecipeEntry`:
- if no slot restriction is present, keep the recipe unchanged;
- otherwise examine `recipe.outputs`;
- require that every requested slot exists and contains a non-null output stack for that recipe;
- build a new `RecipeEntry` whose `outputs` array contains only the selected output stacks, preserving their original relative order.

If any matched recipe fails this requirement, abort the entire generation request and send an error to the player. Do not continue with a partial recipe set.

This keeps behavior predictable: the same selection string must be valid for every recipe included in the generation batch.

### 5. Error handling

Generation should be blocked with a clear message for these cases:

1. **Syntax error**
   - examples: `a`, `1,,2`, `0`, `-1`
   - message should explain that only comma-separated positive integers are allowed.

2. **Duplicate slot index**
   - example: `1,1,2`
   - message should mention duplicate slot selection.

3. **Requested slot missing in a recipe**
   - example: requesting `3` when a matched recipe has only 2 outputs, or output slot 3 is null
   - message should explain that at least one matched recipe does not contain all requested output slots.

4. **No usable outputs after filtering**
   - this should normally be covered by the missing-slot rule, but keep a defensive check so generation cannot proceed with an empty outputs array.

Error reporting should mirror the existing generation flow: send a user-visible failure message and stop before conflict grouping or pattern storage writes happen.

### 6. Interaction with existing generation pipeline

Apply output-slot filtering before these later steps:
- ore-dictionary replacements,
- grouping recipes by first output for conflict detection,
- final `PatternGenerationService.generateAndStore(...)`.

This ensures downstream logic sees the exact outputs that will be encoded into patterns.

Filtering before conflict grouping is especially important because the first retained output may change after slot selection, which should also change conflict grouping keys accordingly.

### 7. Edge cases to cover

The implementation should explicitly handle these boundaries:

- blank field -> all outputs;
- whitespace-only field -> all outputs;
- spaces around commas -> valid;
- single slot like `1` -> valid;
- multiple slots like `1,2,3` -> valid;
- non-ascending order like `2,1` -> valid, but outputs remain in original recipe order after filtering;
- duplicate numbers -> invalid;
- zero/negative/non-numeric tokens -> invalid;
- empty token from repeated comma or trailing comma -> invalid;
- requested slot beyond recipe output count -> invalid and blocks generation;
- requested slot refers to a null output entry -> invalid and blocks generation;
- recipe set containing mixed output counts -> valid only if every matched recipe satisfies all requested slots.

### 8. Tests

Add focused tests for the new behavior.

#### `PacketSaveFieldsTest`
- persists the output-slot field into NBT for pattern-generator items.

#### New tests around `PacketGeneratePatterns`
Prefer extracting parsing/filtering into small package-private helpers so the logic can be unit-tested without full packet execution.

Cover:
- blank input returns no restriction;
- valid input parses correctly;
- spaces are trimmed;
- duplicate tokens are rejected;
- invalid numeric forms are rejected;
- empty segments are rejected;
- filtering keeps only selected outputs;
- filtering preserves original relative output order;
- filtering fails when any requested slot is missing or null;
- generation request fingerprint changes when output-slot input changes.

#### Language resources
Add `en_US.lang` and `zh_CN.lang` entries for:
- field label,
- field hint,
- validation errors if they are exposed through translation keys.

## Out of scope

- Applying output-slot filtering to Preview.
- Supporting ranges like `1-3`.
- Supporting wildcard syntax.
- Reordering outputs according to user-entered sequence.
- Auto-correcting malformed input.
