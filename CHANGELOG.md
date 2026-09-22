# Changelog

All notable changes to Vectrum. Newest first.

## Unreleased

<!-- TODO(docs): Before the first release, rewrite this section as release notes for 0.1.0 in a few sentences of prose (what the mod does, what is new, known problems) and add the release date. The bullet list below can stay as the detailed part. -->

### Added
- Cable severing: right click a linked cable side with the wrench to sever it from its neighbour, so two lines of the
  same type can run directly next to each other without merging into one network. Right click the same side again to
  reconnect it. Works on every cable and the digital cable; the coder is unaffected (its wrench click sets the
  frequency instead).

- Port GUI: right click on a cable side, coder or wireless port with an empty hand opens a screen for the
  clicked side. Role, distribution mode, filter (whitelist/blacklist, ghost slots), priority, frequency, wireless link
  mode per type, copy and paste of settings; live throughput and the current limit. Options whose upgrade is missing
  are not shown. The digital cable has no GUI. Held items keep their old behaviour (wrench, upgrades, diagnostic tool).
- Diagnosis: 13 reasons why goods do or do not flow (no source, no target, source empty, target full,
  filter blocks everything, target unloaded, dimension locked, limit reached ...). Shown by the diagnostic tool, by
  `/vectrum diagnose <pos> [side]`, in the GUI and in the tooltip mods.
- Tooltip mods: Jade and WTHIT (all loaders) and The One Probe (Forge/NeoForge) show role, side and the
  diagnosis of the looked-at cable or port.
- JEI and EMI: items and fluids can be dragged from the recipe viewer into the filter ghost slots.
- Gas module (Forge and NeoForge, with Mekanism): gas cable, plus gas in the universal cable, the coder and the wireless
  port. 1000 mB per transfer, filter with gas identifiers (`mekanism:hydrogen`), priority, modes and upgrades as with
  fluids. Without Mekanism (and on Fabric) the module stays off. Integrated through a compile-only Mekanism API
  dependency. Tested with real Mekanism 10.4.16.80 on a Forge and a NeoForge server. Recipe: 2 iron ingots + 1 fluid
  cable yield 2 gas cables. Universal cables and coders placed before Mekanism was installed have to be placed once more.
- Dimension upgrade for the wireless port (max. 1 per port). A wireless link between different dimensions works only
  if both the sending and the receiving port carry it; links within one dimension need no upgrade. Right click
  installs it, sneak + right click with the item removes it, breaking the port returns it; also
  `/vectrum upgrade <pos> add|remove dimension`. Receivers in unloaded chunks are skipped.
- Wireless port: a block directly at the storage (cables optional). All blocks with the same
  frequency belong together. Per type (items, fluids, energy) it can be set whether the block sends, receives, both
  or nothing (default: receive). No throughput limit, no operating cost (expensive recipe); filter, priority and
  distribution mode per side without an upgrade. Wrench: right click cycles the mode, sneak + right click raises the
  frequency. Command `/vectrum wireless <pos> [mode <type> <mode>]`.
- Digital cable and coder: coders with the same frequency in the same digital network couple their
  transport networks; goods from a source thereby also reach targets in other networks (items, fluids, energy).
  Frequencies are unbounded integers; wrench: right click +1, sneak + right click -1. Command
  `/vectrum frequency <pos> [<value>]`. No relaying across several coders. The diagnostic tool shows the digital
  network, the frequency and the coupling.
- Redstone cable: transmits signal strengths (0-15) between inputs and outputs of a network. The value is
  the greatest signal strength at an input; every output emits it. Next to levers, redstone blocks, dust etc. a side
  becomes an input by itself; an output is set with the wrench (or `/vectrum port <pos> <side> role in|out|off`).
  Reacts immediately to changes, no constant polling. Recipe: 2 redstone + 1 iron ingot yield 6 cables. The
  diagnostic tool shows the signal strength.
- Universal cable: one cable that carries items, fluids and energy at the same time. Each type forms
  its own network within it; the types do not mix. Recipe: 1 item, 1 fluid and 1 energy cable + 1 gold ingot yield
  2 universal cables. Filters only act on the type for which they have entries. The throughput limit exists per type
  (`/vectrum throughput <pos> type <item|fluid|energy>`).
