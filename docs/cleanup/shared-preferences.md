# Migration: android.preference.PreferenceManager → androidx.preference.PreferenceManager

## What is deprecated

`android.preference.PreferenceManager` is deprecated in API 29. The entire
`android.preference.*` package was replaced by `androidx.preference.*`.

## Replacement (PreferenceManager only)

This is a **pure import swap**. The API is identical.

| Before | After |
|--------|-------|
| `import android.preference.PreferenceManager;` | `import androidx.preference.PreferenceManager;` |

## Dependency

`androidx.preference:preference:1.0.0` is already in `app/build.gradle`. No changes needed.

## Example (from Agreement.java pattern PR)

Before:

    import android.preference.PreferenceManager;

After:

    import androidx.preference.PreferenceManager;

Call sites are unchanged:

    PreferenceManager.getDefaultSharedPreferences(context)

## Test isolation note

When writing characterization tests for classes that use `PreferenceManager.getDefaultSharedPreferences()`,
add a `setUp()` override that calls `xdrip.setContextAlways(RuntimeEnvironment.application)` after
`super.setUp()`. This forces `xdrip.context` to re-bind to the current Robolectric app instance,
preventing stale context references across test methods.
