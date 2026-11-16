# Changelog

All notable changes to Vectrum. Newest first.

## Unreleased

<!-- TODO(docs): Before the first release, rewrite this section as release notes for 0.1.0 in a few sentences of prose (what the mod does, what is new, known problems) and add the release date. The bullet list below can stay as the detailed part. -->

### Added
- Port screen: name of the port (only shown in the screen, not copied by paste), redstone mode per port (ignore, with
  signal, without signal; not for wireless ports), priority as a typed number, one upgrade slot per upgrade kind,
  statistics (flow per type, sources and targets of the network), copy, paste and gear icons, and an advanced page
  that is still empty. Pull, Push and Off are the role buttons.
- Upgrades belong to a cable side instead of the block: speed, throughput, types, filter and priority apply to that
  side only. A wireless port keeps one set for the block. Saved upgrades of earlier test worlds are not migrated.
- Messages from client to server for the port name and priority (all three loaders).
- Source cable (Ars Nouveau, Forge/NeoForge): moves source between source jars, relays and other source blocks. Type
  `vectrum:source`, base amount 1000 per transfer (`amount.source`); filter, priority, modes and upgrades work as for
  the other quantity types. Recipe: source gem, energy cable, source gem in a row (2 cables). Only registered when Ars
  Nouveau is loaded.
- Heat cable (Mekanism, Forge/NeoForge): moves heat from the warmer to the cooler block until both reach one
  temperature. Type `vectrum:heat`, base amount 10000 heat units per transfer (`amount.heat`). Recipe: copper ingot,
  energy cable, copper ingot in a row (2 cables). Only registered when Mekanism is loaded.
- The universal cable, the coder and the wireless port carry source and heat when their modules are on; the port
  screens list the types that are on.
- Solid colours for the cables without a texture: redstone #f60444, fluid #2b63f1, gas #00b73c, energy #79a787,
  source #8535b2, universal #443f37, digital #89fffe, heat #e89223.
- Dev runs: `-PwithArs` adds Ars Nouveau, Curios, GeckoLib, MCLib (`mclib_version`) and MixinExtras (`mixinextras_version`) to the Forge and NeoForge runtime. GeckoLib is chosen by its Modrinth version id because the number 4.8.4 is also the Fabric build.
- Configuration file `config/vectrum.toml`: amounts per transfer, effect and maximum count of the throughput, speed and
  item type upgrades (one list each, indexed by the number of installed upgrades), intervals of wireless ports and
  redstone cables, and the performance entries below. Written with the defaults on the first start; wrong entries are
  logged and replaced by the defaults.
- Item cable: where three or more arms in one plane meet at a core (T piece, cross), a box 1 px larger than the cable is
  drawn around the core, with the texture of the plate front. Other cable types do not have it yet.
- Item cable texture (test texture `item_cable.png`, 4 animation frames of 32x32 stacked vertically, 20 ticks per
  frame, colours blended between the frames): the cable is drawn as a picture seen from the front,
  every face looked at from outside and never mirrored (on the top face north is up, on the bottom face south is up).
  Arms: the arm to the right of the core shows the pixels [11,0]-[15,5] (5 px along the arm), the arm to the left
  [0,0]-[4,5]; an arm above the core is the right arm turned 90 degrees counter-clockwise, an arm below the core the left
  arm turned the same way. Cores: every core face shows one of 16 regions depending on which of its four sides (left,
  right, up, down, as seen from the front) have an arm: none, one, two (straight or corner), three or all four. Arms
  pointing towards or away from the viewer are not counted. An arm that is linked or an input shows the picture of a
  core without arms ([6,6]-[11,11]) on its end face. An output has a port plate (2 px deep, 2 px wider than the cable
  on every side): front [0,24]-[7,31] seen from outside, side faces [12,24]-[13,31] with x = 13 at the cable core.
  The back of the plate (towards the core) shows the same picture as the front. An input has no plate.
- Cable textures are now sized to the cable's real pixel thickness (6x6 for the 6 px cables, 10x10 for the universal
  cable) instead of a fixed 16x16 canvas, so painted pixels show at native size
  in-world instead of being squeezed onto a narrower face. The item cable is the exception: it is painted
  from regions of a single 32x32 texture (see below).
