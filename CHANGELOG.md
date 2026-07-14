## Features
- Added "Blooming" and "Forge" trims based on Eternal Starlight mod.

## Changes
- Updated to 26.3-snapshot-3.

## Internals
- Removed the Elytra Trims public API module.
  > It was overcomplicated and bottlenecked updates to newer versions.
  > If you need to modify the mod's behaviour - just use mixins.
