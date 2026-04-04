# Deprecated API Cleanup — Design Spec

**Date:** 2026-04-04
**Scope:** Replace deprecated Android APIs with modern equivalents as preparation for future targetSdkVersion upgrades. No functional changes.

---

## Goal

xDrip currently targets `targetSdkVersion 24` (Android 7.0) while compiling against SDK 34. A large number of deprecated Android APIs are in use across the codebase (~628 files with `@Deprecated` annotations). This work prepares the codebase for future SDK version bumps by systematically replacing deprecated usages — without introducing any behavioral changes.

This is purely a preparatory cleanup. The targetSdkVersion is **not** changed as part of this work.

---

## Scope

### In scope (ordered by risk, low to high)

| # | Category | Est. files | Risk |
|---|---|---|---|
| 1 | `resources.getColor(int)` / `resources.getDrawable(int)` | 1 | Very low |
| 2 | `SharedPreferences` / `PreferenceManager` patterns | ~82 | Low |
| 3 | `AsyncTask` | ~15 | Medium |
| 4 | `new Thread` (direct thread creation) | ~44 | Medium |
| 5 | `startActivityForResult` / `onActivityResult` | ~15 | Medium |
| 6 | Notifications / `PendingIntent` flags | ~38 | Medium-high |
| 7 | Bluetooth / BLE APIs | ~39 | High |

### Out of scope

- `targetSdkVersion` bump (done separately, after cleanup is complete)
- Logic refactoring unrelated to deprecated API replacement
- Renaming, restructuring, or code style changes
- New features

---

## Approach: Pattern-first, then file-by-file (Hybrid)

Work proceeds category by category. Within each category:

### Phase 1 — Pattern PR

- Branch from `master`
- Select the simplest, most isolated file in the category
- Write Robolectric characterization tests that document existing behavior
- Confirm tests pass on the original code
- Replace the deprecated API in that one file
- Confirm tests still pass
- Commit `docs/cleanup/<category>.md` with the migration note in the same PR
- PR title: `[pattern] Replace <deprecated> with <modern> in <ClassName>`

### Phase 2 — Cleanup PRs (file by file)

- One branch per small batch (max ~3–5 files), always branched from `master`
- Write characterization tests first, confirm green on original code
- Replace deprecated API following the pattern from Phase 1
- Confirm tests still pass
- PR title: `[cleanup] Replace <deprecated> in <ClassName>`

### Pipeline

All PRs (pattern and cleanup) branch independently from `master`. Within a category, cleanup PRs do not depend on each other and can be merged in any order. This means they can be reviewed and merged in parallel if desired.

A category is considered done when all Phase 2 PRs are merged. The next category's Phase 1 PR may be opened for review while Phase 2 of the current category is in progress — to avoid idle time during review waits.

```
Category N:  [pattern PR] → review → [cleanup PR 1] → [cleanup PR 2] → ... → DONE
                                            ↕ (in parallel)
Category N+1: [pattern PR] → review
```

---

## PR Rules

Every PR in this initiative must follow these rules:

**Must contain:**
- Characterization tests written before the refactoring
- Tests that are green on original code AND green after refactoring
- Only the deprecated API replacement — nothing else

**Must not contain:**
- Logic changes
- Renaming or restructuring unrelated to the deprecated API
- New features
- Removal of code not directly tied to the deprecated API

---

## Testing Strategy

**Tool:** Robolectric 4.11.1 (already configured, supports SDK 16–34 — no upgrade needed)

**What characterization tests verify:**
- Observable outputs: return values, state changes, calls to collaborators (via Mockito)
- Existing behavior as-is — not what the code "should" do
- All code paths that involve the deprecated API being replaced

**Coverage requirement:**
- All code paths touching the deprecated API being replaced must have at least one test
- 100% line coverage of the full file is not required
- If a section is untestable without major restructuring, document it explicitly in the PR description

**Test structure:**
```java
@RunWith(RobolectricTestRunner.class)
public class SomeClassTest {

    // --- Setup ---

    // --- methodName ---

    /** Characterization: verifies existing behavior before deprecation cleanup */
    @Test
    public void methodName_condition_expectedBehavior() { ... }
}
```

---

## Documentation

Each category gets a migration note in `docs/cleanup/`. These are committed as part of the pattern PR for that category.

```
docs/cleanup/
  context-resources.md      # getColor/getDrawable → ContextCompat variants
  shared-preferences.md     # PreferenceManager → androidx variant
  async-task.md             # AsyncTask → Executor / coroutine
  new-thread.md             # new Thread → Executor
  activity-result.md        # startActivityForResult → registerForActivityResult
  notifications.md          # PendingIntent flags, notification channels
  ble.md                    # (filled in last)
```

Each note contains:
- What is being replaced
- What it is replaced with
- A minimal code example from the pattern PR

---

## BLE / Bluetooth

BLE is the core functionality of xDrip and is medical-critical code. It is included in this initiative but tackled last (category 7), after all other patterns are established and the team has built confidence in the process. The same rules apply: characterization tests first, no functional changes.

---

## Branch and PR naming

| Type | Branch | PR title |
|---|---|---|
| Pattern | `cleanup/<category>-pattern` | `[pattern] Replace <deprecated> with <modern> in <ClassName>` |
| Cleanup | `cleanup/<category>-<ClassName>` | `[cleanup] Replace <deprecated> in <ClassName>` |
