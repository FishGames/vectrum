# Vectrum

<!-- TODO(docs): Personal intro in your own words: why this mod exists and what makes it different (no storage network, no channel limits). A screenshot or short GIF of a working build directly below the title. -->

Logistics for Minecraft without a storage system: move **items, fluids, energy, redstone signals** and, with Mekanism,
**gases** and **heat**, with Ars Nouveau **source**, between your machines and chests with cables. Lay cables, set what goes in and what goes out, done.

Vectrum runs on **Fabric**, **Forge** and **NeoForge** (Minecraft 1.20.1) and needs no other mod. Texts are available
in English and German.

<!-- TODO(docs): Screenshots of the four connection types next to each other. Keep the list short and only name things that are visible in the game. -->

## Features

- Four ways to connect things: single-type cables, one universal cable, a digital network, and wireless ports. Every
  one of them can be crafted at any time and has its own strengths and drawbacks.
- Optional modules switch on when the other mod is installed: **gas cable** and **heat cable** with Mekanism, **source
  cable** with Ars Nouveau. The universal cable, the coder and the wireless port carry these types as well.
- One port screen for everything: role, filter, priority, distribution mode, live throughput.
- Filters by drag and drop from JEI or EMI.
- Upgrades instead of channel limits. There is no range limit, no distance cost, and nothing ever stops working
  because a quota ran out.
- A diagnostic tool and tooltips (Jade, WTHIT, The One Probe) that tell you *why* something does not flow.

<!-- TODO(docs): Download links (Modrinth / CurseForge / GitHub releases), tested loader versions, and a note about the modpack policy. The Mekanism and recipe viewer notes could move into a short 'Compatibility' section that says honestly what was tested (Mekanism yes; other mods not). -->

## Installation

| Loader | You need |
| --- | --- |
| Fabric | Fabric Loader 0.19.5 or newer and Fabric API |
| Forge | Forge 47 or newer |
| NeoForge | NeoForge for Minecraft 1.20.1 |

Put the jar of your loader into the `mods` folder. Optional: JEI or EMI (filter by drag and drop), Jade, WTHIT or
The One Probe (tooltips; The One Probe on Forge/NeoForge only), Mekanism (gases and heat), Ars Nouveau (source); the last two on Forge/NeoForge only.

<!-- TODO(docs): A screenshot or GIF of the first chest-to-chest line, and a sentence about the most common first mistake (the chest that should receive must be set to push). -->

## Getting started

1. Craft a few **item cables** and lay them from one chest to another chest.
2. A cable side that touches an inventory becomes a **port**. By default a port is a **pull**:
   the cable takes goods out of the inventory (the side shows a plate).
3. Right click the cable side at the *target* chest with the **wrench** until it says **push** (the cable puts items into the chest, the plate disappears).
   Items now flow from every pull of the network into that chest.
4. Right click a cable with an **empty hand** to open the port screen for that side (see below).

Fluid cables work the same way. The energy cable has no roles: every side that touches an energy storage both gives
and takes energy, like Mekanism's cables, so connecting it to a source and a consumer is enough. The wrench only
switches a side on or off. Storages that can give and take (batteries) are balanced towards the same fill level.

Two cables of the same type that touch each other always link into one network. To run two lines of the same type
directly next to each other without merging them, right click the touching side with the wrench: it severs the link.
Right click the same side again to reconnect it.

<!-- TODO(docs): The honest comparison in your own words, ideally with two or three example builds (for instance a farm output, a machine room with a universal cable, a base connected by coders, a wireless link between dimensions). Check that every 'Keep in mind' entry is still true after your own playtest. -->

## Which cable for what?

| Block | Good for | Keep in mind |
| --- | --- | --- |
| Item, fluid, energy and redstone cable | Simple, cheap lines for one job | One line per type; different types never mix |
| Universal cable | Items, fluids and energy (and gas, heat, source when their modules are on) in one line, saves space | Costs the three single cables plus gold; each type still forms its own network |
| Digital cable and coder | Joins separate cable networks; the digital cable has no limit and needs no upgrades | Needs a coder at every connection point; does not cross dimensions |
| Wireless port | No cable at all, also across dimensions (with an upgrade) | Expensive; both ports must be in loaded chunks and set to send or receive |

<!-- TODO(docs): Recipe pictures or a link to JEI/EMI instead of the text table. Re-check the amounts after playtesting, the balancing is not final. -->

## Crafting

