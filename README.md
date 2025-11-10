<div style="text-align: center">
<img alt="logo" width="256" src="https://cdn.modrinth.com/data/XpzGz7KD/images/1d33b802dfb5a1b2d25a2490fe1a9df0a681d686.png">

<h1>Elytra Trims</h1>
<p>
<b>With this mod, you can add armor trims, dye your elytra in any color, and put banner patterns on them.</b>
</p>
<a href="https://modrinth.com/mod/elytra-trims">
<img alt="modrinth" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg">
</a>

<a href="https://www.curseforge.com/minecraft/mc-mods/elytra-trims">
<img alt="curseforge" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_vector.svg">
</a>

<a href="https://codeberg.org/KikuGie/elytra-trims">
<img alt="codeberg" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/codeberg_vector.svg">
</a>

<a href="https://discord.com/invite/TBgNUCfryS">
<img alt="discord-singular" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy-minimal/social/discord-singular_vector.svg">
</a>

<a href="https://ko-fi.com/kikugie">
<img alt="kofi-singular" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy-minimal/donate/kofi-singular_vector.svg">
</a>

<br>

<img alt="fabric" height="40" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/fabric_vector.svg">
<img alt="forge" height="40" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/forge_vector.svg">

<hr>
</div>

## Installation
> [!IMPORTANT]  
> Mod functionality and compatibility may be different across Minecraft updates!

### Dependencies
Elytra Trims requires [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) 
or [Kotlin Lang Forge](https://modrinth.com/mod/kotlin-lang-forge) *(alternatively [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge))*
on the respective mod loader.  
On 1.20 and 1.21 you may also need [YACL](https://modrinth.com/mod/yacl) and [ModMenu](https://modrinth.com/mod/modmenu) *(Fabric only)*.

### Prerequisites
In multiplayer, Elytra Trims **must be installed on the server**, however this **doesn't prevent players without the mod from joining.**

| *Elytra Trims = ET*    | Player doesn't have ET                                 | Player has ET                                                      |
|------------------------|--------------------------------------------------------|--------------------------------------------------------------------|
| Server doesn't have ET | Vanilla experience.                                    | Player can join, but can't apply trims, patterns, etc. to elytras. |
| Server has ET          | Player can join, but doesn't see trims, patterns, etc. | Player can craft and see decorated elytras.                        |
|                        |                                                        |                                                                    |

## Features
### Trims
Trims can be applied to elytras similar to the armor. By default, the mod provides textures for a all vanilla trims, and some custom ones!  
**Note**: on 1.20 and 1.21 you may need to install [Elytra Trims Extensions](https://modrinth.com/mod/elytra-trims-extensions) for trim compat.  
![A few trims](https://cdn.modrinth.com/data/XpzGz7KD/images/2966103eb5efb4e408088dbd502da364babf636d.png)

Additionally, you can replace the trim pattern in the recipe with a trial key to get access to even more elytras.  
**Note**: this feature is currently only available on 1.21.10 and newer.  
![All the trims](https://i.imgur.com/Wkt5nSa.jpeg)

### Banner patterns
Banners can be applied to elytras to show the patterns.
In the smithing table, combining elytra with a banner and either paper or leather will transfer
banner or shield patterns respectively.  
**Note**: the patterns are applied **without the base** - you need to dye the elytra to add it.  
![Elytra with banner patterns](https://cdn.modrinth.com/data/XpzGz7KD/images/0327378aa65018c5f4e761c09c14fa94bb7e1d8d.png)

### Dyeing
If you fancy something simplistic, dye your elytra however you want in a crafting table.  
![Dyed elytra](https://cdn.modrinth.com/data/XpzGz7KD/images/511e985762a518992f2bc140b34e9f5c104ade74.png)

### Glowing
Combine an elytra with a glow ink sac in the crafting table to make it glow!
![Glowing trim](https://cdn.modrinth.com/data/XpzGz7KD/images/849d54736e4519df78678bf00a2077a528e866ad.png)

### Cosmic effect
An expensive endeavor, but if you're willing to part with a nether star, you can apply a unique effect to your elytra.
![Cosmic elytra](https://i.imgur.com/mlISKvX.png)

### Easter eggs
Elytra Trims has a couple secret recipes and interactions, which are best discovered by accident, so keep your eyes out!

## Recipes
You can find the full information about Elytra Trims recipes on the [wiki](https://codeberg.org/KikuGie/elytra-trims/wiki).

## FAQ
### Are there any conflicts with other mods?
- Customizable Elytra: overrides elytra renderer entirely.
- Kube JS: breaks elytra decoration recipes.
- Lunar/Feather Client: breaks elytra rendering.

### I can't put trims/banners/dyes on the elytra!
Please check the following criteria:
- Elytra Trims is installed on the server with the correct mod loader.
- There aren't any conflicting mods installed.
- Recipes may differ across the updates, check the mod changelog page.

If nothing helps, please make an [issue report](https://codeberg.org/KikuGie/elytra-trims/issues/new?template=.github%2fISSUE_TEMPLATE%2fbug_report.yml).

### I can't see the trims!
Please check the following criteria:
- You have Elytra Trims installed on the client with the correct mod loader.
- There aren't any conflicting mods installed.

If it's a trim provided by a mod or a datapack, it will need custom textures for the elytra.
Please make a [trim request](https://codeberg.org/KikuGie/elytra-trims/issues/new?template=.github%2fISSUE_TEMPLATE%2ftrim_request.yml),
but be patient, as it takes creative work to make good trim designs.

If it's a non-vanilla elytra, it may need explicit support from Elytra Trims.
In this or other cases, please make an [issue report](https://codeberg.org/KikuGie/elytra-trims/issues/new?template=.github%2fISSUE_TEMPLATE%2fbug_report.yml).
