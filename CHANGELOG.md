# Mistech — Changelog

## Unreleased (Oct 9 – Oct 13, 2026)

All changes since the initial commit (`f272be5`, Oct 8). Compiles; most of it has not yet been tested in-game.

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