- Wrench interop (Forge/NeoForge): the wrench declares the common "wrench" tool action, so it now works as a real
  wrench on other mods' machines that check for it (AE2, Create, EnderIO, Mekanism, ...) just like their own
  wrenches. The reverse also works: any of their wrenches can operate Vectrum's cables, endpoints and wireless ports
  the same way Vectrum's own wrench does. Fabric has no equivalent convention and is unaffected.
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

### Fixed

- Digital cable: model and collision are 4 px thick again (E69).
- Source cable: the creative source jar can fill other jars (it always reports a full jar, so the transfer was counted as nothing, E118).
- Recipes, recipe advancements and loot tables of the optional cables (gas, heat, source) wrote parse errors to the log
  on Forge and NeoForge when the other mod was not installed. The condition key is now `conditions`, and the loot
  tables drop through an item tag whose entry is optional.

### Changed
- Port roles are called **Pull** and **Push** (E115): Pull = the cable takes out of the adjacent block (the default),
  Push = the cable puts into it. The command is `/vectrum port ... role pull|push|off` (was `in|out`); saved worlds
  keep working as they were. Redstone: a lever is pulled, a lamp pushed.
- The port plate now marks **Pull** and exists on the fluid, energy, gas, source, heat and universal cable too, in the
  colour of the cable, 1 px larger than the cable and 2 px deep (E119). Push has no plate (the green plate is gone).
  The redstone cable keeps its old look for now.
- Fluid, gas and source cables get the junction box of the item cable, in their own colour (E120).
- The selection box of cables, endpoints and coders is core plus plain arms; the port plate is no longer part of it (E121).
- The digital cable's selection box is 4 px and widens to 6 px only while a cable, wrench, coder or wireless port is held (E69).
- The energy cable (E116): default **push & pull** (gives and takes, as the blocks allow), optional **pull** (only takes)
  and off. Batteries are balanced to the same fill level (E117).
- Optional modules are managed by `Modules` (gas, source, heat); `TransportType` has the new types `SOURCE` and `HEAT`,
  and the configuration file has the entries `amount.source` and `amount.heat`.
- Performance of large networks:
  - Sources that hold nothing the filters let through no longer ask any target, and an empty source no longer puts
    targets to sleep. Sources that moved nothing for 30 ticks wait longer between runs (up to `idle_interval_max`, default 40
    ticks); goods that arrive in a waiting source are noticed after that time at most.
  - Target lists, back-offs and flow records are kept per transport type instead of globally; a change in one
    network no longer rebuilds the caches of every other network and dimension. Wireless ports clear caches in
    `neighborChanged` only when the sides with a storage changed.
  - The connections of a cable are checked every `refresh_interval` ticks (default 100) and on every neighbour
    update, not on every run.
  - All transfers of a server tick share a time budget (`time_budget_micros`, default 10 ms); work over the limit
    continues in the next tick. One run of one cable has its own limit (`run_budget_micros`, default 2 ms); a cable
    with a huge throughput moves the rest later instead of delaying the server tick and the other cables.
  - Priority groups, filters reduced to a transport type and the values derived from a block's upgrades are computed
    once and cached; the transfer allocates far less per run.
  - Blocks start with an offset derived from their position, so sources placed together do not all run in the same
    tick.
  - Round-robin pointers no longer cause a rewrite of the save data; removing a block no longer scans all settings.
  - The Forge item port reads the occupied slots once per round and remembers refused stacks while moving.
- The throughput limit of a block is derived from its base amount and its throughput upgrades when read; only limits
  set with `/vectrum throughput` are stored. Worlds from before lose the stored derived values on first load.
- After a refusal a target waits for the pause given by `target_pause` (default 20, 40, 80, 100 ticks); only targets
  that refuse goods the source actually holds are put to sleep.
- The default role of a cable side next to an inventory is now input (was output). A newly placed cable therefore
  pulls from an adjacent inventory until a side is set to output with the wrench. Sides you already set keep their
  role. Existing worlds keep their behaviour: on first load, sides of existing cables that never had a role chosen are
  fixed to output (cables placed afterwards use input). The wrench now cycles input, output, off.
- Port plates have no hitbox to run into any more: they can only be clicked (wrench, port screen), and only on
  output sides. Cables are solid on every connected side, including sides with an inventory.
- Cables no longer have an inner layer and no longer render as cutout; all cable textures are one solid layer
  (`*_inner.png` of fluid, energy, gas, redstone, universal and digital cable removed).
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
- The role of a cable side is now "not chosen" until the player switches it (quantity types: input,
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
