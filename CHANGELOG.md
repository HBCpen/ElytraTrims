## Features
- Added "Blooming" and "Forge" trims based on Eternal Starlight mod.

## Changes
- Updated to 26.3-snapshot-3.

## API
- `ETRenderParameters` are now mutable, with `ETRenderingAPI.wrapRenderParameters` now accepting a `Consumer<ETRenderParameters>`.
  > Decorators now make a shallow copy of the parameters before each render call, submitting it to registered wrappers.
  > Old API has been marked as deprecated for this release.
- Removed item and interaction APIs.
