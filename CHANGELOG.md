## 0.2.0
First public release of the client-side fork, based on Elytra Trims 4.9.1 for 26.3.
Earlier builds numbered 0.1.0 were only shared for testing.

- Elytra decorations are now client-side cosmetics, chosen in an in-game editor (default key: K):
  any armor trim pattern and material, dye colors or a hex color, banner/shield patterns and the rainbow and glow
  effects. No items are consumed and the server doesn't need the mod.
- Every trim pattern with a wings texture can be used, even if the server doesn't know it.
- Loom-like pattern editor: add, recolor, move and remove layers, or copy them from a banner or shield in your hand.
- Saved designs: save a look under a name, load it later, and copy it to or paste it from the clipboard.
- Elytra visibility: draw the elytra only while worn, also on top of a chestplate, or always.
- Preview that can be rotated and tilted, in standing, sneaking and gliding poses.
- With LambDynamicLights installed, glowing elytras faintly light up their surroundings.
- Syncing with other players of this mod through a relay server. Cosmetics are signed with the player's Mojang chat
  signing key and verified by every client. Choose between sharing, only showing others, or turning syncing off.
- Removed recipes, cauldron interactions and all other server-side features. The cosmic and animation effects can't
  be chosen as cosmetics, but are still drawn on elytras really decorated with them.
- Can't be installed together with the original Elytra Trims. Resource packs made for it keep working.