| Item | Recipe |
| --- | --- |
| Item cable (6) | iron ingot, copper ingot, iron ingot in a row |
| Fluid cable (6) | iron ingot, glass, iron ingot in a row |
| Energy cable (6) | copper ingot, redstone, copper ingot in a row |
| Redstone cable (6) | redstone, iron ingot, redstone in a row |
| Gas cable (2) | iron ingot, fluid cable, iron ingot in a row (only with Mekanism) |
| Heat cable (2) | copper ingot, energy cable, copper ingot in a row (only with Mekanism) |
| Source cable (2) | source gem, energy cable, source gem in a row (only with Ars Nouveau) |
| Universal cable | item cable + fluid cable + energy cable + gold ingot (shapeless) |
| Digital cable (4) | quartz, amethyst shard, quartz in a row |
| Coder | universal cable in the centre, gold ingots in the corners, diamonds above and below it, ender pearls left and right of it |
| Wireless port (2) | nether star in the centre, gold ingots in the corners, eyes of ender above and below it, diamonds left and right of it |
| Wrench | 3 iron ingots and 1 copper ingot in a wrench shape (iron in the top corners and at the bottom, copper in the centre) |
| Diagnostic tool | glass pane, copper ingot, iron ingot from top to bottom |
| Item endpoint | hopper + item cable + iron ingot (shapeless) |

All recipes are also shown in JEI and EMI.

<!-- TODO(docs): An annotated screenshot of the screen (role, filter slots, priority, throughput line) and a short note on the ghost slots. -->

## The port screen

Right click a cable side, a coder or a wireless port with an empty hand. The screen shows the settings of the side you
clicked; the arrows at the top switch to the next side.

- **Name**: an optional name for the port; empty shows the name of the block. It is only used in this screen and is not copied by copy and paste.
- **Role**: pull (the cable takes out of the block, shown by a plate), push (the cable puts into the block, plain arm), or off. The energy cable cycles push & pull (default: gives and takes, depending on what the block allows), pull (only takes) and off. The digital cable has no roles.
- **Distribution** (pull sides): sequential, round robin, or balanced.
- **Redstone**: ignore the signal, work only with a signal, or only without one. The signal is the one that reaches the block of the port. Not available on wireless ports.
- **Priority**: type a number; ports with a higher number are supplied first. Needs the priority upgrade.
- **Filter**: whitelist or blacklist. Needs the filter upgrade. Drag an item or fluid from JEI or EMI onto a filter
  slot, or click a slot with the item in your hand. A bucket adds the fluid.
- **Upgrade slots**: one slot per upgrade; put upgrades in and take them out by hand. They belong to the side you are looking at.
- **Statistics**: the live flow and the current limit, and how many sources and targets the network has.
- **Copy and paste** (the two icons) the settings of one side to another.
- **Gear**: opens the advanced settings page. It is empty for now; some of the settings above will move there.

Options whose upgrade is missing are not shown. The digital cable has no screen. The coder screen sets the frequency,
the wireless port screen the frequency and whether the port sends, receives, both, or nothing, per type.

<!-- TODO(docs): A table with the real numbers per level (items per second for 0 to 6 throughput upgrades, ticks between transfers for 0 to 3 speed upgrades), and a sentence on which upgrade is worth crafting first. -->

## Upgrades

Upgrades belong to a cable side, so different machines on one cable can be tuned separately. Put them into the slots
of the port screen, or right click a cable side with an upgrade to install it. Sneak and right click with the wrench
takes all upgrades of that side out again; they also drop when the block is broken. A wireless port has one set for
the whole block.

| Upgrade | Effect | Maximum |
| --- | --- | --- |
| Throughput | Four times the amount per transfer | 6 |
| Speed | Halves the time between transfers | 3 |
| Types | One more item type per transfer | 4 |
| Filter | Unlocks the filter | 1 |
| Priority | Unlocks the priority | 1 |
| Dimension | Lets a wireless port link across dimensions | 1 |

