"""Arenas do folclore (Toca da Cuca e Caverna do Mapinguari): JSONs de worldgen, loot, blockstate/modelo, traduções
e os remendos no Java (registros, morte do chefe, árvores que não nascem nas arenas).
Uso (raiz do projeto): python tools/lore/lairs.py   (pode rodar de novo: não duplica nada)"""
import json
import os

R = 'src/main/resources/'
D = R + 'data/mistech/'
A = R + 'assets/mistech/'
J = 'src/main/java/com/mistech/mistechmod/lore/'


def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


# ------------------------------------------------------------------ worldgen
for name, salt in (('cuca', 61420917), ('mapinguari', 70391155)):
    wj(D + 'worldgen/structure/%s_lair.json' % name, {
        'type': 'mistech:lore_lair',
        'lair': name,
        'biomes': ['mistech:ancestral_forest'],
        'spawn_overrides': {},
        'step': 'surface_structures' if name == 'cuca' else 'underground_structures',
        'terrain_adaptation': 'none',
    })
    wj(D + 'worldgen/structure_set/%s_lairs.json' % name, {
        'placement': {'type': 'minecraft:random_spread', 'salt': salt, 'spacing': 28, 'separation': 10},
        'structures': [{'structure': 'mistech:%s_lair' % name, 'weight': 1}],
    })


# ------------------------------------------------------------------ loot
def item(name, w, lo=1, hi=1, extra=None):
    e = {'type': 'minecraft:item', 'name': name, 'weight': w}
    fns = []
    if hi > 1:
        fns.append({'function': 'minecraft:set_count', 'count': {'type': 'minecraft:uniform', 'min': lo, 'max': hi}, 'add': False})
    if extra:
        fns += extra
    if fns:
        e['functions'] = fns
    return e


def potion(pid, w):
    return item('minecraft:potion', w, extra=[{'function': 'minecraft:set_potion', 'id': pid}])


BOOK = [{'function': 'minecraft:enchant_randomly'}]
wj(D + 'loot_table/chests/cuca_lair.json', {'type': 'minecraft:chest', 'pools': [
    {'rolls': {'type': 'minecraft:uniform', 'min': 5, 'max': 8}, 'bonus_rolls': 0.0, 'entries': [
        item('minecraft:emerald', 14, 2, 7),
        item('mistech:matinta_feather', 8, 1, 3),
        item('minecraft:nether_wart', 10, 2, 6),
        item('minecraft:glistering_melon_slice', 6, 1, 3),
        item('minecraft:blaze_powder', 6, 1, 3),
        item('minecraft:fermented_spider_eye', 8, 1, 3),
        item('minecraft:ghast_tear', 3, 1, 2),
        item('minecraft:gold_ingot', 8, 2, 5),
        item('mistech:impure_magic_dust', 8, 2, 6),
        item('mistech:impure_magic_shard', 4, 1, 2),
        potion('minecraft:strong_healing', 5),
        potion('minecraft:long_night_vision', 4),
        potion('minecraft:long_fire_resistance', 4),
        potion('minecraft:strong_poison', 4),
        item('minecraft:book', 4, extra=BOOK),
    ]}]})
wj(D + 'loot_table/chests/mapinguari_cave.json', {'type': 'minecraft:chest', 'pools': [
    {'rolls': {'type': 'minecraft:uniform', 'min': 5, 'max': 8}, 'bonus_rolls': 0.0, 'entries': [
        item('minecraft:bone', 14, 3, 9),
        item('minecraft:leather', 12, 2, 6),
        item('minecraft:rotten_flesh', 8, 2, 7),
        item('minecraft:iron_ingot', 10, 2, 6),
        item('minecraft:gold_ingot', 8, 2, 5),
        item('minecraft:emerald', 8, 2, 6),
        item('minecraft:diamond', 4, 1, 2),
        item('mistech:mithril_ingot', 3, 1, 3),
        item('mistech:mapinguari_claw', 3),
        item('minecraft:golden_apple', 3),
        item('minecraft:book', 4, extra=BOOK),
        item('minecraft:moss_block', 6, 2, 6),
    ]}]})

# ------------------------------------------------------------------ bloco invisível
wj(A + 'blockstates/lair_core.json', {'variants': {'': {'model': 'mistech:block/lair_core'}}})
wj(A + 'models/block/lair_core.json', {'textures': {'particle': 'minecraft:block/tuff'}})

