# Changelog

## Unreleased

- Excluded Ender Chests from observation, overlays and new history snapshots.
- Consolidated the 16 supported Minecraft targets into eight multi-version release JARs with explicit Fabric Loader version ranges.
- Added Minecraft 26.2 support with Fabric API 0.155.2+26.2 or newer.
- Fixed a crash when opening containers on Fabric API versions without `ScreenEvents.afterForeground`.
- Fixed changed-slot highlights rendering over tooltip backgrounds and text on Minecraft 26.x.
- Reworked the in-container UX into a backgroundless left-side history icon with an unobtrusive change dot.
- Changed slot highlights to opt-in and added native multi-line tooltips for added, removed and modified items.
- Added per-container snapshot browsing with real item stacks, chronological navigation and history deletion.
- Deduplicated consecutive snapshots with identical contents while still updating the last-observed time.
- Added container dimension and block coordinates to the global history opened with `H`.

## 1.0.0

- Initial ChestDiff release by sawiq_.
- Added synchronized container observation sessions and final-session baselines.
- Added semantic add/remove/modify/rearrangement detection.
- Added block, double chest, Ender storage, entity and uncertain virtual identities.
- Added in-container `Δ` indicator, colored slot highlights and full diff/history/settings screens.
- Added bounded asynchronous gzip storage with atomic writes, backups and schema migration.
- Added Russian and English translations.
- Added Stonecutter targets for Minecraft 1.21 through 1.21.11 and 26.1 through 26.2.
