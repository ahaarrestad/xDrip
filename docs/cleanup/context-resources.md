# Migration: resources.getColor / resources.getDrawable

## What is deprecated

`Resources.getColor(int)` and `Resources.getDrawable(int)` (without a `Theme` parameter)
are deprecated since API 23.

## Replacement

Use `ContextCompat` from `androidx.core.content`:

| Before | After |
|--------|-------|
| `resources.getColor(R.color.X)` | `ContextCompat.getColor(context, R.color.X)` |
| `resources.getDrawable(R.drawable.X)` | `ContextCompat.getDrawable(context, R.drawable.X)` |

Inside a `View` subclass, use `getContext()` as the context argument.
In an `Activity` or `Service`, use `this` or `getApplicationContext()`.

## Import

    import androidx.core.content.ContextCompat;

## API compatibility

`ContextCompat` is part of `androidx.core` and works on `minSdkVersion 24` (and lower).
No version guard needed.

## Example (from PercentileView.java pattern PR)

Before:

    outerPaint.setColor(resources.getColor(R.color.percentile_outer));

After:

    outerPaint.setColor(ContextCompat.getColor(getContext(), R.color.percentile_outer));
