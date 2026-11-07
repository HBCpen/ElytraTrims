# Elytra Trims Cosmetic

A client-side fork of [Elytra Trims](https://modrinth.com/mod/elytra-trims) by KikuGie
([source](https://codeberg.org/KikuGie/elytra-trims)).

Customize how your elytra looks — armor trims, dye colors, banner/shield patterns and the glow, cosmic and
rainbow effects — as a pure cosmetic:

- **Client-side only.** Works on any server, including vanilla ones. Nothing is sent to the server.
- **No materials needed.** Pick any look in an editor instead of crafting with smithing templates, dyes or banners.
- **Not limited by the server.** Every trim pattern with a wings texture can be used, even if the server doesn't know it.

The cosmetic is applied to whatever elytra you are wearing and is only visible to you (third-person view, inventory,
etc.). Showing the cosmetics of other players who use this mod is planned, but not implemented yet.

## Usage

Press <kbd>K</kbd> in-game (rebindable under *Controls → Elytra Trims*) to open the editor.

- **Trim / Material**: any armor trim pattern and material.
- **Dye**: click to cycle through dye colors (Shift+click to go back) or type a hex color such as `#3355FF`.
- **Effects**: rainbow, glow in the dark, cosmic (end portal) and an animation easter egg.
- **Patterns**: *Edit Patterns* opens a loom-like editor. Pick a color, then click a pattern to stack a new layer,
  or select a layer in the list to recolor, replace, move or remove it. Layers can also be copied from a banner or
  shield in your hand. *Style* switches between the shield and the high-resolution banner textures.
- Drag the preview to rotate and tilt it, and switch its pose between standing, sneaking and gliding.
  *Done* or <kbd>Esc</kbd> saves, *Cancel* discards the changes.

Settings are stored in `config/elytratrims_cosmetic.json`.

Elytras that are really decorated (for example on a server running the original Elytra Trims) are still rendered as-is
for other players.

## Requirements

- Minecraft 26.3 with Fabric Loader
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)

This mod replaces the original Elytra Trims and can't be installed together with it.
Resource packs made for Elytra Trims keep working.

## Building

```sh
./gradlew :26.3-fabric:build
```

The jar is written to `versions/26.3-fabric/build/libs/`.
Run `./gradlew :26.3-fabric:runClientGameTest` to run the client game tests.

## License

[LGPL-3.0](LICENSE), same as the original mod. The LGPL supplements the [GPL-3.0](COPYING), whose text is included as well.