# ------------------------------------------------------------------ traduções
LANG = {
    'en_us': {
        'block.mistech.lair_core': 'Lair Heart',
        'message.mistech.cuca.lair': 'You trespassed into the Cuca\'s den... and she is home!',
        'message.mistech.mapinguari.cave': 'A rotten stench rises from the den... the Mapinguari is awake!',
    },
    'pt_br': {
        'block.mistech.lair_core': 'Coração da Toca',
        'message.mistech.cuca.lair': 'Você invadiu a toca da Cuca... e ela está em casa!',
        'message.mistech.mapinguari.cave': 'Um fedor podre sobe da toca... o Mapinguari acordou!',
    },
}
GUIDE = {
    'en_us': {
        'guide.mistech.cuca.1': ' She also lives in the §lCuca\'s Den§r, a crooked hut in the Ancestral Forest with skull totems and a smoking chimney: walk up to it and she comes out the door.',
        'guide.mistech.mapinguari.1': ' Its home is the §lMapinguari Cave§r: a hole surrounded by bones and clawed stumps in the Ancestral Forest leads down to a huge chamber with its nest.',
    },
    'pt_br': {
        'guide.mistech.cuca.1': ' Ela também mora na §lToca da Cuca§r, uma cabana torta na Mata Ancestral com totens de caveira e chaminé soltando fumaça: chegue perto e ela sai pela porta.',
        'guide.mistech.mapinguari.1': ' A casa dele é a §lCaverna do Mapinguari§r: um buraco cercado de ossos e tocos arranhados na Mata Ancestral desce até um salão enorme com o ninho dele.',
    },
}
for lang, entries in LANG.items():
    p = A + 'lang/%s.json' % lang
    data = json.load(open(p, encoding='utf-8'))
    data.update(entries)
    for k, extra in GUIDE[lang].items():
        if k in data and extra.strip() not in data[k]:
            data[k] = data[k] + extra
    with open(p, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')


# ------------------------------------------------------------------ Java
def patch(path, old, new, marker):
    s = open(path, encoding='utf-8').read()
    if marker in s:
        return
    if old not in s:
        raise SystemExit('não achei o trecho em %s: %r' % (path, old[:60]))
    s = s.replace(old, new, 1)
    open(path, 'w', encoding='utf-8', newline='\n').write(s)


LC = J + 'LoreContent.java'
patch(LC, '''    // ----- criaturas -----''', '''    // ----- arenas dos chefes (Toca da Cuca, Caverna do Mapinguari) -----
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Mistech.MODID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Mistech.MODID);
    public static final DeferredHolder<StructureType<?>, StructureType<LoreLairStructure>> LAIR_TYPE =
            STRUCTURE_TYPES.register("lore_lair", () -> () -> LoreLairStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LAIR_PIECE =
            PIECE_TYPES.register("lore_lair", () -> (StructurePieceType) LoreLairPiece::new);
    public static final DeferredBlock<LairCoreBlock> LAIR_CORE = Mistech.BLOCKS.registerBlock("lair_core", LairCoreBlock::new,
            p -> p.mapColor(MapColor.STONE).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion());
    public static final Supplier<BlockEntityType<LairCoreBlockEntity>> LAIR_CORE_BE = MistechBlockEntities.BLOCK_ENTITY_TYPES
            .register("lair_core", () -> new BlockEntityType<>(LairCoreBlockEntity::new, LAIR_CORE.get()));

    // ----- criaturas -----''', 'LAIR_TYPE')
patch(LC, 'import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;',
      'import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;\n'
      'import net.minecraft.world.level.levelgen.structure.StructureType;\n'
      'import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;', 'import net.minecraft.world.level.levelgen.structure.StructureType;')
patch(LC, '''        FEATURES.register(modEventBus);
''', '''        FEATURES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
''', 'STRUCTURE_TYPES.register(modEventBus)')

LE = J + 'LoreEvents.java'
patch(LE, '''    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {''', '''    /** Chefe que nasceu de uma toca morreu: a toca fica "derrotada" e só acorda outro depois de uns dias. */
    @SubscribeEvent
    static void onBossDeath(LivingDeathEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.getPersistentData().contains(LairCoreBlockEntity.LAIR_TAG)) {
            return;
        }
        BlockPos core = BlockPos.of(entity.getPersistentData().getLongOr(LairCoreBlockEntity.LAIR_TAG, 0L));
        if (level.isLoaded(core) && level.getBlockEntity(core) instanceof LairCoreBlockEntity be) {
            be.onBossDefeated();
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {''', 'onBossDeath')

AT = J + 'AncestralTreeFeature.java'
patch(AT, '''        if (!level.getBlockState(origin.below()).is(BlockTags.DIRT)) {
            return false;
        }''', '''        if (!level.getBlockState(origin.below()).is(BlockTags.DIRT) || nearLair(level, origin)) {
            return false;
        }''', 'nearLair(level, origin)')
patch(AT, '''    @Override
    public boolean place(''', '''    /** As arenas do folclore têm chão de barro batido (packed mud), que não existe na mata: não nasce árvore perto. */
    private static boolean nearLair(WorldGenLevel level, BlockPos origin) {
        int hits = 0;
        for (int dx = -12; dx <= 12; dx += 4) {
            for (int dz = -12; dz <= 12; dz += 4) {
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, origin.getX() + dx, origin.getZ() + dz) - 1;
                var s = level.getBlockState(new BlockPos(origin.getX() + dx, y, origin.getZ() + dz));
                if (s.is(Blocks.PACKED_MUD) || s.is(Blocks.MUD_BRICKS)) {
                    if (++hits >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean place(''', 'private static boolean nearLair')
print('ok')
