# Changelog

All notable changes to Vectrum. Newest first.

## Unreleased

### Added
- Gas module (Forge and NeoForge, with Mekanism): gas cable, plus gas in the universal cable, the coder and the wireless
  port. 1000 mB per transfer, filter with gas identifiers (`mekanism:hydrogen`), priority, modes and upgrades as with
  fluids. Without Mekanism (and on Fabric) the module stays off. Integrated through a compile-only Mekanism API
  dependency. Tested with real Mekanism 10.4.16.80 on a Forge and a NeoForge server. Recipe: 2 iron ingots + 1 fluid
  cable yield 2 gas cables. Universal cables and coders placed before Mekanism was installed have to be placed once more.
- Dimension upgrade for the wireless port (max. 1 per port). A wireless link between different dimensions works only
  if both the sending and the receiving port carry it; links within one dimension need no upgrade. Right click
  installs it, sneak + right click with the item removes it, breaking the port returns it; also
  `/vectrum upgrade <pos> add|remove dimension`. Receivers in unloaded chunks are skipped.
- Wireless port (stage 10, tier 4): a block directly at the storage (cables optional). All blocks with the same
  frequency belong together. Per type (items, fluids, energy) it can be set whether the block sends, receives, both
  or nothing (default: receive). No throughput limit, no operating cost (expensive recipe); filter, priority and
  distribution mode per side without an upgrade. Wrench: right click cycles the mode, sneak + right click raises the
  frequency. Command `/vectrum wireless <pos> [mode <type> <mode>]`.
- Digital cable and coder (stage 9, tier 3): coders with the same frequency in the same digital network couple their
  transport networks; goods from a source thereby also reach targets in other networks (items, fluids, energy).
  Frequencies are unbounded integers; wrench: right click +1, sneak + right click -1. Command
  `/vectrum frequency <pos> [<value>]`. No relaying across several coders. The diagnostic tool shows the digital
  network, the frequency and the coupling.
- Redstone cable (stage 8): transmits signal strengths (0-15) between inputs and outputs of a network. The value is
  the greatest signal strength at an input; every output emits it. Next to levers, redstone blocks, dust etc. a side
  becomes an input by itself; an output is set with the wrench (or `/vectrum port <pos> <side> role in|out|off`).
  Reacts immediately to changes, no constant polling. Recipe: 2 redstone + 1 iron ingot yield 6 cables. The
  diagnostic tool shows the signal strength.
- Universal cable (stage 7, tier 2): one cable that carries items, fluids and energy at the same time. Each type forms
  its own network within it; the types do not mix. Recipe: 1 item, 1 fluid and 1 energy cable + 1 gold ingot yield
  2 universal cables. Filters only act on the type for which they have entries. The throughput limit exists per type
  (`/vectrum throughput <pos> type <item|fluid|energy>`).
- Upgrade system (stage 6): five upgrade items (throughput, speed, types, filter, priority). They are inserted into a
  cable port with a right click, removed again with wrench + sneak + right click, and drop when the block is broken.
  Throughput quadruples the limit per upgrade, speed halves the interval, types allow one more item type per
  transfer, filter and priority unlock the settings of the same name.
- Command `/vectrum upgrade <pos> [add|remove <kind> [n]|clear]` (operators only).
- Filter, priority and distribution modes (stage 5): every port side has a priority (higher number is supplied first),
  a distribution mode (sequential, round robin, balanced) and a filter (whitelist or blacklist for items and fluids).
  Configurable with `/vectrum port <pos> <side> ...` (operators only, until upgrades and interface follow); the
  settings are saved with the world. Full targets are asked less often for a while.
- Vanilla cauldrons can be filled and emptied with fluid cables on Forge and NeoForge (full bucket), as on Fabric.
- Fluids and energy (stage 4): fluid cable and energy cable that work like the item cable (lay cables, switch ends with
  the wrench). Base limits: 1000 mB and 2000 FE per transfer and source side. On Fabric, Team Reborn Energy (MIT) is
  bundled.
- Throughput limit (stage 3): every cable port has a stored limit (base value 4 items per transfer and source side).
  It is only looked up on transfer, never computed across the network, and is saved with the world.
- Command `/vectrum throughput <pos> [<value>|reset]` (operators only) to show and set the limit.
- The diagnostic tool shows the throughput limit of the clicked block.
- Recipe advancements (result of the first real datagen run).

### Changed
- Digital cable is 6 px thick in model, selection box and collision box (it could not be mined at its visible edges).
- The coder is a full block.
- The wireless port connects to cables and coders. A sending port passes the goods of its cable network to all
  receivers of its frequency; a receiving port offers the targets of its cable network to all senders. No relaying
  across several ports. Wireless ports placed earlier link after the next neighbour update (or re-place them).
- Redstone cables connect by default to blocks that read redstone signals (lamps, doors, trapdoors, pistons, rails,
  dispensers, hoppers, note blocks, bells, repeaters, comparators, TNT, command blocks); the wrench and
  `/vectrum port <pos> <side> role in|out|off` still override.
- Texts and comments switched to English (code comments, log and exception messages, README, CHANGELOG, decision log
  `docs/decisions.md`).
- The role of a cable side is now "not chosen" until the player switches it (quantity types: still output,
  redstone: input next to signal sources). Existing worlds keep their roles.
- Fix: `/vectrum throughput <pos> <value>` had not worked since stage 7 (only `... reset <value>`).
- The diagnostic tool and the wrench know the new blocks (coder, digital cable, wireless port).
- Without a types upgrade, only one item type is moved per transfer to a target.
- Filter and priority only act with the matching upgrade; `/vectrum port` refuses to set them without it.
- `Port.moveTo` now accepts a filter and `Port` knows the fill level; `ItemPort` no longer has its own filter method.
- Transport is now the same code for all types (`Transport`, `Port`); the old item classes are gone.
- Cable ends take over the function of the former endpoints; an item cable suffices for tiers 1 and 2.
- Base throughput lowered to 4 items every 0.5 seconds.
- The diagnostic tool is its own item (previously part of the wrench).
- Cable blockstate changed: worlds from earlier test builds are not compatible.

### First skeleton
- Project for Fabric, Forge and NeoForge (Minecraft 1.20.1), network core with tests, item cable with chest-to-chest
  transport, wrench, recipes, datagen, language files (DE and EN).