- Upgrade system: five upgrade items (throughput, speed, types, filter, priority). They are inserted into a
  cable port with a right click, removed again with wrench + sneak + right click, and drop when the block is broken.
  Throughput quadruples the limit per upgrade, speed halves the interval, types allow one more item type per
  transfer, filter and priority unlock the settings of the same name.
- Command `/vectrum upgrade <pos> [add|remove <kind> [n]|clear]` (operators only).
- Filter, priority and distribution modes: every port side has a priority (higher number is supplied first),
  a distribution mode (sequential, round robin, balanced) and a filter (whitelist or blacklist for items and fluids).
  Configurable with `/vectrum port <pos> <side> ...` (operators only, until upgrades and interface follow); the
  settings are saved with the world. Full targets are asked less often for a while.
- Vanilla cauldrons can be filled and emptied with fluid cables on Forge and NeoForge (full bucket), as on Fabric.
- Fluids and energy: fluid cable and energy cable that work like the item cable (lay cables, switch ends with
  the wrench). Base limits: 1000 mB and 2000 FE per transfer and source side. On Fabric, Team Reborn Energy (MIT) is
  bundled.
- Throughput limit: every cable port has a stored limit (base value 4 items per transfer and source side).
  It is only looked up on transfer, never computed across the network, and is saved with the world.
- Command `/vectrum throughput <pos> [<value>|reset]` (operators only) to show and set the limit.
- The diagnostic tool shows the throughput limit of the clicked block.
- Recipe advancements (result of the first real datagen run).

### Changed
- Item, fluid, energy, redstone and gas cables are thinner (6 px instead of 10 px), matching the digital cable. To
  keep them easy to click, the invisible target used for opening the port screen and the wrench widens back to 10 px
  while holding the wrench, a cable, a coder or a wireless port; an empty hand or anything else clicks past the
  cable at its real (thinner) size. The universal cable stays at the old fixed 10 px.
- Recipes rebalanced: the universal cable yields 1 cable per craft (1 item, 1 fluid, 1 energy cable + 1 gold
  ingot). The upgrades are now ring recipes: metal in the corners, redstone at the sides, marker item in the centre
  (throughput: iron + hopper, speed: copper + clock, types: iron + chest, filter: copper + paper, priority: copper +
  gold ingot, dimension: 4 diamonds + 4 gold ingots + eye of ender).
- Datagen generates all cable, coder and wireless port models and blockstates; new item tags `vectrum:cables` and
  `vectrum:upgrades`.
- Digital cable is 6 px thick in model, selection box and collision box (it could not be mined at its visible edges).
- The coder is a full block.
- The wireless port connects to cables and coders. A sending port passes the goods of its cable network to all
  receivers of its frequency; a receiving port offers the targets of its cable network to all senders. No relaying
  across several ports. Wireless ports placed earlier link after the next neighbour update (or re-place them).
- Redstone cables connect by default to blocks that read redstone signals (lamps, doors, trapdoors, pistons, rails,
  dispensers, hoppers, note blocks, bells, repeaters, comparators, TNT, command blocks); the wrench and
  `/vectrum port <pos> <side> role in|out|off` still override.
- The role of a cable side is now "not chosen" until the player switches it (quantity types: still output,
  redstone: input next to signal sources). Existing worlds keep their roles.
- Fix: `/vectrum throughput <pos> <value>` did not work (only `... reset <value>`).
- The diagnostic tool and the wrench know the new blocks (coder, digital cable, wireless port).
- Without a types upgrade, only one item type is moved per transfer to a target.
- Filter and priority only act with the matching upgrade; `/vectrum port` refuses to set them without it.
- Cable ends take over the function of the former endpoints; an item cable alone is enough for basic setups.
- Base throughput lowered to 4 items every 0.5 seconds.
- The diagnostic tool is its own item (previously part of the wrench).
- Cable blockstate changed: worlds from earlier test builds are not compatible.

### Removed
- The template example item and example block.

### First skeleton
- Project for Fabric, Forge and NeoForge (Minecraft 1.20.1), network core with tests, item cable with chest-to-chest
  transport, wrench, recipes, datagen, language files (DE and EN).