Without upgrades a port moves 4 items, 1000 mB of fluid or 2000 FE every half second. The numbers in this table and
in the sentence are the defaults of the [configuration](#configuration). Every upgrade is crafted as a ring: four
ingots in the corners, four redstone at the sides, and a marker item in the centre.

| Upgrade | Corners | Centre |
| --- | --- | --- |
| Throughput | iron ingot | hopper |
| Speed | copper ingot | clock |
| Types | iron ingot | chest |
| Filter | copper ingot | paper |
| Priority | copper ingot | gold ingot |
| Dimension | diamond (gold ingots instead of redstone at the sides) | eye of ender |

<!-- TODO(docs): A small example (lever to lamp over a long distance) with a screenshot. -->

## Redstone cables

A redstone cable carries a signal strength from its pulls to its pushes. The value is the strongest signal at any
pull; every push emits it. Next to a lever, redstone block or similar the side becomes a pull (the cable reads the
signal) by itself, and next to a lamp, door, piston or other block that reads a signal it becomes a push. Change a side with the wrench.

<!-- TODO(docs): A worked example for coders (two bases, one frequency) and one for wireless ports, including the chunk-loading and dimension-upgrade caveats in plain words. -->

## Digital network and wireless

- **Coders** with the same frequency in the same digital network couple their cable networks. Goods from a source then
  also reach targets in the other networks. Set the frequency in the coder screen or with the wrench (right click +1,
  sneak and right click -1).
- **Wireless ports** with the same frequency belong together. Per type they can send, receive, both, or do nothing
  (new ports only receive). They have no throughput limit. Both blocks must be in loaded chunks. Across dimensions,
  both ports need the dimension upgrade.
- A cable network can feed a wireless port and drain it, so no extra chest is needed in between.

<!-- TODO(docs): A short FAQ: nothing moves (roles, filter), only one item type moves (types upgrade), cables placed before Mekanism was installed, wireless port in an unloaded chunk. Screenshots of the diagnostic tool and of a Jade tooltip. -->

## When something does not flow

- **Diagnostic tool**: right click a cable or port. It shows the network, the roles and the reason why nothing moves
  (no source, no target, source empty, target full, filter blocks everything, limit reached, and more).
- **Jade, WTHIT, The One Probe**: look at a cable side to see the same information.
- The port screen shows the live throughput and the limit.

<!-- TODO(docs): Only if you want commands documented for players; otherwise keep it for server admins and add the missing arguments of each command. -->

## Commands

For operators (permission level 2):

| Command | Purpose |
| --- | --- |
| `/vectrum diagnose <pos> [side]` | Explains why goods do or do not flow |
| `/vectrum throughput <pos> [<value>\|reset]` | Shows or sets the throughput limit |
| `/vectrum port <pos> <side> ...` | Role, priority, mode and filter of a side |
| `/vectrum upgrade <pos> [<side>] ...` | Adds or removes upgrades of a side (cables need the side) |
| `/vectrum frequency <pos> [<value>]` | Shows or sets a coder or wireless frequency |
| `/vectrum wireless <pos> [mode <type> <mode>]` | Link mode of a wireless port |

## Configuration

`config/vectrum.toml` is written with these values on the first start and read once when the game or server starts.
Server and clients should use the same file; the port screen shows the maximums of the local file.

```toml
[amount]
item = 4
fluid = 1000
energy = 2000
gas = 1000
source = 1000
heat = 10000

[upgrades]
throughput = [1, 4, 16, 64, 256, 1024, 4096]
speed = [10, 5, 2, 1]
item_types = [1, 2, 3, 4, 5]

[wireless]
interval = 10

[redstone]
interval = 20

[performance]
idle_interval_max = 40
target_pause = [20, 40, 80, 100]
time_budget_micros = 10000
run_budget_micros = 2000
refresh_interval = 100
```

| Entry | Meaning |
| --- | --- |
| `amount` | Units per transfer and source side without upgrades: items, millibuckets, FE, millibuckets of gas, source, heat units |
| `throughput` | Factor on the amount; entry *n* applies with *n* throughput upgrades |
| `speed` | Ticks between two transfers; entry *n* applies with *n* speed upgrades |
| `item_types` | Item types per transfer and target; entry *n* applies with *n* type upgrades |
| `wireless.interval`, `redstone.interval` | Ticks between two runs of a wireless port or a redstone cable |
| `idle_interval_max` | Longest wait in ticks of a source that has moved nothing for more than 30 ticks; 0 turns the waiting off |
| `target_pause` | Ticks a target is skipped after it refused goods, by number of refusals in a row; the last entry repeats |
| `time_budget_micros` | Microseconds per server tick all transfers may use together; work over the limit waits for the next tick; 0 means no limit |
| `run_budget_micros` | Microseconds one run of one cable may use; a cable that would take longer moves the rest in its next run; 0 means no limit |
| `refresh_interval` | Ticks between two checks of a cable's connections; changes next to the cable are noticed at once |

The number of entries of a list minus one is the maximum number of that upgrade (at most 63). Entries must be whole
numbers; wrong entries are reported in the log and replaced by the defaults.

## Building from source

`./gradlew build` builds all loaders (jars in `versions/<version>-<loader>/build/libs`). More in
[`docs/development.md`](docs/development.md).

<!-- TODO(docs): Credits section: textures and models, the libraries and mods that are named, the licence, and where to report bugs. Your note that the mod was made with AI support belongs here or on the mod page, whichever you prefer. -->

## Bundled software

- Fabric jar: [Team Reborn Energy](https://github.com/TechReborn/Energy) 3.0.0 (MIT licence), bundled as jar-in-jar for energy transfer.

## Known gaps

- No mixins are included yet. To add them: create `vectrum.mixins.json`, list it under `mixins` in `fabric.mod.json`,
  and for Forge/NeoForge set `loom { forge { mixinConfig("vectrum.mixins.json") } }` in `build.gradle.kts`.
- Licence: `All Rights Reserved` (see `LICENSE`, same entry in the mod metadata).
