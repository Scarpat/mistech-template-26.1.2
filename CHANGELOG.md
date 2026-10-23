# Mistech — Changelog

## Oct 23, 2026 — Spellcraft, Crusher Forge, machine upgrades, Dream Rifts

Everything since commit `071dafb` (Oct 16). Compiles; **not yet tested in-game**. New structures only generate in new worlds or unexplored chunks. The Create recipes are data-only (no Create build for 26.1 exists yet).

### Spellcraft — a full spell system (72 spells)
- **Spell Scrolls**: right-click to learn a spell (the scroll is consumed). Found in loot (common / rare / epic tiers by structure) or copied on the **Inscription Table** (Writing Table + book + 2 pencils + amethyst; costs paper, pencils, element rune, Impure Magic Fragments and XP by tier; world-only spells can't be copied).
- **Mana**: new mana bar. Max = 100 + 15 per affinity level (cap 600); regen scales with affinity. Robes of the Mage, Essence Amulet and Arcane Surge make spells cheaper. Creative mode doesn't spend mana.
- **Affinity up to level 7**: casting grants affinity and it never decreases. Spell power is +10% per level; **Mastery (level 7)** makes that element's spells cost and recharge 15% less. Tier 1–6 spells require the matching affinity level (tier 6 = level 6). An affinity panel (Sneak + right-click on a focus) shows all 10 levels. Op command: `/mistech affinity get|reset|set|level`.
- **Cast types**: instant, continuous (pulses), **charged** (hold and release: up to ×2 power, ×1.6 cost) and **ritual** (stand still; cancels if you move).
- **Focus items** with spell slots (right-click casts, mouse wheel switches slot, Sneak + right-click opens the slot screen):
  - **Tomes**: Apprentice's (3 slots, T2), Adept's (4, T3), Archmage's (5, T5), plus 11 element Tomes — Flames, Glacial, Waters, Earth, Winds, Electric, Radiant, Shadow, Temporal, Spatial, Life (4 slots, up to T4; −20% mana for their element, +20% for the opposite).
  - **Wands** (faster recharge): Apprentice's (2 slots, −20%), Copper (−25%), Gold (3 slots, −35%), Mithril (3 slots, T4, −50%).
  - **Staffs** with a **mana reserve** spent before your own mana: Wooden (100), Runic (300), Mithril (800), Archmage's (2000, 4 slots, T6, unique). Staffs add power and make continuous spells cheaper.
  - **Gems & runes**: every focus has sockets; a socketed rune gives −5% mana and +5% power for its element (right-click with a rune in the off-hand; Sneak to pull one out).
  - The old **Gnome Staff** and **Mage Staff** now also buff spells when held (−10% mana / +15% power and −15% recharge).
- **Arcane Charger** (workbench block): charges a staff's reserve from FE (20 FE = 1 mana).
- **Resonance**: every spell leaves an element mark for 5 s; a spell of a matching element gets +25% power and a bonus effect (Fire→Water Steam, Ice→Energy Shatter, Water→Ice Deep Freeze, Earth→Life Bloom, Air→Energy Chain Lightning, Light→Dark Eclipse, Time→Space Fold, Fire→Earth Magma). Matching Tome in hand: +40%.
- **Spells**: 10 basic elements from tier 1 up — Spark, Ice Shard, Water Bolt, Gust, Discharge, Healing Tide, Stone Skin, Holy Light, Shadow Veil, Blink, Fire Burst, Wind Step, Dark Drain, Time Slow, Blizzard, Boulder, Meteor, Earthquake, Thunderstorm, Time Stop and more.
  - **Life**: Sprout, **Vine** (a grappling-hook rope: you swing like a pendulum; Quick Vine and Living Web variants), Roots, Living Forest, Tree of Life.
  - **Combinations**: Magma (Lava River, Eruption, Magma Armor), Steam (Mist, Steam Jet), Cloud (Cloud Platform, Cloud Bridge).
  - **Tier 6 legendaries** (found in the world only): Inferno, Ice Age, Tidal Wave, Cataclysm, Hurricane, Judgement, Dawn, Total Eclipse, Eternal Instant, Black Hole, Tree of Life.
  - **Summons** (duration-based; limit 1 + level/3): **Stone Golem**, **Wood Guardians**, **Shadow Wolf**, **Guardian Spirit**, **Phoenix** — all with custom models; they never attack players or each other.
  - **Utilities**: Anchor, Rewind, Telekinesis, Purify, Recharge (mana → FE in your items), Arcane Impulse (speeds up machines), Arcane Focus, Rift (opens your Ender chest anywhere), Light Orb, Vigilant Gaze, Lucky Coin, Echo, Void Touch, Soul Harvest.
- **New spell visuals**: animated 3D magic circles, shockwaves, spikes, pillars of light, real projectiles, camera shake and screen flashes, and custom tinted particles for every spell.
- **World structures** (find with `/locate structure mistech:<name>`):
  - **Fallen Archmage's Tower** (`archmage_tower`) — guaranteed **Archmage's Staff**, tier 4+ scrolls; 1 in 4 chance of the **Aether Crown** (+150 max mana, ×1.75 regen, amulet slot).
  - **Element Shrine** (`element_shrine`, one per element) — single glowing guardian, an element scroll and a chance at the element Tome.
  - **Forbidden Library** (`forbidden_library`) — buried library; guaranteed **Forbidden Tome** (5 slots, T6, +15% power, ignores affinity but costs hearts per cast); 1 in 3 chance of the **Wand of the Void** (T6, −60% recharge).
  - **Mother Tree** (`mother_tree`) — giant oak with a hollow trunk; guaranteed **Vine Scroll** and Life loot.
  - **Rune Stone Circle** (`rune_circle`) — the Rune Altar grants a permanent blessing: resonance marks last 8 s instead of 5.
- **Grimoire**: new **Spells** chapter (scrolls, Inscription Table, casting, staffs, Arcane Charger, Tomes/Wands/sockets, ruins and relics).

### Machines & automation
- **Machine tabs** on every machine: **Redstone** (ignore / needs signal / needs no signal), **Sides** (each face: off / input / output / both) and **Upgrades**; automatic item input/output with adjacent inventories.
- **Runic Upgrades** (Runic Plate → Speed / Efficiency / Range): Speed +50% each (max 4, +10% energy), Efficiency −15% energy each (max 4), Range +1 radius (max 3, Harvester/Planter only). Generators and batteries don't accept them.
- **Arcane Furnace**: 3 parallel lines; with power it runs at 1/4 of furnace time, ×1.5 XP and a 25% chance of an extra ingot from ores; without power it still works at Runic Furnace speed.
- **Arcane Extractor** (250 mB / 15 s, 8,000 mB tank) and **Arcane Compressor** (250 mB → 1 Fragment / 6 s, no incense needed).
- **Arcane Crusher**: 2 lines, 2.5 s per item, extra output from Earth Runes, makes **Mithril / Adamantine / Orichalcum Dust** (ore → 2 dusts).
- **Crusher Forge** (2×2×2 multiblock: Forge Core + Arcane Furnace + Arcane Crusher + 5 Impure Magic Blocks): crushes and smelts in one go — ore → dust → ingots, 3 lines, 60 FE/t each, with animated twin millstones.
- **Impure Magic Block** is now crafted TNT-style: 5 Impure Magic Fragments + 4 Magical Residue.
- **Runic Liquefier**: turns Fragments (250 mB), Impure Magic Dust (125 mB) and Impure Magic Blocks (750 mB) into Impure Essence; 8,000 mB tank.
- **Impure Magic Compressor** now has a reserve: with the output full, 3 more bottles wait (a bucket fills 4 at once).
- **Guide Stone**: Sneak + right-click on an anchor shows a **hologram** of the structure (blue = missing, red = wrong, green = correct, gold = optional); holding it also shows the **range** of incense holders, Harvesters, Planters, batteries, Sanctuary Fountain and more.
- Adamantine and Orichalcum can finally be smelted in any Mistech furnace (they only had blast-furnace recipes).

### Dreams
- **Dream Rift**: right-click a Surface block with a **Dream Fragment** (radius 12) or **Amplified Dream Fragment** (radius 28) to spread the Dream there — plants, trees and ground are replaced, then the biome, vegetation and creatures of a Dream region take over. 13 fixed-region fragments (Gnome Meadow, Sweet Mushroom Grove, Pillow Hills, Murky Wastes, Forest of Hands, Falling Chasms, Wrong Neighborhood, Floating Meadows, Fractal Garden, Inverted Forest, Starry Void, Memory Quilt, Abyssal Gaze). Crafted at the Impure Altar (Tier I/II).

### Farming, herbs & utilities
- **Hammock**: sleep by day; skips to **dusk** instead of morning. Doesn't set your spawn. (Sticks + 3 Hemp Fiber.)
- **Mandrake** (night harvests drop seeds; the Gnome King trades them): ripe plants scream when pulled (Nausea, Darkness, Slowness — helmet mutes it). The root screams in combat, **Mandrake Powder** is a 3×3 fertilizer, and the **Bottled Mandrake** (root in a Glass Jar) grows nearby crops about 3× faster when fed bone meal.
- **Belladonna**: Mystic/Psychedelic Vision now reveals invisible creatures and folklore beings and makes ores glow through stone; edible berries; **Belladonna Arrow**.
- **Coffee**: new **Caffeinated** effect (no Phantoms, can't sleep, +10% XP); a cup in the Gnome Bicycle gives +50% FE for 5 min.
- **Scroll Case**: holds up to 9 runes; right-click uses the selected rune, Z to switch. With a Fire Rune selected it works as a lighter.

### Quality of life
- **Grimoire search** (Ctrl+F or the magnifier). New entries for machines, upgrades, hologram, range and spells.
- **JEI**: "+" buttons for the Writing Table, Crusher, Runic and Arcane Furnaces; new stations.
- **Config files**: server (machine/upgrade/early-game/anchor/scroll case values) and client (range display, hologram).
- Progression: the Extractor now needs the **Ash Generator** first, instead of a Gnome Bicycle.

### Compatibility
- **Jade** (optional): progress, tanks, redstone, upgrades, incense, altar tier and more on Mistech blocks.
- **Create** (optional, recipes only): Deployer rolls cigars and joints; sequenced assembly for incense and Rue Cigar; Millstone, Crushing Wheels, Mechanical Saw and Mixing recipes (30 total). Waiting for a 26.1 build of Create.

### Fixes
- Continuous spells (Blizzard, Earthquake, Time Slow, Vine) no longer skip their recharge when you change hotbar slot.
- Dead summons free their slot immediately; Living Forest now counts as one summon.
- Vine rope feels like a real rope instead of jittering.
- Fixed the Forbidden Library stairway getting blocked and the Archmage Tower stair floating.
- Aether Crown mana bonus now shows on the client bar.
- Fire Burst only damages when the ball lands.

---

## Oct 16, 2026 — The Dream dimension, Spatial Dreamcatcher, gear rework

Everything since commit `927417f` (Oct 14). Compiles and the server starts; not yet fully tested in-game. Old worlds: delete `dimensions/mistech/gnome_realm` so the realm regenerates.

### The Dream — Gnome Realm rebuilt as concentric rings
- The Gnome Realm is now a dream made of **4 rings** around the King's House and mine (kept at the center): **Common Dream** (0–400) → **Nightmare** (400–2000) → **Deep Dream** (2000–5000, psychedelic, floating islands) → **REM Sleep** (5000+, dark and mysterious). The **Dream's Edge** at 30,000 wakes you up at home.
- 14 new biomes: Gnome Meadow, Sweet Mushroom Grove, Pillow Hills · Murky Wastes, Hand Forest, Falling Chasms, Wrong Neighborhood · Floating Meadows (−40% gravity), Fractal Garden, Inverted Forest · Starry Void, Memory Quilt, Abyssal Gaze — plus the Gnome Depths (ores) under the whole dimension.
- Custom biome source and terrain (rolling rings, pillow hills, chasms, sky islands in rings 3–4).
- New blocks: Sleep Poppy, Dream Flower, Giant Dandelion, Cloud Cotton, Blackthorn, Eye Flower, Sleepless Ivy, Kaleidoscope Flower, Prism Mushroom, Spiral Vine, Memory Lily, Sweet Mycelium, Cloud Wool, Prismatic Grass (shifts color in sync), Star Stone, Star Moss, Bouncy Cloud, pink/cyan Dream Mushroom blocks and the **Fractal Tree** wood set.
- Features: Hand Trees, Fractal Trees, Inverted Trees, Spiral Vines, Bouncy Clouds, unlit lamp posts, giant mushrooms.

### Dream creatures
- **Counting Sheep** (shear for Cloud Wool; count 3 in 30 s for Regeneration + Saturation), **Gnome Merchant** (trades), **Corrupted Gnome** (hostile), **Gnome Miner** (sniffs out the nearest ore for you), **Bogeyman** (strong in the dark, weak in light), **Shade** (freezes when looked at).
- **Cloud Whale** (rideable, sings and drops Dream Amber when fed Cloud Cotton), **Air Fish** (schools; food with Slow Falling), **Stained-Glass Moth** (circles lights, drops Dream Dust).
- **Echo** (retraces your path 3.5 s behind and catches you if you stop), **REM Watcher** (a chandelier of eyes that raises the alarm), **Memory** (tells a memory and becomes a Memory Fragment).
- **The Dreamer** — boss (400 HP): sleep bubbles, lullaby, summons Echoes; at half health it **wakes up** (faster, summons Shades, Darkness, teleports behind you). Summoned with the **Bell of Awakening** in REM.

### Dream structures
- Gnome Hamlet, Ruined Hamlet, **Wrong House** (3 identical looping rooms; beds release Bogeymen), **Endless Stair**, **Fractal Temple** (a 27³ Menger sponge on a sky island), **Library of Memories** (3 crooked stacked floors, upside-down top room).
- Dream Anchor (hidden block that runs the loops and spawns).

### Dream items
- Dream Dust, Dream Amber, Air Fish, Memory Fragment, Watcher Eye, Dreamer's Essence, **Bell of Awakening**.
- **Lucid Dreamcatcher**: travel between the dream layers you've already visited.
- **Spatial Dreamcatcher** (Dream Dust, ender pearl, Dream Amber, Dreamcatcher, Space Rune): catch up to 3 dream creatures (bosses excluded) and release them anywhere — inside the Dream or in other dimensions. Keeps health, name, saddle and variant. Released Cloud Whales get a home and roam around it.
- **Cloud Whale saddle**: put a vanilla saddle on a Cloud Whale (dispensers work too) and **pilot it**: fly where you look (~9 blocks/s), jump to climb, a second passenger rides on the clouds. Piloted whales collide with blocks and can go anywhere; sneak + empty hand removes the saddle.
- Grimoire "Gnome Dimension" chapter updated with all of the above.

### Mithril / Adamantine / Orichalcum gear rework
- Stronger tiers: Mithril 200k FE (tools speed 12, +6 dmg; armor 4/8/6/4, toughness 4), Adamantine 400k FE (speed 14, +8; toughness 6), Orichalcum 1M FE (speed 16, +10), Pure Orichalcum 2M FE.
- **Tool abilities** (charged gear only): Adamantine+ pickaxe/shovel dig **3×3** (Pure Orichalcum **5×5**) and the axe fells whole trees (sneak to disable); Orichalcum+ **magnet** (drops go straight to your inventory) and charged sword/spear hits release a **shockwave**; Pure Orichalcum also fires a **ray of light**.
- **Set bonuses** (stack, lowest tier of the 4 pieces counts): Adamantine **Bulwark** (+4 health, −15% physical damage, no knockback while sneaking), Orichalcum **Astral** (+6 health, +15% damage and Night Vision at night), Pure Orichalcum **Solar** (+8 health, Regeneration by day, 4-heart regenerating shield, +50% vs undead).
- Runes socketed in Orichalcum count ×1.25, in Pure Orichalcum ×1.5.

### Fixes
- JEI: fixed a startup crash ("unbound value: mistech:magic_cloth") — the plugin no longer loads mod content too early.
- Goblin Village / structure blueprints and the King's House adjusted for the new dream terrain.

---

## Oct 14, 2026 — Early-game farming, fluid golems, JEI

Everything since commit `dcb44ef` (Oct 13). Compiles; not yet fully tested in-game.

### Early-game farming
- **Growing Pot** (bricks + dirt + bone meal): plant sugar cane, any small flower or a mushroom (incl. the **Yellowish Mushroom**) and it grows copies over time (cane 45 s, flowers 60 s, mushrooms 90 s, Yellowish Mushroom 2 min). Water touching it = 2×, a fed Bottled Gnome nearby = +50%, bone meal = instant copy. Holds a stack; hopper below pulls, hopper above/sides inserts. Accepted plants: `#mistech:pot_plants`.
- **Creature Jar**: sneak + use an empty Glass Jar on an adult chicken, cow/mooshroom, sheep, squid or glow squid. Placed, it produces eggs (variant-aware), milk (up to 4 buckets), wool in the sheep's color, or (glow) ink sacs. Sneak + empty hand releases the animal. The animal is drawn inside the jar and kept on the item.
- **Grinder**: Amethyst Block → 4 Amethyst Shards.

### Golems & logistics
- **Runic Command Rod** (Command Rod + Space Rune + ender pearl; alone in the grid = clear): up to 8 origins and 8 destinations within 48 blocks. Use = origin, sneak = destination; use in air = delivery mode (Priority / Round-robin / Even), sneak in air = collect mode (Priority / Round-robin / Fullest). Use on a golem = assign route, sneak on a golem = copy its route. Points glow while held.
- **Water Rune on golems** (fluid transport): Terracotta and Mithril work golems carry fluids between the rod's points (any fluid tank). Load = item load × 125 mB (4 / 8 buckets, doubled with Earth). With no tank at the origin they pick up fluid source blocks next to the point. A filled bucket works as a filter. They spill one block of fluid on death.
- Essence Extractor and Magic Compressor now expose fluid handling (pipes and golems can pull/push essence). The regular Command Rod also accepts fluid blocks.
- Work golems drop their statue when killed (runes are lost).

### Werewolves & Silver Incense
- **Silver Incense** (4 incense around a mithril ingot → 4): its smoke makes werewolves drop their target and flee, and stops them from spawning nearby — around you in your hand, or 48 blocks around an Incense Holder.
- Full-moon werewolves are now **rare and only around villages**, one at a time (Blood Moon still spawns them anywhere).
- At dawn a werewolf turns back into an unemployed villager instead of vanishing.

### Blocks & visuals
- **Magic Compressor** keeps the hardened shard on top (up to a stack; taken by hand, hopper, pipes or golems) instead of dropping it, and renders it.
- **Writing Table** shows the item being written, floating and spinning above the table.
- Every non-magic incense now leaves 1 Incense Ash (35% chance of 2) instead of a 25% chance.
- Boitatá mount armor now follows the body's animation.

### Compatibility
- **JEI**: categories for Writing Table, Grinder (crushing), Impure and Sanctuary rituals, machines, generators, magic cigars/joints and multiblocks, plus info pages (Incense Ash, Magic Residue, Werewolf Fang).
- **Botany Pots**: Yellowish Mushroom crop.
- Build: Curios now comes from Modrinth (the old maven was down); JEI from the BlameJared maven.

---

## Oct 13, 2026 — Folklore expansion, Boitatá, cachaça, utilities, Arcane Infuser

Commit `dcb44ef` — all changes since the initial commit (`f272be5`, Oct 8).

---

### New biome — Ancestral Forest
- **Ancestral Forest** (`mistech:ancestral_forest`): dense jungle of giant trees, injected into the Overworld via mixin (replaces part of the wettest jungles/dark forests). Light-green fog, dark-green water, darker foliage, floating spores.
- **Ancestral Tree** feature: round trunks 3×3–4×4 wide and 22–40 blocks tall, buttress roots, side branches with canopies, vines, flowering azalea leaves, moss/podzol at the base.
- Folklore creatures spawn more often here, but also in forests, jungles and taigas (`#mistech:folklore_wilds`).
- Fix: crash on startup ("Unreferenced key: mistech:ancestral_forest") — the biome mixin no longer runs during vanilla-only registry validation.

### New folklore creatures
- **Matinta Pereira** — a dark owl that whistles at night. Offer dried tobacco or coffee for a gift (Matinta Feathers and trinkets) and Luck II. Hit her and she becomes an **old witch** (70 HP): curses, scream, summons spirits, vanishes in smoke. Drops **Matinta Feather**.
- **Caipora** — forest guardian riding a peccary. Peaceful until you overhunt: every animal you kill adds to your *hunt sin*; at 12 she shows up angry (charges, whistles that blind/confuse and can get you "lost"). Offer tobacco or cachaça to calm her and get **Hunter's Blessing** (double animal drops, 10 min). "Killing" her gives **Caipora's Curse** (animals drop nothing, 20 min).
- **Iara** — water spirit (40 HP) in rivers, swamps, mangroves and the Ancestral Forest at night. Her **song** pulls nearby players toward the water; grabs and drowns swimmers. Drops **Iara Scale**.
  - **Iara Ring** (ring slot / Curios): immune to the song, Water Breathing; in water: Dolphin's Grace, Conduit Power, Night Vision.
- **Cuca** — boss (400 HP). Summoned by the `awaken_cuca` T3 ritual, or appears in the Ancestral Forest if you go 3+ days without sleeping. Thrown potions, poison cauldron, lullaby, summons witches, teleports. Drops **Cuca's Cauldron**.
  - **Cuca's Cauldron**: over a heat source, mixes up to 3 potions into one **Cuca's Brew** (effects stack, max 8 min); Impure Essence adds +1 level to everything.
- **Mapinguari** — boss (500 HP). Rare night spawn in the Ancestral Forest or `awaken_mapinguari` T3 ritual. Ground slam, stinking roar, takes only 25% damage from projectiles, tears through leaves. Drops **Mapinguari Claw**.
  - **Mapinguari Axe**: 2000 durability, repaired with claws; fells the whole tree at once (sneak to cut a single log).
- New models/textures for all folklore creatures (Saci/Curupira style), plus spawn eggs and guide book entries.

### Boss lairs
- **Cuca's Lair** (`/locate structure mistech:cuca_lair`): clay clearing with a pond, skull totems, crooked mud-and-wood hut with a cauldron, brewing stands and a loot chest.
- **Mapinguari's Cave** (`mistech:mapinguari_lair`): trampled clearing with fallen logs and bones, ramp tunnel down to a huge underground hall with roots, pillars and a nest with a loot chest.
- **Lair Heart**: hidden block that spawns the boss when a player gets close; once defeated, the lair sleeps for 5 in-game days.

### Boitatá
- **Boitatá Armor** (made from Boitatá Scales; better than netherite, fireproof), with 3D details (fire crest, glowing Eye, back spikes):
  - Helm: see inside lava. Chestplate: fire immunity, attackers catch fire. Leggings: sprinting leaves a fire trail (doesn't burn blocks). Boots: walk on lava (temporary magma crust; sneak to sink).
  - Full set — **Serpent Form**: fire heals you, fast lava swimming, **Fire Breath** (key R, 1×/min).
- **Boitatá Egg** (T3 night ritual with the Boitatá Eye): hatches in 3 days when kept hot; Life Rune and Pocket Hourglass speed it up.
- **Boitatá mount**: grows from hatchling (follows and fights) → juvenile (ground mount, walks on lava) → adult (flying mount). Hold **R** for a continuous fire breath. Feed it to grow faster. Snake-like full-body model that undulates. Permadeath.
- **Boitatá Barding**: Scale (+10 armor) and Orichalcum (+16 armor, +25% breath damage), shown as 3D armor pieces.
- **Boitatá Whistle**: Follow / Stay / Wander / Call orders for all your Boitatás.

### Cachaça & drinks
- **Garapa** (sugar cane juice) → **Fermented Garapa** → **Cachaça** → **Aged Cachaça**; also **Rue Cachaça** (removes Taint) and **Honey Cachaça** (Regeneration, cures poison).
- **Fermentation Barrel**: up to 16 bottles; ferments/ages by world time (works while unloaded).
- **Cane Still**: distills Fermented Garapa; heated by fire/lava/magma below, a Fire Rune, or FE.
- All cachaças give Strength I + Resistance I; three in a row gets you drunk.
- Offering cachaça to your **Saci** doubles its fed time and gives an instant gift.

### Utilities
- **Runic Elevator**: jump to go up, sneak to go down between elevators of the same color (up to 32 blocks). Dye to change color (17 colors).
- **Runic Hookah**: water / Impure / Purified Essence base + tobacco, hemp or rue + any rune as flavor. Puffs give that rune's effect; lit hookah emits a small aura. The model shows its contents (liquid, leaves, embers, rune).

### Arcane Infuser
- **Arcane Infuser** → **Greater** (T2 ritual) → **Astral** (T3 ritual): choose any compatible enchantment and level for an item or book, paid with XP, FE, lapis and Impure Essence.
- Max level: Infuser II · Greater = vanilla max · Astral = max +1 and unlocks treasure enchantments (Mending, Frost Walker…). Never curses.

### Mod compatibility
- **Apotheosis**: 4 new gems — Impure, Purified, Astral, Gnomish.
- **Apothic Enchanting**: Mistech blocks now give Eterna/Quanta/Arcana/Clues at the enchanting table (Impure/Pure Magic Blocks, Mithril/Adamantine/Orichalcum Blocks, Mithril Obelisk, Astral Pillar).
- Unpowered Mithril/Adamantine/Orichalcum gear also loses Apotheosis affixes and gems.
- **Productive Metalworks**: melting, alloy temperatures and casting for Mithril (1000°), Adamantine (1500°) and Orichalcum (2500°); Impure and Purified Essence work as foundry fuel.
- New fluids (always registered): **Molten Mithril**, **Molten Adamantine**, **Molten Orichalcum** with buckets and `c:molten_*` tags.
- Curios and Silent Gear support reviewed (Iara Ring added to the Curios ring tag).

### Other
- Pocket Hourglass now also speeds up Boitatá Eggs.
- Temporary blocks (e.g. lava crust) get their timer extended instead of being skipped.
- Guide book: new pages for everything above (EN and PT-BR).
